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

package org.eclipse.kuksa.extension.vssProperty

import org.eclipse.kuksa.extension.copy
import org.eclipse.kuksa.vsscore.model.VssProperty

/**
 * Convenience operator for [copy] which updates the [VssProperty.value] by adding [value] to it.
 */
operator fun VssProperty<Long>.plusAssign(value: Number) {
    copy(this.value + value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssProperty.value] by adding [value] to it.
 */
operator fun VssProperty<Long>.plus(value: Number): VssProperty<Long> {
    return copy(this.value + value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssProperty.value] by subtracting [value] to it.
 */
operator fun VssProperty<Long>.minusAssign(value: Number) {
    copy(this.value - value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssProperty.value] by subtracting [value] to it.
 */
operator fun VssProperty<Long>.minus(value: Number): VssProperty<Long> {
    return copy(this.value - value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssProperty.value] by dividing [value] to it.
 */
operator fun VssProperty<Long>.divAssign(value: Number) {
    copy(this.value / value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssProperty.value] by dividing [value] to it.
 */
operator fun VssProperty<Long>.div(value: Number): VssProperty<Long> {
    return copy(this.value / value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssProperty.value] by multiplying [value] to it.
 */
operator fun VssProperty<Long>.timesAssign(value: Number) {
    copy(this.value * value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssProperty.value] by multiplying [value] to it.
 */
operator fun VssProperty<Long>.times(value: Number): VssProperty<Long> {
    return copy(this.value * value.toLong())
}
