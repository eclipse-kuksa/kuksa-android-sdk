/*
 * Copyright (c) 2023 - 2026 Contributors to the Eclipse Foundation
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

@file:Suppress("UnstableApiUsage")

import org.eclipse.kuksa.property.PropertiesLoader
import org.eclipse.kuksa.version.SemanticVersion
import org.eclipse.kuksa.version.VERSION_FILE_DEFAULT_PATH_KEY
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.eclipse.velocitas.vss-processor-plugin") version "0.1.3"
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
}

android {
    val jvmTarget = libs.versions.jvmTarget.get()
    compileOptions {
        val javaVersion = JavaVersion.toVersion(jvmTarget)
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }

    compileSdk = 37

    defaultConfig {
        applicationId = "org.eclipse.kuksa.testapp"
        minSdk = 27
        targetSdk = 37
        vectorDrawables {
            useSupportLibrary = true
        }

        val versionPath = rootProject.ext[VERSION_FILE_DEFAULT_PATH_KEY] as String
        val semanticVersion = SemanticVersion(versionPath)
        versionCode = semanticVersion.versionCode
        versionName = semanticVersion.versionName
    }
    signingConfigs {
        val propertiesLoader = PropertiesLoader()
        val localProperties = propertiesLoader.load("$rootDir/local.properties")

        val keystoreFile = resolveKeystoreFile(localProperties)
        if (keystoreFile != null) {
            val credentials = validateSigningCredentials(keystoreFile, localProperties)
            create("release") {
                storeFile = credentials.storeFile
                keyAlias = credentials.keyAlias
                keyPassword = credentials.keyPassword
                storePassword = credentials.storePassword
            }
        }
    }
    buildTypes {
        // for local builds, used to find shrinking issues
        val isMinify = project.hasProperty("minify")
        if (isMinify) {
            debug {
                // while isDebuggable is set to true no obfuscation takes place,
                // the shrinking phase will still remove unused classes
                isDebuggable = true

                isMinifyEnabled = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            }
        }

        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    namespace = "org.eclipse.kuksa.testapp"
    lint {
        disable += mutableListOf(
            "GoogleAppIndexingWarning",
            "HardcodedText",
            "InvalidPackage",
            "AutoboxingStateCreation",
        )
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
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

vssProcessor {
    searchPath = "$rootDir/vss"
}

dependencies {
    implementation(project(":kuksa-sdk"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)

    testImplementation(libs.kotest)

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    androidTestImplementation(libs.androidx.compose.ui.tooling.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling.test.manifest)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout.compose)
}

data class SigningCredentials(
    val storeFile: File,
    val keyAlias: String,
    val keyPassword: String,
    val storePassword: String,
)

fun resolveKeystoreFile(localProperties: Properties?): File? {
    val rawKeystorePath = System.getenv("KEYSTORE_PATH")
        ?: localProperties?.getProperty("release.keystore.path")
    val keystorePath = rawKeystorePath?.replaceFirst(
        "^~".toRegex(),
        System.getProperty("user.home"),
    ) ?: return null

    val keystoreFile = File(keystorePath)
    if (!keystoreFile.exists()) {
        throw GradleException("Release keystore file does not exist at: $keystorePath")
    }
    return keystoreFile
}

fun validateSigningCredentials(
    keystoreFile: File,
    localProperties: Properties?,
): SigningCredentials {
    val keyAlias = System.getenv("SIGNING_KEY_ALIAS")
        ?: localProperties?.getProperty("release.keystore.key.alias")
    val keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        ?: localProperties?.getProperty("release.keystore.key.password")
    val storePassword = System.getenv("SIGNING_STORE_PASSWORD")
        ?: localProperties?.getProperty("release.keystore.store.password")

    val missingProps = mutableListOf<String>()
    if (keyAlias.isNullOrBlank()) {
        missingProps.add("SIGNING_KEY_ALIAS / release.keystore.key.alias")
    }
    if (keyPassword.isNullOrBlank()) {
        missingProps.add("SIGNING_KEY_PASSWORD / release.keystore.key.password")
    }
    if (storePassword.isNullOrBlank()) {
        missingProps.add("SIGNING_STORE_PASSWORD / release.keystore.store.password")
    }

    if (missingProps.isNotEmpty()) {
        val missingDetails = missingProps.joinToString(", ")
        throw GradleException(
            "Release keystore path is set to '${keystoreFile.path}', but signing properties are missing: " +
                "$missingDetails. Please set them via environment variables or in local.properties.",
        )
    }

    return SigningCredentials(
        storeFile = keystoreFile,
        keyAlias = keyAlias!!,
        keyPassword = keyPassword!!,
        storePassword = storePassword!!,
    )
}
