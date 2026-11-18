import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// App identity comes from ../app.properties, so the same template can be
// reused for every game without touching Gradle files. Any key can be
// overridden from the command line: -Papp.id=com.example.other
val appConfig = Properties().apply {
    val file = rootProject.file("../app.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun setting(key: String, fallback: String): String =
    providers.gradleProperty(key).orElse(appConfig.getProperty(key) ?: fallback).get()

val appId = setting("app.id", "com.example.penguingame")
val appName = setting("app.name", "PenguinMod Game")
val appOrientation = setting("app.orientation", "sensorLandscape")

// Optional ../signing.properties (gitignored) with storeFile, storePassword,
// keyAlias and keyPassword. Without it, release APKs are built unsigned.
val signingFile = rootProject.file("../signing.properties")
val signing = Properties().apply {
    if (signingFile.exists()) signingFile.inputStream().use { load(it) }
}

android {
    // Fixed Java package for the launcher activity; changing app.id does not
    // require moving source files because the manifest uses the full class name.
    namespace = "com.penguinmod.template"
    compileSdk = 36

    if (signingFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(signing.getProperty("storeFile"))
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = appId
        minSdk = 24
        targetSdk = 36
        versionCode = setting("app.versionCode", "1").toInt()
        versionName = setting("app.versionName", "1.0.0")

        resValue("string", "app_name", appName)
        manifestPlaceholders["orientation"] = appOrientation
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        // Keep game assets uncompressed in the APK. Compressed assets must be
        // deflate-decompressed on every read, which is a large CPU cost when the
        // game loads hundreds of images/sounds at startup. These formats are
        // already compressed internally, so the APK barely grows. If your game
        // uses custom asset extensions, add them here too.
        noCompress += listOf(
            "png", "jpg", "jpeg", "gif", "webp", "svg",
            "ttf", "otf", "woff", "mp3", "ogg", "wav",
            "js", "json", "html", "css"
        )
    }
}

dependencies {
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.foundation)
}
