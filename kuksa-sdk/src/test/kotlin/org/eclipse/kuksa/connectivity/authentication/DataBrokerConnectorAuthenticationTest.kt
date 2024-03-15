/*
 * Copyright (c) 2023 - 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.connectivity.authentication

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.eclipse.kuksa.connectivity.databroker.DataBrokerConnectorProvider
import org.eclipse.kuksa.connectivity.databroker.docker.DockerDatabrokerContainer
import org.eclipse.kuksa.connectivity.databroker.docker.DockerSecureDatabrokerContainer
import org.eclipse.kuksa.connectivity.databroker.request.FetchRequest
import org.eclipse.kuksa.connectivity.databroker.request.UpdateRequest
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.test.TestResourceFile
import org.eclipse.kuksa.test.kotest.Authentication
import org.eclipse.kuksa.test.kotest.CustomDatabroker
import org.eclipse.kuksa.test.kotest.Insecure
import org.eclipse.kuksa.test.kotest.Integration
import kotlin.random.Random
import kotlin.random.nextInt

// DataBroker must be started with Authentication enabled:
// databroker --jwt-public-key /certs/jwt/jwt.key.pub

// ./gradlew clean test -Dkotest.tags="Authentication"
class DataBrokerConnectorAuthenticationTest : BehaviorSpec({
    tags(Integration, Authentication, Insecure, CustomDatabroker)

    var databrokerContainer: DockerDatabrokerContainer? = null
    beforeSpec {
        databrokerContainer = DockerSecureDatabrokerContainer()
            .apply {
                start()
            }
    }

    afterSpec {
        databrokerContainer?.stop()
    }

    val random = Random(System.nanoTime())
    val tlsCertificate = TestResourceFile("tls/CA.pem")

    given("A DataBrokerConnectorProvider") {
        val dataBrokerConnectorProvider = DataBrokerConnectorProvider()
        val speedVssPath = "Vehicle.Speed"

        and("an insecure DataBrokerConnector with a READ_WRITE_ALL JWT") {
            val jwtFile = JwtType.READ_WRITE_ALL

            val dataBrokerConnector = dataBrokerConnectorProvider.createSecure(
                rootCertFileStream = tlsCertificate.inputStream(),
                jwtFileStream = jwtFile.asInputStream(),
            )

            and("a successfully established connection") {
                val connection = dataBrokerConnector.connect()

                `when`("Reading Vehicle.Speed") {
                    val fetchRequest = FetchRequest(speedVssPath)
                    val response = connection.fetch(fetchRequest)

                    then("No error should occur") {
                        response.errorsList.size shouldBe 0
                    }
                }

                `when`("Writing the VALUE of Vehicle.Speed") {
                    val nextFloat = random.nextFloat() * 100F
                    val datapoint = Types.Datapoint.newBuilder().setFloat(nextFloat).build()
                    val updateRequest = UpdateRequest(speedVssPath, datapoint)

                    val response = connection.update(updateRequest)

                    then("No error should occur") {
                        response.errorsList.size shouldBe 0
                    }
                }
            }
        }

        and("an insecure DataBrokerConnector with a READ_ALL JWT") {
            val jwtFile = JwtType.READ_ALL
            val dataBrokerConnector = dataBrokerConnectorProvider.createSecure(
                rootCertFileStream = tlsCertificate.inputStream(),
                jwtFileStream = jwtFile.asInputStream(),
            )

            and("a successfully established connection") {
                val connection = dataBrokerConnector.connect()

                `when`("Reading Vehicle.Speed") {
                    val fetchRequest = FetchRequest(speedVssPath)
                    val response = connection.fetch(fetchRequest)

                    then("No error should appear") {
                        response.errorsList.size shouldBe 0
                    }
                }

                `when`("Writing the VALUE of Vehicle.Speed") {
                    val nextFloat = random.nextFloat() * 100F
                    val datapoint = Types.Datapoint.newBuilder().setFloat(nextFloat).build()
                    val updateRequest = UpdateRequest(speedVssPath, datapoint)
                    val response = connection.update(updateRequest)

                    then("An error should occur") {
                        response.errorsList.size shouldBe 1
                    }
                }
            }
        }

        and("an insecure DataBrokerConnector with a READ_WRITE_ALL_VALUES_ONLY JWT") {
            val jwtFile = JwtType.READ_WRITE_ALL_VALUES_ONLY
            val dataBrokerConnector = dataBrokerConnectorProvider.createSecure(
                rootCertFileStream = tlsCertificate.inputStream(),
                jwtFileStream = jwtFile.asInputStream(),
            )

            and("a successfully established connection") {
                val connection = dataBrokerConnector.connect()
                val panVssPath = "Vehicle.Body.Mirrors.DriverSide.Pan"
                val actuatorTargetField = Types.Field.FIELD_ACTUATOR_TARGET

                `when`("Reading the ACTUATOR_TARGET of Vehicle.Body.Mirrors.DriverSide.Pan") {
                    val fetchRequest = FetchRequest(panVssPath, actuatorTargetField)
                    val response = connection.fetch(fetchRequest)

                    then("No error should occur") {
                        response.errorsList.size shouldBe 0
                    }
                }

                `when`("Writing to the ACTUATOR_TARGET of Vehicle.Body.Mirrors.DriverSide.Pan") {
                    val nextInt = random.nextInt(-100..100)
                    val datapoint = Types.Datapoint.newBuilder().setInt32(nextInt).build()
                    val updateRequest = UpdateRequest(panVssPath, datapoint, actuatorTargetField)

                    val response = connection.update(updateRequest)

                    then("An error should occur") {
                        response.errorsList.size shouldBe 1
                    }
                }

                `when`("Reading the VALUE of Vehicle.Speed") {
                    val fetchRequest = FetchRequest(speedVssPath)
                    val response = connection.fetch(fetchRequest)

                    then("No error should occur") {
                        response.errorsList.size shouldBe 0
                    }
                }

                `when`("Writing the VALUE of Vehicle.Speed") {
                    val nextFloat = random.nextFloat() * 100F
                    val datapoint = Types.Datapoint.newBuilder().setFloat(nextFloat).build()
                    val updateRequest = UpdateRequest(speedVssPath, datapoint)

                    val response = connection.update(updateRequest)

                    then("No error should occur") {
                        response.errorsList.size shouldBe 0
                    }
                }
            }
        }
    }
})

// The tokens provided here might need to be updated irregularly
// see: https://github.com/eclipse/kuksa.val/tree/master/jwt
// The tokens only work when the Databroker is started using the correct public key: jwt.key.pub
enum class JwtType(private val fileName: String) {
    READ_WRITE_ALL("actuate-provide-all.token"), // ACTUATOR_TARGET and VALUE
    READ_WRITE_ALL_VALUES_ONLY("provide-all.token"), // VALUE
    READ_ALL("read-all.token"),
    ;

    fun asInputStream(): InputStream {
        val resourceFile = TestResourceFile(fileName)
        return resourceFile.inputStream()
    }
}
