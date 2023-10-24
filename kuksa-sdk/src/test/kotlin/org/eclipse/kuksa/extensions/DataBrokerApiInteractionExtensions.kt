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

package org.eclipse.kuksa.extensions

import io.kotest.assertions.fail
import org.eclipse.kuksa.DataBrokerApiInteraction
import org.eclipse.kuksa.proto.v1.Types
import kotlin.random.Random

internal suspend fun DataBrokerApiInteraction.updateRandomFloatValue(vssPath: String, maxValue: Int = 300): Float {
    val random = Random(System.nanoTime())
    val randomValue = random.nextInt(maxValue)
    val randomFloat = randomValue.toFloat()
    val updatedDatapoint = Types.Datapoint.newBuilder().setFloat(randomFloat).build()

    try {
        updateProperty(vssPath, listOf(Types.Field.FIELD_VALUE), updatedDatapoint)
    } catch (e: Exception) {
        fail("Updating $vssPath to $randomFloat failed: $e")
    }

    return randomFloat
}

internal suspend fun DataBrokerApiInteraction.updateRandomUint32Value(vssPath: String, maxValue: Int = 300): Int {
    val random = Random(System.nanoTime())
    val randomValue = random.nextInt(maxValue)
    val updatedDatapoint = Types.Datapoint.newBuilder().setUint32(randomValue).build()

    try {
        updateProperty(vssPath, listOf(Types.Field.FIELD_VALUE), updatedDatapoint)
    } catch (e: Exception) {
        fail("Updating $vssPath to $randomValue failed: $e")
    }

    return randomValue
}
