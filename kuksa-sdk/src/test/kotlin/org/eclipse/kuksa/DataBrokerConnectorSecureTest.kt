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
import org.eclipse.kuksa.test.kotest.Integration
import org.eclipse.kuksa.test.kotest.Secure
import org.junit.jupiter.api.Assertions

class DataBrokerConnectorSecureTest : BehaviorSpec({
    tags(Integration, Secure)

    given("A DataBrokerConnectorProvider") {
        val dataBrokerConnectorProvider = DataBrokerConnectorProvider()

        and("a secure DataBrokerConnector with valid Host, Port and TLS certificate") {
            val certificate = DataBrokerConnectionTest::class.java.classLoader?.getResourceAsStream("CA.pem")!!
            val dataBrokerConnector = dataBrokerConnectorProvider.createSecure(rootCertFileStream = certificate)

            `when`("Trying to establish a secure connection") {
                val connection = dataBrokerConnector.connect()

                then("It should return a valid connection") {
                    Assertions.assertNotNull(connection)
                }
            }
        }
    }
})
