plugins {
    kotlin("jvm")
}

group = "com.etas.kuksa.vss-processor"
version = "1.0.0"

dependencies {
    implementation(kotlin("stdlib"))

    implementation(libs.kotlinpoet)
    implementation(libs.symbol.processing.api)
}
