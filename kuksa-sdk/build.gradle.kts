import com.google.protobuf.gradle.id

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")

    `maven-publish`
}

android {
    namespace = "org.eclipse.kuksa"
    compileSdk = 34
    version = "0.1.0-SNAPSHOT"

    defaultConfig {
        minSdk = 27

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
        kotlin {
            compilerOptions {
                // https://youtrack.jetbrains.com/issue/KT-48678/Coroutine-debugger-disable-was-optimised-out-compiler-feature
                // We don't want local variables to be optimized out while debugging into tests
                freeCompilerArgs.add("-Xdebug")
            }
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    api(project(":vss-core")) // Models are exposed

    // needs to be api as long as we expose ProtoBuf specific objects
    api(libs.grpc.protobuf)

    implementation(kotlin("reflect"))

    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.stub)
    implementation(libs.tomcat.annotations)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotest)
    testImplementation(libs.mockk)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        id("grpc") {
            artifact = libs.grpc.protoc.java.gen.get().toString()
        }
        generateProtoTasks {
            all().forEach {
                it.builtins {
                    create("java") {
                        option("lite")
                    }
                }
                it.plugins {
                    create("grpc") {
                        option("lite")
                    }
                }
            }
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "org.eclipse.kuksa"
            artifactId = "kuksa-sdk"
            version = "0.1.0-SNAPSHOT"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
