plugins {
    base
    detekt
}

subprojects {
    apply {
        plugin("ktlint")
    }
    afterEvaluate {
        tasks.check {
            finalizedBy("ktlintCheck")
        }
    }

    // see: https://kotest.io/docs/framework/tags.html#gradle
    tasks.withType<Test> {
        val systemPropertiesMap = HashMap<String, Any>()
        System.getProperties().forEach { key, value ->
            systemPropertiesMap[key.toString()] = value.toString()
        }
        systemProperties = systemPropertiesMap
    }
}
