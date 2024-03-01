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

package org.eclipse.kuksa.extension.vss

import org.eclipse.kuksa.vsscore.model.VssLeaf

/**
 * Convenience operator for [copy] which updates the [VssLeaf.value] by adding [value] to it.
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
operator fun VssLeaf<Long>.plusAssign(value: Number) {
    copy(this.value + value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssLeaf.value] by adding [value] to it.
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
operator fun VssLeaf<Long>.plus(value: Number): VssLeaf<Long> {
    return copy(this.value + value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssLeaf.value] by subtracting [value] to it.
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
operator fun VssLeaf<Long>.minusAssign(value: Number) {
    copy(this.value - value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssLeaf.value] by subtracting [value] to it.
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
operator fun VssLeaf<Long>.minus(value: Number): VssLeaf<Long> {
    return copy(this.value - value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssLeaf.value] by dividing [value] to it.
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 * @throws [ArithmeticException] if divided by zero.
 */
operator fun VssLeaf<Long>.divAssign(value: Number) {
    copy(this.value / value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssLeaf.value] by dividing [value] to it.
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 * @throws [ArithmeticException] if divided by zero.
 */
operator fun VssLeaf<Long>.div(value: Number): VssLeaf<Long> {
    return copy(this.value / value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssLeaf.value] by multiplying [value] to it.
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
operator fun VssLeaf<Long>.timesAssign(value: Number) {
    copy(this.value * value.toLong())
}

/**
 * Convenience operator for [copy] which updates the [VssLeaf.value] by multiplying [value] to it.
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
operator fun VssLeaf<Long>.times(value: Number): VssLeaf<Long> {
    return copy(this.value * value.toLong())
}
