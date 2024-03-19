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

data class VssDriver @JvmOverloads constructor(
    val attentiveProbability: VssAttentiveProbability = VssAttentiveProbability(),
    val distractionLevel: VssDistractionLevel = VssDistractionLevel(),
    val fatigueLevel: VssFatigueLevel = VssFatigueLevel(),
    val heartRate: VssHeartRate = VssHeartRate(),
    val identifier: VssIdentifier = VssIdentifier(),
    val isEyesOnRoad: VssIsEyesOnRoad = VssIsEyesOnRoad(),
    val isHandsOnWheel: VssIsHandsOnWheel = VssIsHandsOnWheel(),
) : VssNode {
    override val comment: String
        get() = ""

    override val description: String
        get() = "Driver data."

    override val type: String
        get() = "branch"

    override val uuid: String
        get() = "1cac57e7b7e756dc8a154eaacbce6426"

    override val vssPath: String
        get() = "Vehicle.Driver"

    override val children: Set<VssNode>
        get() = setOf(
            attentiveProbability,
            distractionLevel,
            fatigueLevel,
            heartRate,
            identifier,
            isEyesOnRoad,
            isHandsOnWheel,
        )

    override val parentClass: KClass<*>?
        get() = null

    data class VssHeartRate @JvmOverloads constructor(
        override val `value`: Int = 0,
    ) : VssSignal<Int> {
        override val dataType: KClass<*>
            get() = UInt::class

        override val comment: String
            get() = ""

        override val description: String
            get() = "Heart rate of the driver."

        override val type: String
            get() = "sensor"

        override val uuid: String
            get() = "d71516905f785c4da867a2f86e774d93"

        override val vssPath: String
            get() = "Vehicle.Driver.HeartRate"

        override val children: Set<VssNode>
            get() = setOf()

        override val parentClass: KClass<*>
            get() = VssVehicle::class
    }
}

data class VssAttentiveProbability @JvmOverloads constructor(
    override val `value`: Float = 0f,
) : VssSignal<Float> {
    override val comment: String
        get() = ""

    override val description: String
        get() = "Probability of attentiveness of the driver."

    override val type: String
        get() = "sensor"

    override val uuid: String
        get() = "fcd202467afb533fbbf9e7da89cc1cee"

    override val vssPath: String
        get() = "Vehicle.Driver.AttentiveProbability"

    override val children: Set<VssNode>
        get() = setOf()

    override val parentClass: KClass<*>
        get() = VssVehicle::class
}

data class VssDistractionLevel @JvmOverloads constructor(
    override val `value`: Float = 0f,
) : VssSignal<Float> {
    override val comment: String
        get() = ""

    override val description: String
        get() = "Distraction level of the driver"

    override val type: String
        get() = "sensor"

    override val uuid: String
        get() = "cb35ec0b924e58979e1469146d65c3fa"

    override val vssPath: String
        get() = "Vehicle.Driver.DistractionLevel"

    override val children: Set<VssNode>
        get() = setOf()

    override val parentClass: KClass<*>
        get() = VssVehicle::class
}

data class VssFatigueLevel @JvmOverloads constructor(
    override val `value`: Float = 0f,
) : VssSignal<Float> {
    override val comment: String
        get() = ""

    override val description: String
        get() = "Fatigue level of the driver"

    override val type: String
        get() = "sensor"

    override val uuid: String
        get() = "49b1626295705a79ae20d8a270c48b6b"

    override val vssPath: String
        get() = "Vehicle.Driver.FatigueLevel"

    override val children: Set<VssNode>
        get() = setOf()

    override val parentClass: KClass<*>
        get() = VssVehicle::class
}

data class VssIdentifier @JvmOverloads constructor(
    val issuer: VssIssuer = VssIssuer(),
    val subject: VssSubject = VssSubject(),
) : VssNode {
    override val comment: String
        get() = ""

    override val description: String
        get() = "Identifier attributes based on OAuth 2.0."

    override val type: String
        get() = "branch"

    override val uuid: String
        get() = "89705397069c5ec58d607318f2ff0ea8"

    override val vssPath: String
        get() = "Vehicle.Driver.Identifier"

    override val children: Set<VssNode>
        get() = setOf(issuer, subject)

    override val parentClass: KClass<*>
        get() = VssVehicle::class
}

data class VssIssuer @JvmOverloads constructor(
    override val `value`: String = "",
) : VssSignal<String> {
    override val comment: String
        get() = ""

    override val description: String
        get() =
            "Unique Issuer for the authentication of the occupant e.g. https://accounts.funcorp.com."

    override val type: String
        get() = "sensor"

    override val uuid: String
        get() = "ee7988d26d7156d2a030ecc506ea97e7"

    override val vssPath: String
        get() = "Vehicle.Driver.Identifier.Issuer"

    override val children: Set<VssNode>
        get() = setOf()

    override val parentClass: KClass<*>
        get() = VssIdentifier::class
}

data class VssSubject @JvmOverloads constructor(
    override val `value`: String = "",
) : VssSignal<String> {
    override val comment: String
        get() = ""

    override val description: String
        get() = "Subject for the authentication of the occupant e.g. UserID 7331677."

    override val type: String
        get() = "sensor"

    override val uuid: String
        get() = "b41ec688af265f10824bc9635989ac55"

    override val vssPath: String
        get() = "Vehicle.Driver.Identifier.Subject"

    override val children: Set<VssNode>
        get() = setOf()

    override val parentClass: KClass<*>
        get() = VssIdentifier::class
}

data class VssIsEyesOnRoad @JvmOverloads constructor(
    override val `value`: Boolean = false,
) : VssSignal<Boolean> {
    override val comment: String
        get() = ""

    override val description: String
        get() = "Has driver the eyes on road or not?"

    override val type: String
        get() = "sensor"

    override val uuid: String
        get() = "625e5009f1145aa0b797ee6c335ca2fe"

    override val vssPath: String
        get() = "Vehicle.Driver.IsEyesOnRoad"

    override val children: Set<VssNode>
        get() = setOf()

    override val parentClass: KClass<*>
        get() = VssVehicle::class
}

data class VssIsHandsOnWheel @JvmOverloads constructor(
    override val `value`: Boolean = false,
) : VssSignal<Boolean> {
    override val comment: String
        get() = ""

    override val description: String
        get() = "Are the driver's hands on the steering wheel or not?"

    override val type: String
        get() = "sensor"

    override val uuid: String
        get() = "90d7dc2c408c528d941829ff88075f24"

    override val vssPath: String
        get() = "Vehicle.Driver.IsHandsOnWheel"

    override val children: Set<VssNode>
        get() = setOf()

    override val parentClass: KClass<*>
        get() = VssVehicle::class
}
