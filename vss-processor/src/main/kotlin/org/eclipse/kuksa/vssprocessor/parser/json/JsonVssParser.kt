/*
 * Copyright (c) 2023 - 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.vssprocessor.parser.json

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import org.eclipse.kuksa.vssprocessor.parser.VssParser
import org.eclipse.kuksa.vssprocessor.spec.VssNodeSpecModel
import java.io.File
import java.io.IOException

private const val ROOT_KEY_VEHICLE = "Vehicle"

private const val KEY_DATA_DESCRIPTION = "description"
private const val KEY_DATA_TYPE = "type"
private const val KEY_DATA_UUID = "uuid"
private const val KEY_DATA_COMMENT = "comment"
private const val KEY_DATA_DATATYPE = "datatype"
private const val KEY_DATA_CHILDREN = "children"

internal class JsonVssParser : VssParser {
    private val dataKeys = listOf(
        KEY_DATA_DESCRIPTION,
        KEY_DATA_TYPE,
        KEY_DATA_UUID,
        KEY_DATA_COMMENT,
        KEY_DATA_DATATYPE,
        KEY_DATA_CHILDREN,
    )

    override fun parseNodes(vssFile: File): List<VssNodeSpecModel> {
        val vssNodeSpecModels = mutableListOf<VssNodeSpecModel>()

        try {
            val jsonStreamReader = vssFile.reader()

            val gson = Gson()
            val rootJsonObject = gson.fromJson(jsonStreamReader, JsonObject::class.java)

            if (rootJsonObject.has(ROOT_KEY_VEHICLE)) {
                val vehicleJsonObject = rootJsonObject.getAsJsonObject(ROOT_KEY_VEHICLE)
                vssNodeSpecModels += parseSpecModels(ROOT_KEY_VEHICLE, vehicleJsonObject)
            } else {
                throw IOException("Invalid VSS file '${vssFile.path}'")
            }
        } catch (e: JsonParseException) {
            throw IOException("Invalid VSS file '${vssFile.path}'", e)
        }

        return vssNodeSpecModels.toList()
    }

    private fun parseSpecModels(
        vssPath: String,
        jsonObject: JsonObject,
    ): Collection<VssNodeSpecModel> {
        val parsedSpecModels = mutableListOf<VssNodeSpecModel>()

        val parsedSpecModel = parseSpecModel(vssPath, jsonObject)
        parsedSpecModels += parsedSpecModel

        if (jsonObject.has(KEY_DATA_CHILDREN)) {
            val childrenJsonElement = jsonObject.getAsJsonObject(KEY_DATA_CHILDREN)

            val filteredKeys = childrenJsonElement.asMap().keys
                .filter { key -> !dataKeys.contains(key) }

            filteredKeys.forEach { key ->
                val childJsonElement = childrenJsonElement.getAsJsonObject(key)
                val newVssPath = "$vssPath.$key"
                // recursively go deeper in hierarchy and parse next element
                parsedSpecModels += parseSpecModels(newVssPath, childJsonElement)
            }
        }

        return parsedSpecModels
    }

    private fun parseSpecModel(
        vssPath: String,
        jsonObject: JsonObject,
    ): VssNodeSpecModel {
        val uuid = jsonObject.get(KEY_DATA_UUID).asString
            ?: throw JsonParseException("Could not parse '$KEY_DATA_UUID' for '$vssPath'")

        val type = jsonObject.get(KEY_DATA_TYPE).asString
            ?: throw JsonParseException("Could not parse '$KEY_DATA_TYPE' for '$vssPath'")

        val description = jsonObject.get(KEY_DATA_DESCRIPTION).asString ?: ""
        val datatype = jsonObject.get(KEY_DATA_DATATYPE)?.asString ?: ""
        val comment = jsonObject.get(KEY_DATA_COMMENT)?.asString ?: ""

        return VssNodeSpecModel(uuid, vssPath, description, type, comment, datatype)
    }
}
