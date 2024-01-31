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

package org.eclipse.kuksa.testapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import org.eclipse.kuksa.CoroutineCallback
import org.eclipse.kuksa.DataBrokerConnection
import org.eclipse.kuksa.DisconnectListener
import org.eclipse.kuksa.PropertyListener
import org.eclipse.kuksa.VssSpecificationListener
import org.eclipse.kuksa.extension.entriesMetadata
import org.eclipse.kuksa.extension.valueType
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.Types.Field
import org.eclipse.kuksa.testapp.databroker.DataBrokerEngine
import org.eclipse.kuksa.testapp.databroker.JavaDataBrokerEngine
import org.eclipse.kuksa.testapp.databroker.KotlinDataBrokerEngine
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo
import org.eclipse.kuksa.testapp.databroker.view.DataBrokerView
import org.eclipse.kuksa.testapp.databroker.viewmodel.ConnectionViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.ConnectionViewModel.ConnectionViewState
import org.eclipse.kuksa.testapp.databroker.viewmodel.OutputEntry
import org.eclipse.kuksa.testapp.databroker.viewmodel.OutputViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.TopAppBarViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.VSSPropertiesViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.VssSpecificationsViewModel
import org.eclipse.kuksa.testapp.extension.TAG
import org.eclipse.kuksa.testapp.preferences.ConnectionInfoRepository
import org.eclipse.kuksa.testapp.ui.theme.KuksaAppAndroidTheme
import org.eclipse.kuksa.vsscore.annotation.VssDefinition
import org.eclipse.kuksa.vsscore.model.VssSpecification

@VssDefinition("vss_rel_4.0.yaml")
class KuksaDataBrokerActivity : ComponentActivity() {
    private lateinit var connectionInfoRepository: ConnectionInfoRepository

    private val topAppBarViewModel: TopAppBarViewModel by viewModels()
    private val connectionViewModel: ConnectionViewModel by viewModels {
        ConnectionViewModel.Factory(connectionInfoRepository)
    }
    private val vssPropertiesViewModel: VSSPropertiesViewModel by viewModels()
    private val vssSpecificationsViewModel: VssSpecificationsViewModel by viewModels()
    private val outputViewModel: OutputViewModel by viewModels()

    private val dataBrokerConnectionCallback = object : CoroutineCallback<DataBrokerConnection>() {
        override fun onSuccess(result: DataBrokerConnection?) {
            outputViewModel.addOutputEntry("Connection to DataBroker successful established")
            connectionViewModel.updateConnectionState(ConnectionViewState.CONNECTED)

            loadVssPathSuggestions()
        }

        override fun onError(error: Throwable) {
            outputViewModel.addOutputEntry("Connection to DataBroker failed: ${error.message}")
            connectionViewModel.updateConnectionState(ConnectionViewState.DISCONNECTED)
        }
    }

    private val onDisconnectListener = DisconnectListener {
        connectionViewModel.updateConnectionState(ConnectionViewState.DISCONNECTED)
        outputViewModel.clear()
        outputViewModel.addOutputEntry("DataBroker disconnected")
    }

    private val propertyListener = object : PropertyListener {
        override fun onPropertyChanged(entryUpdates: List<KuksaValV1.EntryUpdate>) {
            Log.d(TAG, "onPropertyChanged() called with: updatedValues = $entryUpdates")
            outputViewModel.addOutputEntry("Updated value: $entryUpdates")
        }

        override fun onError(throwable: Throwable) {
            outputViewModel.addOutputEntry("${throwable.message}")
        }
    }

    private val specificationListener = object : VssSpecificationListener<VssSpecification> {
        override fun onSpecificationChanged(vssSpecification: VssSpecification) {
            outputViewModel.addOutputEntry("Updated specification: $vssSpecification")
        }

        override fun onError(throwable: Throwable) {
            outputViewModel.addOutputEntry("Updated specification: ${throwable.message}")
        }
    }

    private lateinit var dataBrokerEngine: DataBrokerEngine
    private val kotlinDataBrokerEngine by lazy {
        KotlinDataBrokerEngine(lifecycleScope)
    }

    private val javaDataBrokerEngine by lazy {
        JavaDataBrokerEngine()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate() called with: savedInstanceState = $savedInstanceState")

        setContent {
            KuksaAppAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DataBrokerView(
                        topAppBarViewModel,
                        connectionViewModel,
                        vssPropertiesViewModel,
                        vssSpecificationsViewModel,
                        outputViewModel,
                    )
                }
            }
        }

        connectionInfoRepository = ConnectionInfoRepository(this)

        dataBrokerEngine = kotlinDataBrokerEngine

        topAppBarViewModel.onCompatibilityModeChanged = { isCompatibilityModeEnabled ->
            dataBrokerEngine = if (isCompatibilityModeEnabled) {
                javaDataBrokerEngine
            } else {
                kotlinDataBrokerEngine
            }
            val enabledState = if (isCompatibilityModeEnabled) "enabled" else "disabled"
            outputViewModel.addOutputEntry("Java Compatibility Mode $enabledState")
        }

        connectionViewModel.onConnect = { connectionInfo ->
            connect(connectionInfo)
        }

        connectionViewModel.onDisconnect = {
            disconnect()
        }

        vssPropertiesViewModel.onGetProperty = { property: Property ->
            fetchPropertyFieldType(property)
            fetchProperty(property)
        }

        vssPropertiesViewModel.onSetProperty = { property: Property, datapoint: Datapoint ->
            updateProperty(property, datapoint)
        }

        vssPropertiesViewModel.onSubscribeProperty = { property: Property ->
            dataBrokerEngine.subscribe(property, propertyListener)
        }

        vssPropertiesViewModel.onUnsubscribeProperty = { property: Property ->
            dataBrokerEngine.unsubscribe(property, propertyListener)
        }

        vssSpecificationsViewModel.onSubscribeSpecification = { specification ->
            dataBrokerEngine.subscribe(specification, specificationListener)
        }

        vssSpecificationsViewModel.onUnsubscribeSpecification = { specification ->
            dataBrokerEngine.unsubscribe(specification, specificationListener)
        }

        vssSpecificationsViewModel.onGetSpecification = { specification ->
            fetchSpecification(specification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        dataBrokerEngine.unregisterDisconnectListener(onDisconnectListener)
    }

    private fun connect(connectionInfo: ConnectionInfo) {
        Log.d(TAG, "Connecting to DataBroker: $connectionInfo")

        outputViewModel.addOutputEntry("Connecting to data broker - Please wait")
        connectionViewModel.updateConnectionState(ConnectionViewState.CONNECTING)

        dataBrokerEngine.registerDisconnectListener(onDisconnectListener)
        dataBrokerEngine.connect(this, connectionInfo, dataBrokerConnectionCallback)
    }

    private fun disconnect() {
        Log.d(TAG, "Disconnecting from DataBroker")
        dataBrokerEngine.disconnect()
        dataBrokerEngine.unregisterDisconnectListener(onDisconnectListener)
    }

    private fun fetchPropertyFieldType(property: Property) {
        val propertyWithMetaData = property.copy(fields = listOf(Field.FIELD_METADATA))

        dataBrokerEngine.fetch(
            propertyWithMetaData,
            object : CoroutineCallback<GetResponse>() {
                override fun onSuccess(result: GetResponse?) {
                    val entriesMetadata = result?.entriesMetadata ?: emptyList()
                    val automaticValueType = if (entriesMetadata.size == 1) {
                        entriesMetadata.first().valueType
                    } else {
                        Types.Datapoint.ValueCase.VALUE_NOT_SET
                    }

                    Log.d(TAG, "Fetched automatic value type from meta data: $automaticValueType")

                    val vssProperties = vssPropertiesViewModel.vssProperties
                        .copy(valueType = automaticValueType)
                    vssPropertiesViewModel.updateVssProperties(vssProperties)
                }

                override fun onError(error: Throwable) {
                    Log.w(TAG, "Could not resolve type of value for $property")
                }
            },
        )
    }

    private fun fetchProperty(property: Property) {
        Log.d(TAG, "Fetch property: $property")

        dataBrokerEngine.fetch(
            property,
            object : CoroutineCallback<GetResponse>() {
                override fun onSuccess(result: GetResponse?) {
                    val errorsList = result?.errorsList
                    errorsList?.forEach {
                        outputViewModel.addOutputEntry(it.toString())

                        return
                    }

                    val outputEntry = OutputEntry()
                    result?.entriesList?.withIndex()?.forEach {
                        val dataEntry = it.value
                        val text = dataEntry.toString().substringAfter("\n")

                        outputEntry.addMessage(text)
                    }
                    outputViewModel.addOutputEntry(outputEntry)
                }

                override fun onError(error: Throwable) {
                    outputViewModel.addOutputEntry("Connection to data broker failed: ${error.message}")
                }
            },
        )
    }

    private fun updateProperty(property: Property, datapoint: Datapoint) {
        Log.d(TAG, "Update property: $property dataPoint: $datapoint, type: ${datapoint.valueCase}")

        dataBrokerEngine.update(
            property,
            datapoint,
            object : CoroutineCallback<KuksaValV1.SetResponse>() {
                override fun onSuccess(result: KuksaValV1.SetResponse?) {
                    val errorsList = result?.errorsList
                    errorsList?.forEach {
                        outputViewModel.addOutputEntry(it.toString())
                        return
                    }

                    outputViewModel.addOutputEntry(result.toString())
                }

                override fun onError(error: Throwable) {
                    outputViewModel.addOutputEntry("Connection to data broker failed: ${error.message}")
                }
            },
        )
    }

    private fun fetchSpecification(specification: VssSpecification) {
        dataBrokerEngine.fetch(
            specification,
            object : CoroutineCallback<VssSpecification>() {
                override fun onSuccess(result: VssSpecification?) {
                    Log.d(TAG, "Fetched specification: $result")
                    outputViewModel.addOutputEntry("Fetched specification: $result")
                }

                override fun onError(error: Throwable) {
                    outputViewModel.addOutputEntry("Could not fetch specification: ${error.message}")
                }
            },
        )
    }

    private fun loadVssPathSuggestions() {
        val property = Property("Vehicle", listOf(Field.FIELD_VALUE))

        dataBrokerEngine.fetch(
            property,
            object : CoroutineCallback<GetResponse>() {
                override fun onSuccess(result: GetResponse?) {
                    val entriesList = result?.entriesList
                    val vssPaths = entriesList?.map { it.path } ?: emptyList()

                    vssPropertiesViewModel.updateSuggestions(vssPaths)
                }

                override fun onError(error: Throwable) {
                    outputViewModel.addOutputEntry(error.toString())
                }
            },
        )
    }
}
