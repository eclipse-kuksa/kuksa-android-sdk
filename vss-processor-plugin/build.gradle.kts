import org.eclipse.kuksa.vssprocessor.plugin.version.SemanticVersion
import org.eclipse.kuksa.vssprocessor.plugin.version.VERSION_FILE_DEFAULT_NAME
import org.eclipse.kuksa.vssprocessor.plugin.version.VERSION_FILE_DEFAULT_PATH_KEY

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

val versionDefaultPath = "$rootDir/../$VERSION_FILE_DEFAULT_NAME"
rootProject.ext[VERSION_FILE_DEFAULT_PATH_KEY] = versionDefaultPath

plugins {
    alias(libs.plugins.pluginPublishing)
    signing
    `kotlin-dsl`
}

val versionPath = rootProject.ext[VERSION_FILE_DEFAULT_PATH_KEY] as String
val semanticVersion = SemanticVersion(versionPath)
version = semanticVersion.versionName
group = "org.eclipse.kuksa"

gradlePlugin {
    website.set("https://github.com/eclipse-kuksa/kuksa-android-sdk")
    vcsUrl.set("https://github.com/eclipse-kuksa/kuksa-android-sdk")
    plugins {
        create("VssProcessorPlugin") {
            id = "org.eclipse.kuksa.vss-processor-plugin"
            implementationClass = "org.eclipse.kuksa.vssprocessor.plugin.VssProcessorPlugin"
            displayName = "VSS Processor Plugin"
            tags.set(listOf("KUKSA", "Vehicle Signal Specification", "VSS", "Android", "Kotlin"))
            description = "Vehicle Signal Specification (VSS) Plugin of the KUKSA SDK. This is used in combination " +
                "with the KSP processor component 'KUKSA VSS Processor'. The plugin is configured to provide " +
                "VSS Files to KSP processor. This is mandatory to use the 'KUKSA VSS Processor' component."
        }
    }
}

// <property>.set calls need to be done instead of "=" because of a IDEA bug.
// https://youtrack.jetbrains.com/issue/KTIJ-17783/False-positive-Val-cannot-be-reassigned-in-build.gradle.kts
afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "OSSRHRelease"

                url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("ORG_OSSRH_USERNAME")
                    password = System.getenv("ORG_OSSRH_PASSWORD")
                }
            }
            // Snapshot are disabled for Plugins since the plugin marker has issues finding the correct jar with the
            // automatic timestamps / build number being added as a postfix to the files.
        }
        publications {
            getByName<MavenPublication>("pluginMaven") {
                pom {
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            name.set("Mark Hüsers")
                            email.set("mark.huesers@etas.com")
                            organization.set("ETAS GmbH")
                            organizationUrl.set("https://www.etas.com")
                        }
                        developer {
                            name.set("Sebastian Schildt")
                            email.set("sebastian.schildt@etas.com")
                            organization.set("ETAS GmbH")
                            organizationUrl.set("https://www.etas.com")
                        }
                        developer {
                            name.set("Andre Weber")
                            email.set("andre.weber3@etas.com")
                            organization.set("ETAS GmbH")
                            organizationUrl.set("https://www.etas.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/eclipse-kuksa/kuksa-android-sdk.git")
                        developerConnection.set("scm:git:ssh://github.com/eclipse-kuksa/kuksa-android-sdk.git")
                        url.set("https://github.com/eclipse-kuksa/kuksa-android-sdk/tree/main")
                    }
                }
            }
        }
    }

    signing {
        var keyId: String? = System.getenv("ORG_GPG_KEY_ID")
        if (keyId != null && keyId.length > 8) {
            keyId = keyId.takeLast(8)
        }
        val privateKey = System.getenv("ORG_GPG_PRIVATE_KEY")
        val passphrase = System.getenv("ORG_GPG_PASSPHRASE")

        useInMemoryPgpKeys(
            keyId,
            privateKey,
            passphrase,
        )

        sign(publishing.publications)

        setRequired({
            val publishToMavenLocalTask = gradle.taskGraph.allTasks.find { it.name.contains("ToMavenLocal") }
            val isPublishingToMavenLocal = publishToMavenLocalTask != null

            !isPublishingToMavenLocal // disable signing when publishing to MavenLocal
        })
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(gradleTestKit())
    testImplementation(libs.kotest)
    testImplementation(libs.mockk)
}