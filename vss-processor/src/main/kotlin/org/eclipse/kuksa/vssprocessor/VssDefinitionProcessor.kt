/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

@file:OptIn(KspExperimental::class)

package org.eclipse.kuksa.vssprocessor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo
import org.eclipse.kuksa.vsscore.annotation.VssDefinition
import org.eclipse.kuksa.vsscore.model.VssNode
import org.eclipse.kuksa.vsscore.model.parentVssPath
import org.eclipse.kuksa.vssprocessor.parser.YamlDefinitionParser
import org.eclipse.kuksa.vssprocessor.spec.SpecModel
import org.eclipse.kuksa.vssprocessor.spec.VssPath
import org.eclipse.kuksa.vssprocessor.spec.VssSpecificationSpecModel
import java.io.File

/**
 * Generates a [VssNode] for every specification listed in the input file.
 * These nodes are a usable kotlin data class reflection of the specification.
 *
 * @param codeGenerator to generate class files with
 * @param logger to log output with
 */
class VssDefinitionProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val visitor = VssDefinitionVisitor()
    private val yamlParser = YamlDefinitionParser()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(VssDefinition::class.qualifiedName.toString())
        val deferredSymbols = symbols.filterNot { it.validate() }

        symbols.forEach { it.accept(visitor, Unit) }

        logger.info("Deferred symbols: ${deferredSymbols.count()}")
        return deferredSymbols.toList()
    }

    private inner class VssDefinitionVisitor : KSVisitorVoid() {
        @OptIn(KspExperimental::class)
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val containingFile = classDeclaration.containingFile ?: return

            codeGenerator.createNewFile(
                Dependencies(false, containingFile),
                PACKAGE_NAME,
                DEFAULT_FILE_NAME,
            )

            val vssDefinition = classDeclaration.getAnnotationsByType(VssDefinition::class).first()
            val vssDefinitionPath = vssDefinition.vssDefinitionPath

            val definitionFile = loadAssetFile(vssDefinitionPath)
            if (!definitionFile.exists()) {
                logger.error("No VSS definition file was found!")
                return
            }

            val simpleSpecificationElements = yamlParser.parseSpecifications(definitionFile)
            val specificationElements = mapChildSpecifications(simpleSpecificationElements)

            generateModelFiles(specificationElements)
        }

        // Uses the default file path for generated files (from the code generator) and searches for the given file.
        private fun loadAssetFile(fileName: String): File {
            val generationPath = codeGenerator.generatedFile.first().absolutePath
            val buildPath = generationPath.replaceAfter(BUILD_FOLDER_NAME, "")
            val assetsFilePath = "$buildPath/$ASSETS_BUILD_DIRECTORY"
            val assetsFolder = File(assetsFilePath)

            return assetsFolder.walk().first { it.name == fileName }
        }

        // Takes all defined elements and nests them accordingly to their parents / child depending on the vssPath.
        // Map<ParentVssPath, ChildElements>
        private fun mapChildSpecifications(
            specificationElements: List<VssSpecificationSpecModel>,
        ): Map<VssPath, VssSpecificationSpecModel> {
            val vssPathToElement = specificationElements.associateBy({ VssPath(it.vssPath) }, { it })
            for (element in specificationElements) {
                val parentKey = VssPath(element.parentVssPath)
                val parentSpecification = vssPathToElement[parentKey] ?: continue

                parentSpecification.childSpecifications.add(element) // It must be a child of the parent
            }

            return vssPathToElement
        }

        private fun generateModelFiles(vssPathToSpecification: Map<VssPath, SpecModel>) {
            val duplicateSpecificationNames = vssPathToSpecification.keys
                .groupBy { it.leaf }
                .filter { it.value.size > 1 }
                .keys

            logger.logging("Ambiguous specifications - Generate nested classes: $duplicateSpecificationNames")

            for ((vssPath, vssSpecification) in vssPathToSpecification) {
                // Every duplicate is produced as a nested class - No separate file should be generated
                if (duplicateSpecificationNames.contains(vssPath.leaf)) {
                    continue
                }

                val classSpec = vssSpecification.createClassSpec(duplicateSpecificationNames, PACKAGE_NAME, logger)

                val file = FileSpec.builder(PACKAGE_NAME, classSpec.name!!)
                    .addType(classSpec)
                    .build()

                file.writeTo(codeGenerator, false)
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "org.eclipse.kuksa.vss"
        private const val DEFAULT_FILE_NAME = "VssProcessor"
        private const val ASSETS_BUILD_DIRECTORY = "intermediates/assets/"
        private const val BUILD_FOLDER_NAME = "build/"
    }
}

/**
 * Provides the environment for the [VssDefinitionProcessor].
 */
class VssDefinitionProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return VssDefinitionProcessor(environment.codeGenerator, environment.logger)
    }
}
