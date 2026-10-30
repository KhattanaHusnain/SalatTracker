plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    id("com.google.dagger.hilt.android")

    id("com.google.devtools.ksp") version "2.3.9"
}

android {
    namespace = "com.salattracker"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.salattracker"
        minSdk = 27
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "Boolean",
            "ENABLE_LOGGING",
            "true"
        )
    }

    buildTypes {
        release {
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }

    // Mushaf page fonts (app/src/main/assets/quran/mushaf_fonts/) are read via
    // androidx.compose.ui.text.font.Font(path, AssetManager) at runtime, which wants the asset
    // stored uncompressed in the APK (mmap'd directly) rather than re-inflated on every open.
    androidResources {
        noCompress += "ttf"
    }
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}
android.buildFeatures.buildConfig = true
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Hilt Compose
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt + WorkManager
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Material
    implementation(libs.material.v1130)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // Location
    implementation(libs.play.services.location)

    // Google Drive backup (AuthorizationClient for the drive.appdata scope)
    implementation(libs.play.services.auth)

    // On-device prayer time calculation
    implementation(libs.adhan2)

    testImplementation(libs.kotlinx.coroutines.test)
}
// Export Room schemas (committed under app/schemas) so migrations can be verified against them.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
