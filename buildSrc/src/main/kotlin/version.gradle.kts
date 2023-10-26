import org.eclipse.kuksa.util.Version

val file = File("$rootDir/version")
val semanticVersion = file.readText()
val version = Version(semanticVersion)

updateExtras()

tasks.register("setReleaseVersion") {
    group = "version"
    doLast {
        version.suffix = ""

        updateExtras()
    }
}

tasks.register("setSnapshotVersion") {
    group = "version"
    doLast {
        version.suffix = "SNAPSHOT"

        updateExtras()
    }
}

tasks.register("printVersion") {
    group = "version"
    doLast {
        val version = version.version

        println("VERSION=$version")
    }

    mustRunAfter("setReleaseVersion", "setSnapshotVersion")
}


fun updateExtras() {
    rootProject.extra["projectVersion"] = version.version
    rootProject.extra["projectVersionCode"] = version.versionCode
}
