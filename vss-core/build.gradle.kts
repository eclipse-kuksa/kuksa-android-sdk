plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "org.eclipse.kuksa.vss-core"
version = "1.0.0"

dependencies {
    implementation(kotlin("stdlib"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.eclipse.kuksa.vss-core"
            artifactId = "vss-core"
            version = "1.0.0"

            from(components["java"])
        }
    }
}
