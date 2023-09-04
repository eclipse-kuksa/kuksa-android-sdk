package com.etas.kuksa.vssprocessor

interface VssSpecification {
    val uuid: String
    val vssPath: String
    val description: String
    val type: String
}

interface VSSProperty<T> {
    val value: T
}
