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
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import org.eclipse.kuksa.vsscore.annotation.VssModelGenerator
import org.eclipse.kuksa.vsscore.model.parentClassName
import org.eclipse.kuksa.vssprocessor.parser.factory.VssParserFactory
import org.eclipse.kuksa.vssprocessor.spec.VssNodeSpecModel
import org.eclipse.kuksa.vssprocessor.spec.VssPath
import java.io.File

/**
 * Generates a [org.eclipse.kuksa.vsscore.model.VssNode] for every entry listed in the input file.
 * These nodes are a usable kotlin data class reflection of the element.
 *
 * @param codeGenerator to generate class files with
 * @param logger to log output with
 */
class VssModelGeneratorProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val visitor = VssModelGeneratorVisitor()
    private val vssParserFactory = VssParserFactory()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(VssModelGenerator::class.qualifiedName.toString())
        val deferredSymbols = symbols.filterNot { it.validate() }

        symbols.forEach { it.accept(visitor, Unit) }

        logger.info("Deferred symbols: $deferredSymbols")
        return emptyList()
    }

    private inner class VssModelGeneratorVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val containingFile = classDeclaration.containingFile ?: return

            val dependencies = Dependencies(false, containingFile)
            val annotatedProcessorFileName = classDeclaration.toClassName().simpleName + FILE_NAME_PROCESSOR_POSTFIX
            codeGenerator.createNewFile(
                dependencies,
                PACKAGE_NAME,
                annotatedProcessorFileName,
            )

            val vssFiles = loadVssFiles()
            if (vssFiles.isEmpty()) {
                logger.error("No VSS files were found! Is the plugin correctly configured?")
                return
            }

            val simpleNodeElements = mutableListOf<VssNodeSpecModel>()
            vssFiles.forEach { definitionFile ->
                logger.info("Parsing models for definition file: ${definitionFile.name}")
                val vssParser = vssParserFactory.create(definitionFile)
                val specModels = vssParser.parseNodes(definitionFile)

                simpleNodeElements.addAll(specModels)
            }

            val vssPathToVssNodeElement = simpleNodeElements
                .distinctBy { it.uuid }
                .associateBy({ VssPath(it.vssPath) }, { it })

            generateModelFiles(vssPathToVssNodeElement)
        }

        // Uses the default file path for generated files (from the code generator) and searches for the given file.
        private fun loadVssFiles(): Collection<File> {
            val generatedFile = codeGenerator.generatedFile.firstOrNull() ?: return emptySet()
            val generationPath = generatedFile.absolutePath
            val buildPath = generationPath.replaceAfterLast("$BUILD_FOLDER_NAME$fileSeparator", "")
            val kspInputFilePath = "$buildPath$fileSeparator$KSP_INPUT_BUILD_DIRECTORY"
            val kspInputFolder = File(kspInputFilePath)

            return kspInputFolder
                .walk()
                .filter { it.isFile }
                .toSet()
        }

        private fun generateModelFiles(vssPathToVssNode: Map<VssPath, VssNodeSpecModel>) {
            val duplicateNodeNames = vssPathToVssNode.keys
                .groupBy { it.leaf }
                .filter { it.value.size > 1 }
                .keys

            logger.info("Ambiguous VssNode - Generate nested classes: $duplicateNodeNames")

            val generatedFilesVssPathToClassName = mutableMapOf<String, String>()
            for ((vssPath, specModel) in vssPathToVssNode) {
                // Every duplicate is produced as a nested class - No separate file should be generated
                if (duplicateNodeNames.contains(vssPath.leaf)) {
                    continue
                }

                specModel.logger = logger
                val classSpec = specModel.createClassSpec(
                    PACKAGE_NAME,
                    vssPathToVssNode.values,
                    duplicateNodeNames,
                )

                val className = classSpec.name ?: throw NoSuchFieldException("Class spec $classSpec has no name field!")
                val fileSpecBuilder = FileSpec.builder(PACKAGE_NAME, className)

                val parentImport = buildParentImport(specModel, generatedFilesVssPathToClassName)
                if (parentImport.isNotEmpty()) {
                    fileSpecBuilder.addImport(PACKAGE_NAME, parentImport)
                }

                val file = fileSpecBuilder
                    .addType(classSpec)
                    .build()

                file.writeTo(codeGenerator, false)
                generatedFilesVssPathToClassName[vssPath.path] = className
            }
        }

        // Uses a map of vssPaths to ClassNames which are validated if it contains a parent of the given specModel.
        // If the actual parent is a sub class (Driver) in another class file (e.g. Vehicle) then this method returns
        // a sub import e.g. "Vehicle.Driver". Otherwise just "Vehicle" is returned.
        private fun buildParentImport(
            specModel: VssNodeSpecModel,
            parentVssPathToClassName: Map<String, String>,
        ): String {
            var availableParentVssPath = specModel.vssPath
            var parentSpecClassName: String? = null

            // Iterate up from the parent until the actual file name = class name was found. This indicates
            // that the actual parent is a sub class in this file.
            while (availableParentVssPath.contains(".")) {
                availableParentVssPath = availableParentVssPath.substringBeforeLast(".")

                parentSpecClassName = parentVssPathToClassName[availableParentVssPath]
                if (parentSpecClassName != null) break
            }

            if (parentSpecClassName == null) {
                logger.info("Could not create import string for: ${specModel.vssPath} - No parent was found")
                return ""
            }

            val parentClassName = specModel.parentClassName

            return if (parentSpecClassName != parentClassName) {
                "$parentSpecClassName.$parentClassName" // Sub class in another file
            } else {
                parentClassName // Main class = File name
            }
        }
    }

    private companion object {
        private const val PACKAGE_NAME = "org.eclipse.kuksa.vss"
        private const val FILE_NAME_PROCESSOR_POSTFIX = "Processor"
        private const val KSP_INPUT_BUILD_DIRECTORY = "kspInput"
        private const val BUILD_FOLDER_NAME = "build"

        private val fileSeparator = File.separator
    }
}

/**
 * Provides the environment for the [VssModelGeneratorProcessor].
 */
class VssModelGeneratorProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return VssModelGeneratorProcessor(environment.codeGenerator, environment.logger)
    }
}
