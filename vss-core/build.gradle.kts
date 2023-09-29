plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "org.eclipse.kuksa.vss-core"
version = "0.1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.eclipse.kuksa.vss-core"
            artifactId = "vss-core"
            version = "0.1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}
