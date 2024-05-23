plugins {
    kotlin("multiplatform") version "1.9.22" apply false
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.24.0")
    }
}

allprojects {

    group = "dev.gitlive"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

}
