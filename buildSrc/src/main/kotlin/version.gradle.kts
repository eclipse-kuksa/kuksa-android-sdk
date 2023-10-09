import org.eclipse.kuksa.util.VersionProperties

val properties = VersionProperties("$rootDir/version.properties")
properties.load()

rootProject.extra["projectVersion"] = properties.version
rootProject.extra["projectVersionCode"] = properties.versionCode

tasks.register("increaseMajorVersion") {
    group = "version"
    doLast {
        properties.major += 1
        properties.minor = 0
        properties.patch = 0
        properties.store()
    }
}

tasks.register("increaseMinorVersion") {
    group = "version"
    doLast {
        properties.minor += 1
        properties.patch += 0
        properties.store()
    }
}

tasks.register("increasePatchVersion") {
    group = "version"
    doLast {
        properties.patch += 1
        properties.store()
    }
}

tasks.register("setReleaseVersion") {
    group = "version"
    doLast {
        properties.suffix = ""
        properties.store()
    }
}

tasks.register("setSnapshotVersion") {
    group = "version"
    doLast {
        properties.suffix = "SNAPSHOT"
        properties.store()
    }
}

tasks.register("printVersion") {
    group = "version"
    doLast {
        val version = properties.version

        println("VERSION=$version")
    }
}

tasks.register("printVersionCode") {
    group = "version"
    doLast {
        val versionCode = properties.versionCode

        println("VERSION_CODE=$versionCode")
    }
}
