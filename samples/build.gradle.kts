import org.eclipse.kuksa.version.SemanticVersion
import org.eclipse.kuksa.version.VERSION_FILE_DEFAULT_PATH_KEY

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
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.eclipse.kuksa.vss-processor-plugin")
    kotlin("android")
}

android {
    namespace = "org.eclipse.kuksa.samples"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.eclipse.kuksa.samples"
        minSdk = 27
        targetSdk = 34

        val versionPath = rootProject.ext[VERSION_FILE_DEFAULT_PATH_KEY] as String
        val semanticVersion = SemanticVersion(versionPath)
        versionCode = semanticVersion.versionCode
        versionName = semanticVersion.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

vssProcessor {
    // Optional - See plugin documentation. Files inside the "$rootDir/vss" folder are used automatically.
    searchPath = "$rootDir/vss"
}

dependencies {
    implementation(project(":kuksa-sdk")) // org.eclipse.kuksa.kuksa-sdk:<VERSION>
    ksp(project(":vss-processor")) // org.eclipse.kuksa.vss-processor:<VERSION>

    // app dependencies
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
}
