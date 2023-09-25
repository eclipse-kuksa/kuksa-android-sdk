package org.eclipse.kuksa.vssprocessor.spec

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.squareup.kotlinpoet.TypeSpec

/**
 * Is used by the [SymbolProcessor] to generate a class spec which can be written into a file.
 */
internal interface SpecModel {
    /**
     * @param nestedClasses which can be used to create a class spec with nested classes.
     * @param packageName to use for the generated class specs.
     * @param logger to use for log output.
     */
    fun createClassSpec(
        nestedClasses: Set<String> = emptySet(),
        packageName: String,
        logger: KSPLogger,
    ): TypeSpec
}
