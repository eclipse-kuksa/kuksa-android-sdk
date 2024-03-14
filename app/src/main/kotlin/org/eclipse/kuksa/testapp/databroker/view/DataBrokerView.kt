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

package org.eclipse.kuksa.testapp.databroker.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.eclipse.kuksa.proto.v1.Types.Datapoint.ValueCase
import org.eclipse.kuksa.testapp.R
import org.eclipse.kuksa.testapp.databroker.view.connection.DataBrokerConnectionView
import org.eclipse.kuksa.testapp.databroker.view.suggestions.SuggestionAdapter
import org.eclipse.kuksa.testapp.databroker.view.suggestions.SuggestionTextView
import org.eclipse.kuksa.testapp.databroker.viewmodel.ConnectionViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.OutputViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.TopAppBarViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.TopAppBarViewModel.DataBrokerMode
import org.eclipse.kuksa.testapp.databroker.viewmodel.VSSPathsViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.VssNodesViewModel
import org.eclipse.kuksa.testapp.extension.compose.Headline
import org.eclipse.kuksa.testapp.extension.compose.OverflowMenu
import org.eclipse.kuksa.testapp.extension.compose.SimpleExposedDropdownMenuBox
import org.eclipse.kuksa.testapp.preferences.ConnectionInfoRepository
import org.eclipse.kuksa.testapp.ui.theme.KuksaAppAndroidTheme
import org.eclipse.kuksa.vss.VssVehicle
import org.eclipse.kuksa.vsscore.model.VssNode
import java.time.format.DateTimeFormatter

val SettingsMenuPadding = 16.dp
val DefaultEdgePadding = 25.dp
val DefaultElementPadding = 10.dp
val MinimumButtonWidth = 150.dp

@Composable
fun DataBrokerView(
    topAppBarViewModel: TopAppBarViewModel,
    connectionViewModel: ConnectionViewModel,
    vssPathsViewModel: VSSPathsViewModel,
    vssNodesViewModel: VssNodesViewModel,
    outputViewModel: OutputViewModel,
) {
    Scaffold(
        topBar = {
            TopBar(topAppBarViewModel, connectionViewModel)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .consumeWindowInsets(paddingValues = it),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                if (!connectionViewModel.isConnected) {
                    DataBrokerConnectionView(connectionViewModel)
                }
                val dataBrokerMode = topAppBarViewModel.dataBrokerMode
                if (connectionViewModel.isConnected) {
                    when (dataBrokerMode) {
                        DataBrokerMode.VSS_PATH -> DataBrokerProperties(vssPathsViewModel)
                        DataBrokerMode.VSS_FILE -> DataBrokerVssNodes(vssNodesViewModel)
                    }
                }
                Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
            }
            Column {
                DataBrokerOutput(outputViewModel)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopBar(
    topAppBarViewModel: TopAppBarViewModel,
    connectionViewModel: ConnectionViewModel,
) {
    TopAppBar(
        title = { Text("TestApp") },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        actions = {
            ConnectionStatusIcon(connectionViewModel)
            SettingsMenu(connectionViewModel, topAppBarViewModel)
        },
    )
}

@Composable
private fun SettingsMenu(
    connectionViewModel: ConnectionViewModel,
    topAppBarViewModel: TopAppBarViewModel,
) {
    OverflowMenu {
        Row(
            modifier = Modifier
                .clickable(
                    enabled = connectionViewModel.isDisconnected,
                ) {
                    val newValue = !topAppBarViewModel.isCompatibilityModeEnabled
                    topAppBarViewModel.isCompatibilityModeEnabled = newValue
                }
                .padding(horizontal = SettingsMenuPadding),
        ) {
            Checkbox(
                checked = topAppBarViewModel.isCompatibilityModeEnabled,
                onCheckedChange = null,
                enabled = connectionViewModel.isDisconnected,
            )
            Text(
                text = "Java Compatibility Mode",
                modifier = Modifier.padding(start = SettingsMenuPadding),
            )
        }
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        Row(
            modifier = Modifier
                .clickable {
                    val newMode = if (!topAppBarViewModel.isVssFileModeEnabled) {
                        DataBrokerMode.VSS_FILE
                    } else {
                        DataBrokerMode.VSS_PATH
                    }
                    topAppBarViewModel.updateDataBrokerMode(newMode)
                }
                .padding(horizontal = SettingsMenuPadding),
        ) {
            Checkbox(
                checked = topAppBarViewModel.isVssFileModeEnabled,
                onCheckedChange = null,
            )
            Text(text = "VSS Mode", modifier = Modifier.padding(start = SettingsMenuPadding))
        }
    }
}

@Composable
private fun ConnectionStatusIcon(
    connectionViewModel: ConnectionViewModel,
) {
    val modifier: Modifier = Modifier
    if (connectionViewModel.isConnected) {
        Icon(
            painter = painterResource(id = R.drawable.round_power_24),
            contentDescription = "Disconnect",
            modifier = modifier.clickable(enabled = connectionViewModel.isConnected) {
                connectionViewModel.onDisconnect()
            },
        )
    } else {
        Icon(
            painter = painterResource(id = R.drawable.round_power_off_24),
            contentDescription = "Connect",
            modifier = modifier.clickable(enabled = connectionViewModel.isDisconnected) {
                val coroutineScope = CoroutineScope(Dispatchers.Default)
                coroutineScope.launch {
                    val connectionInfo = connectionViewModel.connectionInfoFlow.first()
                    connectionViewModel.onConnect(connectionInfo)
                }
            },
        )
    }
}

@Composable
fun DataBrokerVssNodes(viewModel: VssNodesViewModel) {
    Column {
        Headline(name = "Generated VSS Nodes")

        val adapter = object : SuggestionAdapter<VssNode> {
            override fun toString(item: VssNode): String {
                return item.vssPath
            }
        }

        SuggestionTextView(
            value = "Vehicle",
            suggestions = viewModel.vssNodes,
            adapter = adapter,
            onItemSelected = {
                val vssNode = it ?: VssVehicle()
                viewModel.updateNode(vssNode)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DefaultEdgePadding, end = DefaultEdgePadding),
        )
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = {
                    viewModel.onGetNode(viewModel.node)
                },
                modifier = Modifier.requiredWidth(80.dp),
            ) {
                Text(text = "Get")
            }
            if (viewModel.isSubscribed) {
                Button(onClick = {
                    viewModel.subscribedNodes.remove(viewModel.node)
                    viewModel.onUnsubscribeNode(viewModel.node)
                }) {
                    Text(text = "Unsubscribe")
                }
            } else {
                Button(onClick = {
                    viewModel.subscribedNodes.add(viewModel.node)
                    viewModel.onSubscribeNode(viewModel.node)
                }) {
                    Text(text = "Subscribe")
                }
            }
        }
    }
}

@Composable
fun DataBrokerProperties(viewModel: VSSPathsViewModel) {
    val dataBrokerProperty = viewModel.dataBrokerProperty
    var expanded by remember { mutableStateOf(false) }

    Column {
        Headline(name = "VSS Paths")
        SuggestionTextView(
            suggestions = viewModel.suggestions,
            value = dataBrokerProperty.vssPath,
            onValueChanged = {
                val newVssProperties = dataBrokerProperty.copy(
                    vssPath = it,
                    valueType = ValueCase.VALUE_NOT_SET,
                )
                viewModel.updateDataBrokerProperty(newVssProperties)
            },
            label = {
                Text(text = "VSS Path")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DefaultEdgePadding, end = DefaultEdgePadding),
        )
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        Row {
            TextField(
                value = "${dataBrokerProperty.valueType}",
                onValueChange = {},
                label = {
                    Text("Value Type")
                },
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            modifier = Modifier.size(23.dp),
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "More",
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledLabelColor = Color.Black,
                    disabledTrailingIconColor = Color.Black,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = DefaultEdgePadding, end = DefaultEdgePadding)
                    .clickable {
                        expanded = true
                    },
            )
            Box(modifier = Modifier.requiredHeight(23.dp)) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.height(400.dp),
                ) {
                    viewModel.valueTypes.forEach {
                        DropdownMenuItem(
                            text = {
                                Text(it.toString())
                            },
                            onClick = {
                                expanded = false

                                val newVssProperties = dataBrokerProperty.copy(valueType = it)
                                viewModel.updateDataBrokerProperty(newVssProperties)
                            },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        Row {
            TextField(
                value = dataBrokerProperty.value,
                onValueChange = {
                    val newVssProperties = dataBrokerProperty.copy(value = it)
                    viewModel.updateDataBrokerProperty(newVssProperties)
                },
                modifier = Modifier
                    .padding(start = DefaultEdgePadding)
                    .weight(1f),
                singleLine = true,
                label = {
                    Text(text = "Value")
                },
            )
            Spacer(modifier = Modifier.padding(start = DefaultElementPadding, end = DefaultElementPadding))
            SimpleExposedDropdownMenuBox(
                Modifier
                    .weight(2f)
                    .padding(end = DefaultEdgePadding),
                label = "Field Type",
                list = viewModel.fieldTypes,
                onValueChange = {
                    val newVssProperties = dataBrokerProperty.copy(fieldTypes = setOf(it))
                    viewModel.updateDataBrokerProperty(newVssProperties)
                },
            )
        }
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = {
                    viewModel.onGetProperty(dataBrokerProperty)
                },
                modifier = Modifier.requiredWidth(80.dp),
            ) {
                Text(text = "Get")
            }
            Button(
                enabled = dataBrokerProperty.valueType != ValueCase.VALUE_NOT_SET,
                onClick = {
                    viewModel.onSetProperty(dataBrokerProperty, viewModel.datapoint)
                },
                modifier = Modifier.requiredWidth(80.dp),
            ) {
                Text(text = "Set")
            }
            if (viewModel.isSubscribed) {
                Button(onClick = {
                    viewModel.subscribedProperties.remove(dataBrokerProperty)
                    viewModel.onUnsubscribeProperty(dataBrokerProperty)
                }) {
                    Text(text = "Unsubscribe")
                }
            } else {
                Button(onClick = {
                    viewModel.subscribedProperties.add(dataBrokerProperty)
                    viewModel.onSubscribeProperty(dataBrokerProperty)
                }) {
                    Text(text = "Subscribe")
                }
            }
        }
    }
}

@Composable
fun DataBrokerOutput(viewModel: OutputViewModel, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)
    val scrollState = rememberScrollState(0)

    val outputEntries = viewModel.output

    Surface(
        modifier = modifier.height(500.dp),
        color = MaterialTheme.colorScheme.primary,
        shape = shape,
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")
            Headline(name = "Output", color = Color.White)
            outputEntries.forEach { outputEntry ->
                val date = outputEntry.localDateTime.format(dateFormatter)
                val newLine = System.lineSeparator()

                val onTextLayout: ((TextLayoutResult) -> Unit) = {
                    scope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }

                val logTextModifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(start = DefaultElementPadding, end = DefaultElementPadding)

                OutputText(date, logTextModifier, onTextLayout)
                outputEntry.messages.forEach {
                    OutputText(it + newLine, logTextModifier, onTextLayout)
                }
            }
        }
    }
}

@Composable
private fun OutputText(
    text: String,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    Text(
        modifier = modifier,
        text = text,
        fontSize = 14.sp,
        textAlign = TextAlign.Start,
        onTextLayout = onTextLayout,
    )
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    KuksaAppAndroidTheme {
        val connectionInfoRepository = ConnectionInfoRepository(LocalContext.current)
        DataBrokerView(
            TopAppBarViewModel(),
            ConnectionViewModel(connectionInfoRepository),
            VSSPathsViewModel(),
            VssNodesViewModel(),
            OutputViewModel(),
        )
    }
}
