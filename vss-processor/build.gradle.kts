plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "org.eclipse.kuksa.vss-processor"
version = "0.1.0-SNAPSHOT"

dependencies {
    implementation(project(":vss-core"))

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.symbol.processing.api)

    testImplementation(libs.kotest)
    testImplementation(libs.mockk)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    kotlin {
        compilerOptions {
            // https://youtrack.jetbrains.com/issue/KT-48678/Coroutine-debugger-disable-was-optimised-out-compiler-feature
            // We don't want local variables to be optimized out while debugging into tests
            freeCompilerArgs.add("-Xdebug")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.eclipse.kuksa.vss-processor"
            artifactId = "vss-processor"
            version = "0.1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}
