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

package org.eclipse.kuksa.vssprocessor.parser.json.extension

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey

internal fun JsonObject.has(vssDataKey: VssDataKey): Boolean {
    return has(vssDataKey.key)
}

internal fun JsonObject.getAsJsonObject(vssDataKey: VssDataKey): JsonObject {
    return getAsJsonObject(vssDataKey.key)
}

internal fun JsonObject.get(vssDataKey: VssDataKey): JsonElement? {
    return get(vssDataKey.key)
}
