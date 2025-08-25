// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.compose) apply false
}
buildscript {
    dependencies {
        classpath(libs.oss.licenses.plugin)
    }
}
subprojects {
    // Align Kotlin with Java 17 globally
    plugins.withId("org.jetbrains.kotlin.android") {
        the<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension>().jvmToolchain(17)
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        the<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>().jvmToolchain(17)
    }
}
