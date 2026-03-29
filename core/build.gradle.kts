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
    api(project(":rex"))
    api(project(":base"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.core)
}

tasks.test {
    useJUnitPlatform()
}
