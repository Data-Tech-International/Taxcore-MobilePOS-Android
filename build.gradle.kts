// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.13.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
        classpath("io.realm:realm-gradle-plugin:10.19.0")

        // Add the Google Services plugin
        classpath("com.google.gms:google-services:4.4.4")

        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.6")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    afterEvaluate {
        extensions.findByName("kapt")?.let {
            (it as org.jetbrains.kotlin.gradle.plugin.KaptExtension).arguments {
                arg("dagger.formatGeneratedSource", "disabled")
                arg("dagger.gradle.incremental", "enabled")
            }
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
