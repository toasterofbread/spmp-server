plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://jitpack.io")
    gradlePluginPortal()
}

dependencies {
    implementation("com.vanniktech.maven.publish:com.vanniktech.maven.publish.gradle.plugin:0.28.0")
    implementation("com.github.toasterofbread.gradle-jextract:io.github.krakowski.jextract.gradle.plugin:87098f61d3")
    implementation("de.undercouch:gradle-download-task:5.6.0")
}