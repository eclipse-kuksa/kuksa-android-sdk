/*
 *
 *  * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *
 *
 */

package org.eclipse.kuksa.vssSpecification

import org.eclipse.kuksa.vsscore.model.VssProperty
import org.eclipse.kuksa.vsscore.model.VssSpecification
import kotlin.reflect.KClass

data class VssDriver(
    val heartRate: VssHeartRate = VssHeartRate(),
    override val uuid: String = "Driver",
    override val vssPath: String = "Vehicle.Driver",
    override val description: String = "Driver data.",
    override val type: String = "branch",
    override val comment: String = "",
) : VssSpecification {
    override val children: Set<VssSpecification>
        get() = setOf(heartRate)
}

data class VssHeartRate(
    override val uuid: String = "HeartRate",
    override val vssPath: String = "Vehicle.Driver.HeartRate",
    override val description: String = "Heart rate of the driver.",
    override val type: String = "sensor",
    override val comment: String = "",
    override val value: Int = 100,
) : VssProperty<Int> {
    override val parentClass: KClass<*>?
        get() = VssDriver::class
}
