plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.mbteam.fugas"
    compileSdk = 34

    defaultConfig {
        // ДОЛЖЕН совпадать с applicationId установленного APK, иначе обновление встанет как второе приложение
        applicationId = "com.mbteam.fugas"
        minSdk = 26
        targetSdk = 34
        versionCode = 675
        versionName = "6.7.5"
        // Адрес туннеля cloudpub (меняется при перезапуске туннеля, поэтому держим здесь)
        buildConfigField("String", "SERVER_URL", "\"https://ВАШ-АДРЕС.cloudpub.ru\"")
    }

    buildFeatures { buildConfig = true }

    signingConfigs {
        create("release") {
            // Тот же keystore, что у текущего APK. Пути и пароли берём из переменных окружения, в git не кладём.
            storeFile = file(System.getenv("FUGAS_KEYSTORE") ?: "fugas-release.jks")
            storePassword = System.getenv("FUGAS_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("FUGAS_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("FUGAS_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging")
}
