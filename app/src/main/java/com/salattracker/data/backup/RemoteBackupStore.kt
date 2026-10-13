package com.salattracker.data.backup

import com.salattracker.data.remote.api.DRIVE_BACKUP_FILE_NAME
import com.salattracker.data.remote.api.DriveApiService
import com.salattracker.data.remote.api.DriveFileResource
import com.salattracker.di.DriveRetrofit
import com.salattracker.domain.model.RemoteBackupInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RemoteError {
    data object Unauthorized : RemoteError
    data object QuotaExceeded : RemoteError
    data object RateLimited : RemoteError
    data class ServerError(val code: Int) : RemoteError
    data class Other(val message: String) : RemoteError
}

class RemoteBackupException(val error: RemoteError, message: String, cause: Throwable? = null) : Exception(message, cause)

/** The remote file's identity plus the same summary [RemoteBackupInfo] the UI shows. */
data class RemoteHead(
    val fileId: String,
    val info: RemoteBackupInfo,
    val md5Checksum: String?,
)

interface RemoteBackupStore {
    suspend fun accountEmail(token: String): String
    /** Null means no backup file exists yet for this account. */
    suspend fun head(token: String): RemoteHead?
    suspend fun download(token: String, fileId: String): ByteArray
    /** Creates the file when [previous] is null, otherwise replaces its content and metadata in place. */
    suspend fun put(token: String, previous: RemoteHead?, bytes: ByteArray, meta: BackupMeta): RemoteHead
}

@Singleton
class DriveBackupStore @Inject constructor(
    @DriveRetrofit retrofit: Retrofit,
) : RemoteBackupStore {

    private val api = retrofit.create(DriveApiService::class.java)

    override suspend fun accountEmail(token: String): String {
        val response = callApi { api.about(bearer(token)) }
        return response.user?.emailAddress
            ?: throw RemoteBackupException(RemoteError.Other("no email"), "Drive did not return an account email")
    }

    override suspend fun head(token: String): RemoteHead? {
        val response = callApi { api.list(bearer(token)) }
        val file = response.files?.firstOrNull() ?: return null
        return file.toRemoteHead()
    }

    override suspend fun download(token: String, fileId: String): ByteArray = withContext(Dispatchers.IO) {
        callApi { api.download(bearer(token), fileId) }.bytes()
    }

    override suspend fun put(token: String, previous: RemoteHead?, bytes: ByteArray, meta: BackupMeta): RemoteHead {
        val metadataJson = JSONObject().apply {
            if (previous == null) {
                put("name", DRIVE_BACKUP_FILE_NAME)
                put("parents", JSONArray().put("appDataFolder"))
            }
            put("appProperties", JSONObject().apply {
                put("formatVersion", meta.formatVersion.toString())
                put("revisionId", meta.revisionId)
                put("createdAt", meta.createdAt.toString())
                put("contentSha256", meta.contentSha256)
                put("prayerEntryCount", meta.prayerEntryCount.toString())
                put("oldestPrayerDate", meta.oldestPrayerDate?.toString() ?: "")
                // appProperties values are capped at 124 bytes (key+value) by Drive; device
                // model strings are occasionally long (e.g. some Samsung/Xiaomi build names).
                put("deviceModel", meta.deviceModel.take(48))
            })
        }.toString()

        val multipart = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(MultipartBody.Part.create(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())))
            .addPart(MultipartBody.Part.create(bytes.toRequestBody("application/gzip".toMediaType())))
            .build()

        val resource = callApi {
            if (previous == null) api.create(bearer(token), body = multipart)
            else api.update(bearer(token), fileId = previous.fileId, body = multipart)
        }
        return resource.toRemoteHead()
    }

    private fun bearer(token: String) = "Bearer $token"

    private fun DriveFileResource.toRemoteHead(): RemoteHead {
        val props = appProperties ?: emptyMap()
        val info = RemoteBackupInfo(
            revisionId       = props["revisionId"] ?: "",
            createdAt        = props["createdAt"]?.toLongOrNull() ?: 0L,
            sizeBytes        = size?.toLongOrNull() ?: 0L,
            prayerEntryCount = props["prayerEntryCount"]?.toIntOrNull() ?: 0,
            oldestPrayerDate = props["oldestPrayerDate"]?.takeIf { it.isNotEmpty() },
            deviceModel      = props["deviceModel"] ?: "",
            contentSha256    = props["contentSha256"] ?: "",
        )
        val fileId = id ?: throw RemoteBackupException(RemoteError.Other("no id"), "Drive returned a file with no id")
        return RemoteHead(fileId = fileId, info = info, md5Checksum = md5Checksum)
    }

    private suspend fun <T> callApi(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: HttpException) {
            throw e.toRemoteBackupException()
        } catch (e: IOException) {
            throw RemoteBackupException(RemoteError.Other(e.message ?: "network error"), "Network error talking to Drive", e)
        }
    }

    private fun HttpException.toRemoteBackupException(): RemoteBackupException {
        val body = response()?.errorBody()?.string().orEmpty()
        val error = when {
            code() == 401 -> RemoteError.Unauthorized
            code() == 403 && "storageQuotaExceeded" in body -> RemoteError.QuotaExceeded
            code() == 403 && "rateLimitExceeded" in body -> RemoteError.RateLimited
            code() == 429 -> RemoteError.RateLimited
            code() in 500..599 -> RemoteError.ServerError(code())
            else -> RemoteError.Other(message())
        }
        return RemoteBackupException(error, "Drive request failed (${code()})", this)
    }
}
