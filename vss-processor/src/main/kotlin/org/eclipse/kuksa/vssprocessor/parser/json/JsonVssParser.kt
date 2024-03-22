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
import org.eclipse.kuksa.vssprocessor.parser.KEY_CHILDREN
import org.eclipse.kuksa.vssprocessor.parser.ROOT_KEY_VEHICLE
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.COMMENT
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.DATATYPE
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.DESCRIPTION
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.MAX
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.MIN
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.TYPE
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.UNIT
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.UUID
import org.eclipse.kuksa.vssprocessor.parser.VssParser
import org.eclipse.kuksa.vssprocessor.parser.json.extension.get
import org.eclipse.kuksa.vssprocessor.spec.VssNodePropertiesBuilder
import org.eclipse.kuksa.vssprocessor.spec.VssNodeSpecModel
import java.io.File
import java.io.IOException

internal class JsonVssParser : VssParser {

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

        if (jsonObject.has(KEY_CHILDREN)) {
            val childrenJsonElement = jsonObject.getAsJsonObject(KEY_CHILDREN)

            val filteredKeys = childrenJsonElement.asMap().keys
                .filter { key -> key != KEY_CHILDREN }
                .filter { key -> VssDataKey.findByKey(key) == null }

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
        val uuid = jsonObject.get(UUID)?.asString
            ?: throw JsonParseException("Could not parse '${UUID.key}' for '$vssPath'")

        val type = jsonObject.get(TYPE)?.asString
            ?: throw JsonParseException("Could not parse '${TYPE.key}' for '$vssPath'")

        val description = jsonObject.get(DESCRIPTION)?.asString ?: ""
        val datatype = jsonObject.get(DATATYPE)?.asString ?: ""
        val comment = jsonObject.get(COMMENT)?.asString ?: ""
        val unit = jsonObject.get(UNIT)?.asString ?: ""
        val min = jsonObject.get(MIN)?.asString ?: ""
        val max = jsonObject.get(MAX)?.asString ?: ""

        val vssNodeProperties = VssNodePropertiesBuilder(uuid, type)
            .withDescription(description)
            .withComment(comment)
            .withDataType(datatype)
            .withUnit(unit)
            .withMin(min, datatype)
            .withMax(max, datatype)
            .build()

        return VssNodeSpecModel(vssPath, vssNodeProperties)
    }
}
