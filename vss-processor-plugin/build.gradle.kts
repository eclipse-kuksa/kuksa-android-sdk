import org.eclipse.kuksa.vssprocessor.plugin.version.SemanticVersion
import org.eclipse.kuksa.vssprocessor.plugin.version.VERSION_FILE_DEFAULT_NAME

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

plugins {
    alias(libs.plugins.pluginPublishing)
    signing
    `kotlin-dsl`
}

// TODO: This linking to the version file currently throws a warning:
// Caught exception: Already watching path: kuksa-android-sdk/vss-processor-plugin/..
//
// The reason is that two different root projects (main + composite (this)) are referencing to the same version.txt
// file because data models like SemanticVersion can't be shared. However the same build folder is used for the
// caching so the cache does not know about this. The issue will be ignored as a warning for now.
//
// Similar issue: https://github.com/gradle/gradle/issues/27940
val pluginDescription = "Vehicle Signal Specification (VSS) Plugin of the KUKSA SDK. This is used in combination " +
    "with the KSP processor component 'KUKSA VSS Processor'. The plugin is configured to provide " +
    "VSS Files to KSP processor. This is mandatory to use the 'KUKSA VSS Processor' component."

val versionPath = "$rootDir/../$VERSION_FILE_DEFAULT_NAME"
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
            description = pluginDescription
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
            all {
                with((this as MavenPublication)) {
                    pom {
                        name = "${project.group}:${project.name}"
                        description = pluginDescription
                        url = "https://github.com/eclipse-kuksa/kuksa-android-sdk"
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

afterEvaluate {
    tasks.find { it.name.contains("generateMetadataFileForPluginMavenPublication") }
        ?.notCompatibleWithConfigurationCache(
            """
            The "generateMetadataFileForPluginMavenPublication" task is currently not using the configuration cache
            correctly which leads to:

            java.io.FileNotFoundException: /build/libs/vss-processor-plugin-0.1.3.jar (No such file or directory)

            Reproduction steps:
            ./gradlew :vss-processor-plugin:clean :vss-processor-plugin:generateMetadataFileForPluginMavenPublication
            """.trimIndent(),
        )
}

// IMPORTANT: The currently used dependencies here are already covered by the other modules in this project so dash oss
// scripts do not have to be included here (yet).
// But keep in mind to check the coverage when adding new dependencies.
// TODO: Automated with https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/79
dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(gradleTestKit())
    testImplementation(libs.kotest)
    testImplementation(libs.mockk)
}
