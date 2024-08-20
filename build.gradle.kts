// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.5.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
        classpath("io.realm:realm-gradle-plugin:10.15.1")

        // Add the Google Services plugin
        classpath("com.google.gms:google-services:4.3.15")

        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.4")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://www.jitpack.io" )
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
