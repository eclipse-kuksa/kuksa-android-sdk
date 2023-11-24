plugins {
    kotlin("jvm")
    publish
    alias(libs.plugins.dokka)
}

group = "org.eclipse.kuksa"
version = rootProject.extra["projectVersion"].toString()

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

configure<Publish_gradle.PublishPluginExtension> {
    mavenPublicationName = "release"
    componentName = "java"
    description = "Vehicle Signal Specification (VSS) Code Generator for the KUKSA SDK"
}

tasks.register("javadocJar", Jar::class) {
    dependsOn("dokkaHtml")
    from("$buildDir/dokka/html")
    archiveClassifier.set("javadoc")
}

java {
    withJavadocJar() // needs to be called after tasks.register("javadocJar")
    withSourcesJar()
}
