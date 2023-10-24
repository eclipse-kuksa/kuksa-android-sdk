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

package org.eclipse.kuksa

import org.eclipse.kuksa.pattern.listener.Listener
import org.eclipse.kuksa.proto.v1.Types.DataEntry
import org.eclipse.kuksa.proto.v1.Types.Field
import org.eclipse.kuksa.vsscore.model.VssSpecification

/**
 * The Listener is used to notify about changes to subscribed properties.
 */
interface PropertyListener : Listener {
    /**
     * Will be triggered with the [updatedValue] when the underlying [field] of the [vssPath] changed it's value.
     */
    fun onPropertyChanged(vssPath: String, field: Field, updatedValue: DataEntry)

    /**
     * Will be triggered when an error happens during subscription and forwards the [throwable].
     */
    fun onError(throwable: Throwable)
}

/**
 * The Listener is used to notify about subscribed [VssSpecification]. If a [VssSpecification] has children
 * then [onSpecificationChanged] will be called on every value change for every children.
 */
interface VssSpecificationListener<T : VssSpecification> {
    /**
     * Will be triggered with the [vssSpecification] when the underlying vssPath changed it's value or to inform about
     * the initial state.
     */
    fun onSpecificationChanged(vssSpecification: T)

    /**
     * Will be triggered when an error happens during subscription and forwards the [throwable].
     */
    fun onError(throwable: Throwable)
}
