plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.salatdnd"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.salatdnd"
        minSdk = 23
        targetSdk = 29
        versionCode = 1
        versionName = "1.0"
    }
}
