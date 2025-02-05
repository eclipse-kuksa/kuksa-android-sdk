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
import org.eclipse.kuksa.connectivity.databroker.DisconnectListener
import org.eclipse.kuksa.connectivity.databroker.v1.DataBrokerConnection
import org.eclipse.kuksa.connectivity.databroker.v1.listener.VssNodeListener
import org.eclipse.kuksa.connectivity.databroker.v1.listener.VssPathListener
import org.eclipse.kuksa.connectivity.databroker.v1.request.FetchRequest
import org.eclipse.kuksa.connectivity.databroker.v1.request.SubscribeRequest
import org.eclipse.kuksa.connectivity.databroker.v1.request.UpdateRequest
import org.eclipse.kuksa.connectivity.databroker.v1.request.VssNodeFetchRequest
import org.eclipse.kuksa.connectivity.databroker.v1.request.VssNodeSubscribeRequest
import org.eclipse.kuksa.connectivity.databroker.v1.request.VssNodeUpdateRequest
import org.eclipse.kuksa.connectivity.databroker.v1.response.VssNodeUpdateResponse
import org.eclipse.kuksa.coroutine.CoroutineCallback
import org.eclipse.kuksa.extension.entriesMetadata
import org.eclipse.kuksa.extension.firstValue
import org.eclipse.kuksa.extension.stringValue
import org.eclipse.kuksa.extension.valueType
import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.Types.Field
import org.eclipse.kuksa.testapp.databroker.DataBrokerEngine
import org.eclipse.kuksa.testapp.databroker.JavaDataBrokerEngine
import org.eclipse.kuksa.testapp.databroker.KotlinDataBrokerEngine
import org.eclipse.kuksa.testapp.databroker.connection.model.ConnectionInfo
import org.eclipse.kuksa.testapp.databroker.connection.viewmodel.ConnectionViewModel
import org.eclipse.kuksa.testapp.databroker.connection.viewmodel.ConnectionViewModel.ConnectionViewState
import org.eclipse.kuksa.testapp.databroker.view.DataBrokerView
import org.eclipse.kuksa.testapp.databroker.viewmodel.OutputEntry
import org.eclipse.kuksa.testapp.databroker.viewmodel.OutputViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.TopAppBarViewModel
import org.eclipse.kuksa.testapp.databroker.vssnodes.viewmodel.VssNodesViewModel
import org.eclipse.kuksa.testapp.databroker.vsspaths.viewmodel.DataBrokerProperty
import org.eclipse.kuksa.testapp.databroker.vsspaths.viewmodel.VSSPathsViewModel
import org.eclipse.kuksa.testapp.extension.TAG
import org.eclipse.kuksa.testapp.preferences.ConnectionInfoRepository
import org.eclipse.kuksa.testapp.ui.theme.KuksaAppAndroidTheme
import org.eclipse.kuksa.vsscore.annotation.VssModelGenerator
import org.eclipse.kuksa.vsscore.model.VssNode
import org.eclipse.velocitas.vss.VssVehicle

@VssModelGenerator
class KuksaDataBrokerActivity : ComponentActivity() {
    private lateinit var connectionInfoRepository: ConnectionInfoRepository

    private val topAppBarViewModel: TopAppBarViewModel by viewModels()
    private val connectionViewModel: ConnectionViewModel by viewModels {
        ConnectionViewModel.Factory(connectionInfoRepository)
    }
    private val vssPathsViewModel: VSSPathsViewModel by viewModels()
    private val vssNodesViewModel: VssNodesViewModel by viewModels()
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

    private val vssPathListener = object : VssPathListener {
        override fun onEntryChanged(entryUpdates: List<KuksaValV1.EntryUpdate>) {
            Log.d(TAG, "onEntryChanged() called with: updatedValues = $entryUpdates")

            val entries = mutableListOf<String>().apply {
                add("Updated Entries")
                addAll(entryUpdates.map { it.entry.toString() })
            }
            val outputEntry = OutputEntry(messages = entries)
            outputViewModel.addOutputEntry(outputEntry)
        }

        override fun onError(throwable: Throwable) {
            outputViewModel.addOutputEntry("${throwable.message}")
        }
    }

    private val vssNodeListener = object : VssNodeListener<VssNode> {
        override fun onNodeChanged(vssNode: VssNode) {
            outputViewModel.addOutputEntry("Updated node: $vssNode")
        }

        override fun onError(throwable: Throwable) {
            outputViewModel.addOutputEntry("Updated node: ${throwable.message}")
        }
    }

    private lateinit var dataBrokerEngine: DataBrokerEngine
    private val kotlinDataBrokerEngine by lazy {
        KotlinDataBrokerEngine(lifecycleScope)
    }

    private val javaDataBrokerEngine by lazy {
        JavaDataBrokerEngine()
    }

    @Suppress("performance:SpreadOperator") // Neglectable: Field types are 1-2 elements mostly
    override fun onCreate(savedInstanceState: Bundle?) {
        fun addVssPathsListeners() {
            vssPathsViewModel.onGetProperty = { property: DataBrokerProperty ->
                fetchPropertyFieldType(property)
                fetchProperty(property)
            }

            vssPathsViewModel.onSetProperty = { property: DataBrokerProperty, datapoint: Datapoint ->
                updateProperty(property, datapoint)
            }

            vssPathsViewModel.onSubscribeProperty = { property: DataBrokerProperty ->
                val request = SubscribeRequest(property.vssPath, *property.fieldTypes.toTypedArray())
                dataBrokerEngine.subscribe(request, vssPathListener)
            }

            vssPathsViewModel.onUnsubscribeProperty = { property: DataBrokerProperty ->
                val request = SubscribeRequest(property.vssPath, *property.fieldTypes.toTypedArray())
                dataBrokerEngine.unsubscribe(request, vssPathListener)
            }
        }

        fun addVssNodesListeners() {
            vssNodesViewModel.onSubscribeNode = { vssNode ->
                val request = VssNodeSubscribeRequest(vssNode)
                dataBrokerEngine.subscribe(request, vssNodeListener)
            }

            vssNodesViewModel.onUnsubscribeNode = { vssNode ->
                val request = VssNodeSubscribeRequest(vssNode)
                dataBrokerEngine.unsubscribe(request, vssNodeListener)
            }

            vssNodesViewModel.onUpdateSignal = { signal ->
                val request = VssNodeUpdateRequest(signal)
                dataBrokerEngine.update(
                    request,
                    object : CoroutineCallback<VssNodeUpdateResponse>() {
                        override fun onSuccess(result: VssNodeUpdateResponse?) {
                            val errorsList = result?.flatMap { it.errorsList }
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

            vssNodesViewModel.onGetNode = { vssNode ->
                fetchVssNode(vssNode)
            }
        }

        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate() called with: savedInstanceState = $savedInstanceState")

        setContent {
            KuksaAppAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DataBrokerView(
                        topAppBarViewModel,
                        connectionViewModel,
                        vssPathsViewModel,
                        vssNodesViewModel,
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

        addVssPathsListeners()
        addVssNodesListeners()
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

    private fun fetchPropertyFieldType(property: DataBrokerProperty) {
        val request = FetchRequest(property.vssPath, Field.FIELD_METADATA)

        dataBrokerEngine.fetch(
            request,
            object : CoroutineCallback<GetResponse>() {
                override fun onSuccess(result: GetResponse?) {
                    val entriesMetadata = result?.entriesMetadata ?: emptyList()
                    val automaticValueType = if (entriesMetadata.size == 1) {
                        entriesMetadata.first().valueType
                    } else {
                        Datapoint.ValueCase.VALUE_NOT_SET
                    }

                    Log.d(TAG, "Fetched automatic value type from meta data: $automaticValueType")

                    val dataBrokerProperty = vssPathsViewModel.dataBrokerProperty
                        .copy(valueType = automaticValueType)
                    vssPathsViewModel.updateDataBrokerProperty(dataBrokerProperty)
                }

                override fun onError(error: Throwable) {
                    Log.w(TAG, "Could not resolve type of value for $property")
                }
            },
        )
    }

    @Suppress("performance:SpreadOperator") // Neglectable: Field types are 1-2 elements mostly
    private fun fetchProperty(property: DataBrokerProperty) {
        Log.d(TAG, "Fetch property: $property")

        val request = FetchRequest(property.vssPath, *property.fieldTypes.toTypedArray())

        dataBrokerEngine.fetch(
            request,
            object : CoroutineCallback<GetResponse>() {
                override fun onSuccess(result: GetResponse?) {
                    if (result == null) return

                    val errorsList = result.errorsList
                    errorsList?.forEach {
                        outputViewModel.addOutputEntry(it.toString())

                        return
                    }

                    val outputEntry = OutputEntry()
                    result.entriesList?.withIndex()?.forEach {
                        val dataEntry = it.value
                        val text = dataEntry.toString().substringAfter("\n")

                        outputEntry.addMessage(text)
                    }

                    outputViewModel.addOutputEntry(outputEntry)

                    val updatedValue = result.firstValue?.stringValue ?: ""
                    val dataBrokerProperty = vssPathsViewModel.dataBrokerProperty
                        .copy(value = updatedValue)
                    vssPathsViewModel.updateDataBrokerProperty(dataBrokerProperty)
                }

                override fun onError(error: Throwable) {
                    outputViewModel.addOutputEntry("Connection to data broker failed: ${error.message}")
                }
            },
        )
    }

    @Suppress("performance:SpreadOperator") // Neglectable: Field types are 1-2 elements mostly
    private fun updateProperty(property: DataBrokerProperty, datapoint: Datapoint) {
        Log.d(TAG, "Update property: $property dataPoint: $datapoint, type: ${datapoint.valueCase}")

        val request = UpdateRequest(property.vssPath, datapoint, *property.fieldTypes.toTypedArray())
        dataBrokerEngine.update(
            request,
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

    private fun fetchVssNode(vssNode: VssNode) {
        val request = VssNodeFetchRequest(vssNode)
        dataBrokerEngine.fetch(
            request,
            object : CoroutineCallback<VssNode>() {
                override fun onSuccess(result: VssNode?) {
                    Log.d(TAG, "Fetched node: $result")

                    if (result == null) return

                    vssNodesViewModel.updateNode(result)

                    outputViewModel.addOutputEntry("Fetched node: $result")
                }

                override fun onError(error: Throwable) {
                    outputViewModel.addOutputEntry("Could not fetch node: ${error.message}")
                }
            },
        )
    }

    private fun loadVssPathSuggestions() {
        val property = FetchRequest(VssVehicle().vssPath, Field.FIELD_VALUE)

        dataBrokerEngine.fetch(
            property,
            object : CoroutineCallback<GetResponse>() {
                override fun onSuccess(result: GetResponse?) {
                    val entriesList = result?.entriesList
                    val vssPaths = entriesList?.map { it.path } ?: emptyList()

                    vssPathsViewModel.updateSuggestions(vssPaths)
                }

                override fun onError(error: Throwable) {
                    outputViewModel.addOutputEntry(error.toString())
                }
            },
        )
    }
}
