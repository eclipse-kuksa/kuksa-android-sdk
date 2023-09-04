plugins {
    kotlin("jvm")
}

group = "com.etas.kuksa.vss-processor"
version = "1.0.0"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation(libs.kotlinpoet)
    implementation(libs.symbol.processing.api)
    implementation(libs.kotlinx.serialization.json)
}
