package com.salattracker.data.remote.api

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.*

/** File name of the single backup blob kept in the user's hidden Drive app-data folder. */
internal const val DRIVE_BACKUP_FILE_NAME = "salat-backup.json.gz"

/**
 * Minimal hand-written Drive v3 client for the appDataFolder backup file. Every call
 * takes an explicit bearer token (from [com.salattracker.data.backup.DriveAuthorizer])
 * rather than an OkHttp auth interceptor, since the token is short-lived and refreshed
 * per call site instead of being baked into the client.
 */
interface DriveApiService {

    @GET("drive/v3/about")
    suspend fun about(
        @Header("Authorization") authorization: String,
        @Query("fields") fields: String = "user(emailAddress,displayName)",
    ): DriveAboutResponse

    @GET("drive/v3/files")
    suspend fun list(
        @Header("Authorization") authorization: String,
        @Query("spaces") spaces: String = "appDataFolder",
        @Query("q") query: String = "name = '$DRIVE_BACKUP_FILE_NAME' and trashed = false",
        @Query("fields") fields: String = "files(id,name,size,md5Checksum,appProperties)",
        @Query("pageSize") pageSize: Int = 1,
    ): DriveFileListResponse

    @GET("drive/v3/files/{fileId}")
    @Streaming
    suspend fun download(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String,
        @Query("alt") alt: String = "media",
    ): ResponseBody

    @POST("upload/drive/v3/files")
    suspend fun create(
        @Header("Authorization") authorization: String,
        @Query("uploadType") uploadType: String = "multipart",
        @Query("fields") fields: String = "id,size,md5Checksum,appProperties",
        @Body body: MultipartBody,
    ): DriveFileResource

    @PATCH("upload/drive/v3/files/{fileId}")
    suspend fun update(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String,
        @Query("uploadType") uploadType: String = "multipart",
        @Query("fields") fields: String = "id,size,md5Checksum,appProperties",
        @Body body: MultipartBody,
    ): DriveFileResource
}

data class DriveAboutResponse(@SerializedName("user") val user: DriveUser?)
data class DriveUser(
    @SerializedName("emailAddress") val emailAddress: String?,
    @SerializedName("displayName") val displayName: String?,
)

data class DriveFileListResponse(@SerializedName("files") val files: List<DriveFileResource>?)

data class DriveFileResource(
    @SerializedName("id") val id: String?,
    @SerializedName("size") val size: String?,
    @SerializedName("md5Checksum") val md5Checksum: String?,
    @SerializedName("appProperties") val appProperties: Map<String, String>?,
)
