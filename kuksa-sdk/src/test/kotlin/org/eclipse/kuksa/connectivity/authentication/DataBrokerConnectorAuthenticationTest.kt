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

import io.grpc.StatusRuntimeException
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.instanceOf
import org.eclipse.kuksa.connectivity.databroker.DataBrokerConnectorProvider
import org.eclipse.kuksa.connectivity.databroker.DataBrokerException
import org.eclipse.kuksa.connectivity.databroker.docker.DataBrokerDockerContainer
import org.eclipse.kuksa.connectivity.databroker.docker.SecureDataBrokerDockerContainer
import org.eclipse.kuksa.connectivity.databroker.request.FetchRequest
import org.eclipse.kuksa.connectivity.databroker.request.SubscribeRequest
import org.eclipse.kuksa.connectivity.databroker.request.UpdateRequest
import org.eclipse.kuksa.mocking.FriendlyVssPathListener
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.test.kotest.Authentication
import org.eclipse.kuksa.test.kotest.Integration
import org.eclipse.kuksa.test.kotest.Secure
import org.eclipse.kuksa.test.kotest.SecureDataBroker
import org.eclipse.kuksa.test.kotest.eventuallyConfiguration
import kotlin.random.Random
import kotlin.random.nextInt

// DataBroker must be started with Authentication enabled:
// databroker --jwt-public-key /certs/jwt/jwt.key.pub

// ./gradlew clean test -Dkotest.tags="Authentication"
class DataBrokerConnectorAuthenticationTest : BehaviorSpec({
    tags(Integration, Authentication, Secure, SecureDataBroker)

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

    val random = Random(System.nanoTime())

    given("A DataBrokerConnectorProvider") {
        val dataBrokerConnectorProvider = DataBrokerConnectorProvider()
        val speedVssPath = "Vehicle.Speed"

        and("a secure DataBrokerConnector with a READ_WRITE_ALL JWT") {
            val jwtFile = JwtType.READ_WRITE_ALL

            val dataBrokerConnector = dataBrokerConnectorProvider.createSecure(
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

        and("a secure DataBrokerConnector with a READ_ALL JWT") {
            val jwtFile = JwtType.READ_ALL
            val dataBrokerConnector = dataBrokerConnectorProvider.createSecure(
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

        and("a secure DataBrokerConnector with a READ_WRITE_ALL_VALUES_ONLY JWT") {
            val jwtFile = JwtType.READ_WRITE_ALL_VALUES_ONLY
            val dataBrokerConnector = dataBrokerConnectorProvider.createSecure(
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

        and("a secure DataBrokerConnector with no JWT") {
            val dataBrokerConnector = dataBrokerConnectorProvider.createSecure(
                jwtFileStream = null,
            )

            `when`("Trying to connect") {
                val result = runCatching {
                    dataBrokerConnector.connect()
                }

                then("The connection should be successful") {
                    result.getOrNull() shouldNotBe null
                }

                val connection = result.getOrNull()!!

                `when`("Reading the VALUE of Vehicle.Speed") {
                    val fetchRequest = FetchRequest(speedVssPath)
                    val fetchResult = runCatching {
                        connection.fetch(fetchRequest)
                    }

                    then("An error should occur") {
                        val exception = fetchResult.exceptionOrNull()
                        exception shouldNotBe null
                        exception shouldBe instanceOf(DataBrokerException::class)
                        exception!!.message shouldContain "UNAUTHENTICATED"
                    }
                }

                `when`("Writing the VALUE of Vehicle.Speed") {
                    val nextFloat = random.nextFloat() * 100F
                    val datapoint = Types.Datapoint.newBuilder().setFloat(nextFloat).build()
                    val updateRequest = UpdateRequest(speedVssPath, datapoint)

                    val updateResult = runCatching {
                        connection.update(updateRequest)
                    }

                    then("An error should occur") {
                        val exception = updateResult.exceptionOrNull()
                        exception shouldNotBe null
                        exception shouldBe instanceOf(DataBrokerException::class)
                        exception!!.message shouldContain "UNAUTHENTICATED"
                    }
                }

                `when`("Subscribing to the VALUE of Vehicle.Speed") {
                    val subscribeRequest = SubscribeRequest(speedVssPath)
                    val vssPathListener = FriendlyVssPathListener()

                    connection.subscribe(subscribeRequest, vssPathListener)

                    then("An error should occur") {
                        eventually(eventuallyConfiguration) {
                            vssPathListener.errors.size shouldBe 1

                            val exception = vssPathListener.errors.first()
                            exception shouldBe instanceOf(StatusRuntimeException::class)
                            exception.message shouldContain "UNAUTHENTICATED"
                        }
                    }
                }
            }
        }
    }
})
