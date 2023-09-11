import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

val baselineFile = project.file("$rootDir/config/detekt/baseline.xml")

plugins {
    id("io.gitlab.arturbosch.detekt") // see https://github.com/detekt/detekt
}

dependencies {
    detektPlugins(lib("detekt-formatting"))
}

detekt {
    buildUponDefaultConfig = true
    allRules = false // only stable rules
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
    }

    parallel = true
    setSource(projectDir)
    include("**/*.kt", "**/*.kts")
    exclude("**/resources/**", "**/build/**")
    config.setFrom(project.file("$rootDir/config/detekt/config.yml"))
    baseline.set(baselineFile)

    jvmTarget = "1.8"
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "1.8"

    setSource(projectDir)
    baseline.set(baselineFile)
    include("**/*.kt", "**/*.kts")
    exclude("**/resources/**", "**/build/**")
    config.setFrom(project.file("$rootDir/config/detekt/config.yml"))
}
