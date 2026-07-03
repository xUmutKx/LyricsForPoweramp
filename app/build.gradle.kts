plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.google.devtool.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    id("kotlin-parcelize")
    id("com.google.android.gms.oss-licenses-plugin")
}
val appVersionCode = 24
val appVersionName = "3.0"
android {
    namespace = "io.github.abhishekabhi789.lyricsforpoweramp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.abhishekabhi789.lyricsforpoweramp"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField(
            type = "String",
            name = "GITHUB_REPO_URL",
            value = "\"https://github.com/abhishekabhi789/LyricsForPoweramp\""
        )
        buildConfigField(
            type = "String",
            name = "PLAY_STORE_URL",
            value = "\"https://play.google.com/store/apps/details?id=io.github.abhishekabhi789.lyricsforpoweramp\""
        )
        buildConfigField(
            type = "String",
            name = "EMAIL",
            value = "\"justsomerandomapps@gmail.com\""
        )
        buildConfigField(
            type = "String",
            name = "WEBSITE",
            value = "\"https://abhishekabhi789.github.io/\""
        )
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            versionNameSuffix = "-debug"
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk { debugSymbolLevel = "FULL" }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
base {
    archivesName = "Lyrics4Poweramp-v${appVersionName}"
}
dependencies {
    implementation(project(":poweramp_api_lib"))
    implementation(libs.okhttp)
    implementation(libs.taglib)
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.compiler)
    implementation(libs.accompanist.permissions)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.media3.exoplayer)
    implementation(libs.documentfile)
    implementation(libs.core.splashscreen)
    implementation(libs.play.services.oss.licenses)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.work.runtime.ktx)
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(platform(libs.compose.bom))
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
