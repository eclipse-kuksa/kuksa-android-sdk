/*
 * Copyright (c) 2023 - 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.connectivity.databroker

import io.grpc.StatusException
import io.grpc.stub.StreamObserver
import io.kotest.assertions.fail
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.instanceOf
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import org.eclipse.kuksa.connectivity.databroker.docker.DataBrokerDockerContainer
import org.eclipse.kuksa.connectivity.databroker.docker.InsecureDataBrokerDockerContainer
import org.eclipse.kuksa.connectivity.databroker.v2.request.ActuateRequestV2
import org.eclipse.kuksa.connectivity.databroker.v2.request.FetchValueRequestV2
import org.eclipse.kuksa.connectivity.databroker.v2.request.FetchValuesRequestV2
import org.eclipse.kuksa.connectivity.databroker.v2.request.PublishValueRequestV2
import org.eclipse.kuksa.connectivity.databroker.v2.request.SubscribeRequestV2
import org.eclipse.kuksa.extensions.toSignalId
import org.eclipse.kuksa.extensions.updateRandomFloatValue
import org.eclipse.kuksa.proto.v2.KuksaValV2.OpenProviderStreamRequest
import org.eclipse.kuksa.proto.v2.KuksaValV2.OpenProviderStreamResponse
import org.eclipse.kuksa.proto.v2.KuksaValV2.ProvideActuationRequest
import org.eclipse.kuksa.proto.v2.Types
import org.eclipse.kuksa.proto.v2.Types.SignalID
import org.eclipse.kuksa.test.kotest.Insecure
import org.eclipse.kuksa.test.kotest.InsecureDataBroker
import org.eclipse.kuksa.test.kotest.Integration
import org.junit.jupiter.api.Assertions
import kotlin.random.Random

class DataBrokerConnectionV2Test : BehaviorSpec({
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

    given("A successfully established connection to the DataBroker") {
        val dataBrokerConnectorProvider = DataBrokerConnectorProvider()
        val connector = dataBrokerConnectorProvider.createInsecure(
            port = databrokerContainer!!.port,
        )
        val dataBrokerConnection = connector.connect()

        `when`("trying to fetch multiple values") {
            val signalIds = listOf(
                "Vehicle.Acceleration.Longitudinal".toSignalId(),
                "Vehicle.Acceleration.Vertical".toSignalId(),
            )
            val randomFloat1 = createRandomFloatDatapoint()
            val randomFloat2 = createRandomFloatDatapoint()

            dataBrokerConnection.kuksaValV2.publishValue(PublishValueRequestV2(signalIds[0], randomFloat1))
            dataBrokerConnection.kuksaValV2.publishValue(PublishValueRequestV2(signalIds[1], randomFloat2))

            val request = FetchValuesRequestV2(signalIds)
            val valuesResponse = dataBrokerConnection.kuksaValV2.fetchValues(request)

            then("the values should contain the correct information") {
                valuesResponse.dataPointsCount shouldBe 2
                val dataPoint1 = valuesResponse.getDataPoints(0)
                dataPoint1.value.float shouldBe randomFloat1.value.float
                val dataPoint2 = valuesResponse.getDataPoints(1)
                dataPoint2.value.float shouldBe randomFloat2.value.float
            }
        }

        `when`("no ActuationProvider exists for Vehicle.Cabin.Seat.Row1.DriverSide.Heating") {
            `when`("trying to actuate Vehicle.Cabin.Seat.Row1.DriverSide.Heating") {
                val signalId = "Vehicle.Cabin.Seat.Row1.DriverSide.Heating".toSignalId()
                val value = Types.Value.newBuilder().setInt32(50).build()

                val request = ActuateRequestV2(signalId, value)
                val result = runCatching {
                    dataBrokerConnection.kuksaValV2.actuate(request)
                }
                then("An Exception should be thrown") {
                    result.isFailure shouldBe true
                    val exception = result.exceptionOrNull()!!
                    exception shouldBe instanceOf(DataBrokerException::class)
                    exception.message shouldContain "UNAVAILABLE"
                }
            }
        }

        `when`("an ActuationProvider exists for Vehicle.Cabin.Seat.Row1.DriverSide.Heating") {
            val responseStream = object : StreamObserver<OpenProviderStreamResponse> {
                override fun onNext(value: OpenProviderStreamResponse) {
                    // unimplemented
                }

                override fun onError(t: Throwable) {
                    // unimplemented
                }

                override fun onCompleted() {
                    // unimplemented
                }
            }
            val requestStream = dataBrokerConnection.kuksaValV2.openProviderStream(responseStream)

            val signalId = "Vehicle.Cabin.Seat.Row1.DriverSide.Heating".toSignalId()

            val provideActuationRequest = ProvideActuationRequest.newBuilder()
                .addActuatorIdentifiers(signalId)
                .build()
            val openProviderStreamRequest = OpenProviderStreamRequest.newBuilder()
                .setProvideActuationRequest(provideActuationRequest)
                .build()
            requestStream.onNext(openProviderStreamRequest)

            `when`("trying to actuate Vehicle.Cabin.Seat.Row1.DriverSide.Heating") {
                val value = Types.Value.newBuilder().setInt32(50).build()

                val request = ActuateRequestV2(signalId, value)
                val result = runCatching {
                    dataBrokerConnection.kuksaValV2.actuate(request)
                }
                then("No Exception should be thrown") {
                    result.isSuccess shouldBe true
                    result.exceptionOrNull() shouldBe null
                }
            }
        }

        and("a SubscribeRequest with a valid VSS Path") {
            val vssPath = "Vehicle.Acceleration.Lateral"
            val signalId = SignalID.newBuilder().setPath(vssPath).build()

            val initialValue = dataBrokerConnection.updateRandomFloatValue(vssPath)

            val subscribeRequest = SubscribeRequestV2(listOf(vssPath))
            `when`("Subscribing to the VSS path") {
                val responseFlow = dataBrokerConnection.kuksaValV2.subscribe(subscribeRequest)

                then("An initial update is sent") {
                    val subscribeResponse = responseFlow.first()
                    subscribeResponse shouldNotBe null
                    subscribeResponse.entriesCount shouldBe 1
                    subscribeResponse.entriesMap[vssPath] shouldNotBe null
                    subscribeResponse.entriesMap[vssPath]?.value?.float shouldBe initialValue
                }

                `when`("The observed VSS path changes") {
                    val randomFloatDatapoint = createRandomFloatDatapoint()
                    dataBrokerConnection.kuksaValV2.publishValue(PublishValueRequestV2(signalId, randomFloatDatapoint))

                    then("The #onEntryChanged callback is triggered with the new value") {
                        val subscribeResponse = responseFlow.first()
                        subscribeResponse shouldNotBe null
                        subscribeResponse.entriesCount shouldBe 1
                        subscribeResponse.entriesMap[vssPath] shouldNotBe null
                        subscribeResponse.entriesMap[vssPath]?.value?.float shouldBe randomFloatDatapoint.value.float
                    }
                }
            }

            val validDatapoint = createRandomFloatDatapoint()

            `when`("Updating the DataBroker property (VSS path) with a valid Datapoint") {
                // make sure that the value is set and known to us
                dataBrokerConnection.kuksaValV2.publishValue(PublishValueRequestV2(signalId, validDatapoint))

                and("When fetching it afterwards") {
                    val fetchRequest = FetchValueRequestV2(signalId)
                    val response = dataBrokerConnection.kuksaValV2.fetchValue(fetchRequest)

                    then("The response contains the correctly set value") {
                        val dataPoint = response.dataPoint
                        val capturedValue = dataPoint.value.float
                        Assertions.assertEquals(
                            validDatapoint.value.float,
                            capturedValue,
                            0.0001F,
                        )
                    }
                }
            }

            `when`("publishing a Datapoint of a wrong/different type") {
                val datapoint = createRandomIntDatapoint()
                val updateRequest = PublishValueRequestV2(signalId, datapoint)
                val result = runCatching {
                    dataBrokerConnection.kuksaValV2.publishValue(updateRequest)
                }

                then("It should throw an Exception containing the Error INVALID_ARGUMENT") {
                    result.isFailure shouldBe true
                    val exception = result.exceptionOrNull()!!
                    exception shouldBe instanceOf(DataBrokerException::class)
                    exception.message shouldContain "INVALID_ARGUMENT"
                }

                and("Fetching it afterwards") {
                    val fetchRequest = FetchValueRequestV2(signalId)
                    val getResponse = dataBrokerConnection.kuksaValV2.fetchValue(fetchRequest)

                    then("The response still contains the previously set value") {
                        val dataPoint = getResponse.dataPoint
                        Assertions.assertEquals(
                            validDatapoint.value.float,
                            dataPoint.value.float,
                            0.0001F,
                        )
                    }
                }
            }
        }

        and("A SubscribeRequest with an invalid VSS Path") {
            val invalidVssPath = "Vehicle.Some.Unknown.Path"
            val invalidSignalId = SignalID.newBuilder().setPath(invalidVssPath).build()

            `when`("Trying to subscribe to the INVALID VSS path") {
                val subscribeRequest = SubscribeRequestV2(listOf(invalidVssPath))

                val result = runCatching {
                    dataBrokerConnection.kuksaValV2.subscribe(subscribeRequest).first()
                }

                then("A DataBrokerException with error message 'NOT_FOUND' is thrown") {
                    result.isFailure shouldBe true
                    val exception = result.exceptionOrNull()!!
                    exception shouldBe instanceOf(StatusException::class)
                    exception.message shouldContain "NOT_FOUND"
                }
            }

            `when`("Trying to update the INVALID VSS Path") {
                val datapoint = createRandomFloatDatapoint()
                val updateRequest = PublishValueRequestV2(invalidSignalId, datapoint)

                val result = kotlin.runCatching {
                    dataBrokerConnection.kuksaValV2.publishValue(updateRequest)
                }

                then("It should fail with an errorCode 404 (path not found)") {
                    result.onSuccess { fail("Should not succeed") }
                }
            }

            `when`("Trying to fetch the INVALID VSS path") {
                val fetchRequest = FetchValueRequestV2(invalidSignalId)

                val result = runCatching {
                    dataBrokerConnection.kuksaValV2.fetchValue(fetchRequest)
                }

                then("A DataBrokerException with error message 'NOT_FOUND' is thrown") {
                    result.isFailure shouldBe true
                    val exception = result.exceptionOrNull()!!
                    exception shouldBe instanceOf(DataBrokerException::class)
                    exception.message shouldContain "NOT_FOUND"
                }
            }
        }

        and("A SubscribeRequest with a negative bufferSize") {
            val vssPath = "Vehicle.Speed"
            val signalPaths = listOf(vssPath)
            val subscribeRequest = SubscribeRequestV2(signalPaths, -128)

            val result = runCatching {
                dataBrokerConnection.kuksaValV2.subscribe(subscribeRequest)
            }

            then("It should throw a DataBrokerException") {
                result.onFailure { e ->
                    e shouldBe instanceOf(DataBrokerException::class)
                    e.message shouldContain "INVALID_ARGUMENT"
                }.onSuccess {
                    fail("Using a negative bufferSize should not succeed.")
                }
            }
        }

        // this test closes the connection, the connection can't be used afterward anymore
        `when`("A DisconnectListener is registered successfully") {
            val disconnectListener = mockk<DisconnectListener>(relaxed = true)
            val disconnectListeners = dataBrokerConnection.disconnectListeners
            disconnectListeners.register(disconnectListener)

            `when`("The Connection is closed manually") {
                dataBrokerConnection.disconnect()

                then("The DisconnectListener is triggered") {
                    verify { disconnectListener.onDisconnect() }
                }
            }
        }
        // connection is closed at this point
    }
})

private val random = Random(System.currentTimeMillis())
private fun createRandomFloatDatapoint(): Types.Datapoint {
    val newValue = random.nextFloat()
    val value = Types.Value.newBuilder().setFloat(newValue).build()
    return Types.Datapoint.newBuilder().setValue(value).build()
}

private fun createRandomIntDatapoint(): Types.Datapoint {
    val newValue = random.nextInt()
    val value = Types.Value.newBuilder().setInt32(newValue).build()
    return Types.Datapoint.newBuilder().setValue(value).build()
}
