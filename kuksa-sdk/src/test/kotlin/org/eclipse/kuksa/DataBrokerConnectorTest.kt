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

import io.kotest.core.spec.style.BehaviorSpec
import org.eclipse.kuksa.test.databroker.DataBrokerConnectorProvider
import org.eclipse.kuksa.test.kotest.Insecure
import org.eclipse.kuksa.test.kotest.Integration
import org.junit.jupiter.api.Assertions

class DataBrokerConnectorTest : BehaviorSpec({
    tags(Integration, Insecure)

    given("A DataBrokerConnectorProvider") {
        val dataBrokerConnectorProvider = DataBrokerConnectorProvider()

        and("an insecure DataBrokerConnector with valid Host and Port") {
            val dataBrokerConnector = dataBrokerConnectorProvider.createInsecure()

            `when`("Trying to establish an insecure connection") {
                val connection = dataBrokerConnector.connect()

                then("It should return a valid connection") {
                    Assertions.assertNotNull(connection)
                }
            }
        }

        and("a DataBrokerConnector with INVALID Host and Port") {
            val invalidHost = "0.0.0.0"
            val invalidPort = 12345
            val dataBrokerConnector = dataBrokerConnectorProvider.createInsecure(invalidHost, invalidPort)

            `when`("Trying to establish a connection") {
                var isExceptionCaught = false
                try {
                    dataBrokerConnector.connect()
                } catch (ignored: DataBrokerException) {
                    isExceptionCaught = true
                }

                then("It should throw an exception") {
                    Assertions.assertTrue(isExceptionCaught)
                }
            }
        }
    }
})
