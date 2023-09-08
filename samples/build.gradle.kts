android {
    namespace = "com.example.samples"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.samples"
        minSdk = 27
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

dependencies {
    implementation(project(":kuksa-sdk")) { isTransitive = false }

    // transitive lib dependencies
    implementation(libs.grpc.protobuf)

    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.stub)
    implementation(libs.tomcat.annotations)
    implementation(libs.kotlinx.coroutines.android)

    // app dependencies
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
}

plugins {
    id("com.android.application")

    kotlin("android")
}
