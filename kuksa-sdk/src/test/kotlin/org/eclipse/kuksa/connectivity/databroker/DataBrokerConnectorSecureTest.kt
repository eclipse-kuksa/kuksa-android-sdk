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

package org.eclipse.kuksa.connectivity.databroker

import io.kotest.core.spec.style.BehaviorSpec
import org.eclipse.kuksa.connectivity.databroker.docker.DataBrokerDockerContainer
import org.eclipse.kuksa.connectivity.databroker.docker.SecureDataBrokerDockerContainer
import org.eclipse.kuksa.test.TestResourceFile
import org.eclipse.kuksa.test.kotest.Integration
import org.eclipse.kuksa.test.kotest.Secure
import org.eclipse.kuksa.test.kotest.SecureDataBroker
import org.eclipse.kuksa.test.kotest.Tls
import org.junit.jupiter.api.Assertions

// run command: ./gradlew clean test -Dkotest.tags="Secure"
class DataBrokerConnectorSecureTest : BehaviorSpec({
    tags(Integration, Secure, Tls, SecureDataBroker)

    var databrokerContainer: DataBrokerDockerContainer? = null
    beforeSpec {
        databrokerContainer = SecureDataBrokerDockerContainer()
            .apply {
                start()
            }
    }

    afterSpec {
        databrokerContainer?.stop()
    }

    given("A DataBrokerConnectorProvider") {
        val dataBrokerConnectorProvider = DataBrokerConnectorProvider()

        and("a secure DataBrokerConnector with valid Host, Port and TLS certificate") {
            val tlsCertificate = TestResourceFile("tls/CA.pem")

            val dataBrokerConnector = dataBrokerConnectorProvider.createSecure(
                rootCertFileStream = tlsCertificate.inputStream(),
            )

            `when`("Trying to establish a secure connection") {
                val connection = dataBrokerConnector.connect()

                then("It should return a valid connection") {
                    Assertions.assertNotNull(connection)
                }
            }
        }
    }
})
