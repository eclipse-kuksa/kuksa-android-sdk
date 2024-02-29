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

package org.eclipse.kuksa.vssprocessor.parser

import org.eclipse.kuksa.vssprocessor.spec.VssNodeSpecModel
import java.io.File

internal interface VssParser {
    /**
     * @param definitionFile to parse [VssNodeSpecModel] with
     * @param elementDelimiter which is the separator string between the specifications. The default is an empty line.
     */
    fun parseNodes(
        definitionFile: File,
        elementDelimiter: String = "",
    ): List<VssNodeSpecModel>
}
