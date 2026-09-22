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

package org.eclipse.kuksa.vssNode

import org.eclipse.kuksa.vsscore.model.VssNode
import org.eclipse.kuksa.vsscore.model.VssSignal
import kotlin.reflect.KClass

data class VssVehicle(
    val driver: VssDriver = VssDriver(),
    val passenger: VssPassenger = VssPassenger(),
    val body: VssBody = VssBody(),
    override val uuid: String = "Vehicle",
    override val vssPath: String = "Vehicle",
    override val description: String = "High-level vehicle data.",
    override val type: String = "branch",
    override val comment: String = "",
) : VssNode {
    override val children: Set<VssNode>
        get() = setOf(driver, passenger, body)
}

data class VssBody(
    override val uuid: String = "Body",
    override val vssPath: String = "Vehicle.Body",
    override val description: String = "All body components.",
    override val type: String = "branch",
    override val comment: String = "",
) : VssNode {
    override val parentClass: KClass<*>
        get() = VssVehicle::class
}

data class VssPassenger(
    val heartRate: VssHeartRate = VssHeartRate(),
    override val uuid: String = "Passenger",
    override val vssPath: String = "Vehicle.Passenger",
    override val description: String = "Passenger data",
    override val type: String = "branch",
    override val comment: String = "",
) : VssNode {
    override val children: Set<VssNode>
        get() = setOf(heartRate)
    override val parentClass: KClass<*>
        get() = VssVehicle::class

    data class VssHeartRate(
        override val uuid: String = "Passenger HeartRate",
        override val vssPath: String = "Vehicle.Passenger.HeartRate",
        override val description: String = "Heart rate of the Passenger.",
        override val type: String = "sensor",
        override val comment: String = "",
        override val value: Int = 80,
    ) : VssSignal<Int> {
        override val dataType: KClass<*>
            get() = UInt::class

        override val parentClass: KClass<*>
            get() = VssPassenger::class
    }
}

data class VssValueInt(
    override val uuid: String = "Value",
    override val vssPath: String = "Value",
    override val description: String = "",
    override val type: String = "sensor",
    override val comment: String = "",
    override val value: Int = 0,
) : VssSignal<Int>

data class VssValueFloat(
    override val uuid: String = "Value",
    override val vssPath: String = "Value",
    override val description: String = "",
    override val type: String = "sensor",
    override val comment: String = "",
    override val value: Float = 0f,
) : VssSignal<Float>

data class VssValueDouble(
    override val uuid: String = "Value",
    override val vssPath: String = "Value",
    override val description: String = "",
    override val type: String = "sensor",
    override val comment: String = "",
    override val value: Double = 0.0,
) : VssSignal<Double>

data class VssValueLong(
    override val uuid: String = "Value",
    override val vssPath: String = "Value",
    override val description: String = "",
    override val type: String = "sensor",
    override val comment: String = "",
    override val value: Long = 0L,
) : VssSignal<Long>

data class VssInvalid(
    override val uuid: String = "Invalid",
    override val vssPath: String = "Invalid",
    override val description: String = "",
    override val type: String = "",
    override val comment: String = "",
    override val value: Exception = Exception(),
) : VssSignal<Exception>
