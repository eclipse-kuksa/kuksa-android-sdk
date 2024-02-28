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
 */

package org.eclipse.kuksa.extension

import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.vsscore.model.VssLeaf
import org.eclipse.kuksa.vsscore.model.VssNode
import org.eclipse.kuksa.vsscore.model.vssProperties

/**
 * Finds all [VssLeaf] heirs for the [VssNode] and converts them into a collection of [Property].
 */
fun VssNode.createProperties(
    vararg fields: Types.Field = arrayOf(Types.Field.FIELD_VALUE),
): Collection<Property> {
    return vssProperties
        .map { Property(it.vssPath, fields.toSet()) }
}
