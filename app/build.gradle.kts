plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
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
    kotlin {
        sourceSets.main {
            kotlin.srcDir("build/generated/ksp/main/kotlin")
        }
        sourceSets.test {
            kotlin.srcDir("build/generated/ksp/test/kotlin")
        }
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
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
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
                proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            }
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

dependencies {
    implementation(project(":kuksa-sdk"))
    implementation(project(":vss-processor"))
    ksp(project(":vss-processor"))

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
}
