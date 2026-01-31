import com.android.build.gradle.ProguardFiles.getDefaultProguardFile
import org.gradle.kotlin.dsl.android
import org.gradle.kotlin.dsl.ksp
import org.gradle.kotlin.dsl.libs
import org.jetbrains.kotlin.gradle.internal.types.error.ErrorModuleDescriptor.platform

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.google.devtool.ksp)
    alias(libs.plugins.hilt.android)
    id("kotlin-parcelize")
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    namespace = "io.github.abhishekabhi789.lyricsforpoweramp"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.abhishekabhi789.lyricsforpoweramp"
        minSdk = 23
        targetSdk = 36
        versionCode = 20
        versionName = "2.0"
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
    base {
        archivesName.set("Lyrics4Poweramp-v${defaultConfig.versionName}")
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
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
    implementation(libs.gson)
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
    implementation(libs.foundation.android)
    implementation(libs.work.runtime.ktx)
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(platform(libs.compose.bom))
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
