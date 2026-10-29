plugins {
    id("com.android.application")
    id("com.chaquo.python")
}

android {
    namespace = "ru.obxodik.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.obxodik.android"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        val releaseKeystore = System.getenv("OBXODIK_KEYSTORE")
        if (!releaseKeystore.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("OBXODIK_STORE_PASSWORD")
                keyAlias = System.getenv("OBXODIK_KEY_ALIAS")
                keyPassword = System.getenv("OBXODIK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val releaseKeystore = System.getenv("OBXODIK_KEYSTORE")
            if (!releaseKeystore.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.core:core:1.15.0")
    implementation(files("libs/hev-socks5-tunnel.aar"))
}

chaquopy {
    defaultConfig {
        version = "3.12"
        System.getenv("OBXODIK_BUILD_PYTHON")
            ?.takeIf { it.isNotBlank() }
            ?.let { buildPython(it) }
        pip {
            options("--timeout", "120", "--retries", "5")
            install("cryptography")
            install("httpx")
            install("h2")
            install("certifi")
        }
    }
}
