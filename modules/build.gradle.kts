plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)
    implementation(libs.okhttp)

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.core)
}

tasks.test {
    useJUnitPlatform()
}
