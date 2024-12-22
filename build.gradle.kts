plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.3")
        classpath("com.google.gms:google-services:4.4.2")
    }
}
