plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)
    implementation(libs.okhttp)
    implementation(libs.bouncy.castle)
    implementation(libs.serialization.json)

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}
