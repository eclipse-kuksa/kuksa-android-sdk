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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldNotBe
import org.eclipse.kuksa.connectivity.databroker.docker.DataBrokerDockerContainer
import org.eclipse.kuksa.connectivity.databroker.docker.InsecureDataBrokerDockerContainer
import org.eclipse.kuksa.test.kotest.Insecure
import org.eclipse.kuksa.test.kotest.InsecureDataBroker
import org.eclipse.kuksa.test.kotest.Integration

class DataBrokerConnectorTest : BehaviorSpec({
    tags(Integration, Insecure, InsecureDataBroker)

    var databrokerContainer: DataBrokerDockerContainer? = null
    beforeSpec {
        databrokerContainer = InsecureDataBrokerDockerContainer()
            .apply {
                start()
            }
    }

    afterSpec {
        databrokerContainer?.stop()
    }

    given("A DataBrokerConnectorProvider") {
        val dataBrokerConnectorProvider = DataBrokerConnectorProvider()

        and("an insecure DataBrokerConnector with valid Host and Port") {
            val dataBrokerConnector = dataBrokerConnectorProvider.createInsecure()

            `when`("Trying to establish an insecure connection") {
                val connection = dataBrokerConnector.connect()

                then("It should return a valid connection") {
                    connection shouldNotBe null
                }
            }
        }

        and("a DataBrokerConnector with INVALID Host and Port") {
            val invalidHost = "0.0.0.0"
            val invalidPort = 12345
            val dataBrokerConnector = dataBrokerConnectorProvider.createInsecure(invalidHost, invalidPort)

            `when`("Trying to establish a connection") {
                val exception = shouldThrow<DataBrokerException> {
                    dataBrokerConnector.connect()
                }

                then("It should throw a DataBrokerException") {
                    exception shouldNotBe null
                }
            }
        }
    }
})
