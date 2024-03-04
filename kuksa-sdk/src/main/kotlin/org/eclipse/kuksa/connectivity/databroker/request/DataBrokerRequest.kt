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

package org.eclipse.kuksa.connectivity.databroker.request

import org.eclipse.kuksa.proto.v1.Types.Field
import org.eclipse.kuksa.proto.v1.Types.Field.FIELD_VALUE

/**
 * Consists of request information for the [org.eclipse.kuksa.connectivity.databroker.DataBrokerConnection].
 */
interface DataBrokerRequest {
    /**
     * The VehicleSignalSpecification (VSS) path.
     */
    val vssPath: String

    /**
     * The corresponding field type(s) of the [vssPath] request. The [fields] can be used to subscribe to different
     * information. The default is [FIELD_VALUE].
     */
    val fields: Array<out Field>
}
