/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import org.eclipse.kuksa.version.SemanticVersion

val file = File("$rootDir/version.txt")
val fileContent = file.readText()
val semanticVersion = SemanticVersion(fileContent)

updateExtras()

// Do not chain this command because it writes into a file which needs to be re-read inside the next gradle command
tasks.register("setReleaseVersion") {
    group = "version"
    doLast {
        semanticVersion.suffix = ""

        updateVersion()
    }
}

// Do not chain this command because it writes into a file which needs to be re-read inside the next gradle command
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
