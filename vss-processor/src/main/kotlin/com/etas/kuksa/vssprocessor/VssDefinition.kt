package com.etas.kuksa.vssprocessor

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class VssDefinition(val vssDefinitionPath: String)
