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

@file:Suppress("UnstableApiUsage")

import org.eclipse.kuksa.property.PropertiesLoader
import org.eclipse.kuksa.version.SemanticVersion
import org.eclipse.kuksa.version.VERSION_FILE_DEFAULT_PATH_KEY

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.eclipse.kuksa.vss-processor-plugin") // Always take the newest version since it's locally included
    kotlin("plugin.serialization")
    kotlin("android")
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtension.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }

    compileSdk = 34

    defaultConfig {
        applicationId = "org.eclipse.kuksa.testapp"
        minSdk = 27
        targetSdk = 34
        vectorDrawables {
            useSupportLibrary = true
        }

        val versionPath = rootProject.ext[VERSION_FILE_DEFAULT_PATH_KEY] as String
        val semanticVersion = SemanticVersion.create(versionPath)
        versionCode = semanticVersion.versionCode
        versionName = semanticVersion.versionName
    }
    signingConfigs {
        create("release") {
            val propertiesLoader = PropertiesLoader()
            val localProperties = propertiesLoader.load("$rootDir/local.properties")

            val keystorePath = System.getenv("KEYSTORE_PATH") ?: localProperties?.getProperty("release.keystore.path")
            println("Defined keystore path: $keystorePath")
            if (keystorePath == null) return@create

            storeFile = File(keystorePath)
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                ?: localProperties?.getProperty("release.keystore.key.alias")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                ?: localProperties?.getProperty("release.keystore.key.password")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                ?: localProperties?.getProperty("release.keystore.store.password")
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
        textOutput = file("stdout")
        textReport = true
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

dependencies {
    implementation(project(":kuksa-sdk"))
    ksp(project(":vss-processor"))
    testImplementation(project(":test"))

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
    debugImplementation(libs.androidx.compose.ui.tooling)

    androidTestImplementation(libs.androidx.compose.ui.tooling.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling.test.manifest)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout.compose)
}
