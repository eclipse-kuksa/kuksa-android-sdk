import org.eclipse.kuksa.util.SemanticVersion

val file = File("$rootDir/version.txt")
val fileContent = file.readText()
val semanticVersion = SemanticVersion(fileContent)

updateExtras()

tasks.register("setReleaseVersion") {
    group = "version"
    doLast {
        semanticVersion.suffix = ""

        updateVersion()
    }
}

tasks.register("setSnapshotVersion") {
    group = "version"
    doLast {
        semanticVersion.suffix = "SNAPSHOT"

        updateVersion()
    }
}

tasks.register("printVersion") {
    group = "version"
    doLast {
        val version = semanticVersion.versionString

        println("VERSION=$version")
    }

    mustRunAfter("setReleaseVersion", "setSnapshotVersion")
}

fun updateExtras() {
    rootProject.extra["projectVersion"] = semanticVersion.versionString
    rootProject.extra["projectVersionCode"] = semanticVersion.versionCode
}

fun updateVersion() {
    updateExtras()

    file.writeText(semanticVersion.versionString)
}
