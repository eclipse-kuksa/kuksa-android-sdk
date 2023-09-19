plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "org.eclipse.kuksa.vss-processor"
version = "1.0.0"

dependencies {
    implementation(project(":vss-core"))

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.symbol.processing.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.eclipse.kuksa.vss-processor"
            artifactId = "vss-processor"
            version = "1.0.0"

            from(components["java"])
        }
    }
}
