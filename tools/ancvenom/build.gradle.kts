plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("ancvenom.MainKt")
}

dependencies {
    implementation(project(":modules"))
    implementation(project(":core"))
    implementation(project(":rex"))
    implementation(project(":base"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)
    implementation(libs.clikt)
    implementation(libs.mordant)
}

tasks.jar {
    manifest { attributes["Main-Class"] = "ancvenom.MainKt" }
    // Fat jar — bundle all dependencies
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
