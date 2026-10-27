plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.dailytext.widget"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.dailytext.widget"
        minSdk = 26
        targetSdk = 34
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    // 고정 서명 키: 새 버전을 설치할 때 기존 앱 위에 그대로 덮어쓸 수 있게 한다.
    signingConfigs {
        create("fixed") {
            storeFile = file("widget.keystore")
            storePassword = "widget123"
            keyAlias = "widget"
            keyPassword = "widget123"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("fixed")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
