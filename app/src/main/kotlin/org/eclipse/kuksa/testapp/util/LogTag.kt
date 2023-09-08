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

package org.eclipse.kuksa.testapp.util

import kotlin.reflect.KClass

/**
 * Utility class used to provide LogTags for Android Logger.
 */
internal object LogTag {
    private const val MAX_TAG_LENGTH = 23

    /**
     * Provides a LogTag for a java class type.
     */
    fun of(clazz: Class<*>) = clazz.simpleName.take(MAX_TAG_LENGTH)

    /**
     * Provides a LogTag for a kotlin class type.
     */
    fun of(clazz: KClass<*>) = of(clazz.java)
}
