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

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.eclipse.kuksa.proto.v1.Types.Datapoint.ValueCase
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo
import org.eclipse.kuksa.testapp.databroker.viewmodel.ConnectionViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.ConnectionViewModel.*
import org.eclipse.kuksa.testapp.databroker.viewmodel.OutputViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.TopAppBarViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.TopAppBarViewModel.DataBrokerMode
import org.eclipse.kuksa.testapp.databroker.viewmodel.VSSPropertiesViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.VssSpecificationsViewModel
import org.eclipse.kuksa.testapp.extension.compose.Headline
import org.eclipse.kuksa.testapp.extension.compose.LazyDropdownMenu
import org.eclipse.kuksa.testapp.extension.compose.OverflowMenu
import org.eclipse.kuksa.testapp.extension.compose.SimpleExposedDropdownMenuBox
import org.eclipse.kuksa.testapp.extension.compose.rememberCountdown
import org.eclipse.kuksa.testapp.ui.theme.KuksaAppAndroidTheme

val DefaultEdgePadding = 25.dp
val DefaultElementPadding = 10.dp
val MinimumButtonWidth = 150.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DataBrokerView(
    topAppBarViewModel: TopAppBarViewModel,
    connectionViewModel: ConnectionViewModel,
    vssPropertiesViewModel: VSSPropertiesViewModel,
    vssSpecificationsViewModel: VssSpecificationsViewModel,
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
                val dataBrokerMode = topAppBarViewModel.dataBrokerMode
                DataBrokerConnection(connectionViewModel)
                if (connectionViewModel.isConnected) {
                    AnimatedContent(
                        targetState = dataBrokerMode,
                        label = "DataBrokerModeAnimation",
                    ) { mode ->
                        when (mode) {
                            DataBrokerMode.VSS_PATH -> DataBrokerProperties(vssPropertiesViewModel)
                            DataBrokerMode.SPECIFICATION -> DataBrokerSpecifications(vssSpecificationsViewModel)
                        }
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
            OverflowMenu {
                Row(
                    modifier = Modifier
                        .clickable(
                            enabled = connectionViewModel.isDisconnected,
                        ) {
                            val newValue = !topAppBarViewModel.isCompatibilityModeEnabled
                            topAppBarViewModel.isCompatibilityModeEnabled = newValue
                        }
                        .padding(horizontal = 16.dp),
                ) {
                    Checkbox(
                        checked = topAppBarViewModel.isCompatibilityModeEnabled,
                        onCheckedChange = null,
                        enabled = connectionViewModel.isDisconnected,
                    )
                    Text(text = "Java Compatibility Mode", modifier = Modifier.padding(start = 16.dp))
                }
                Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
                Row(
                    modifier = Modifier
                        .clickable {
                            val newMode = if (!topAppBarViewModel.isSpecificationModeEnabled) {
                                DataBrokerMode.SPECIFICATION
                            } else {
                                DataBrokerMode.VSS_PATH
                            }
                            topAppBarViewModel.updateDataBrokerMode(newMode)
                        }
                        .padding(horizontal = 16.dp),
                ) {
                    Checkbox(
                        checked = topAppBarViewModel.isSpecificationModeEnabled,
                        onCheckedChange = null,
                    )
                    Text(text = "Specification Mode", modifier = Modifier.padding(start = 16.dp))
                }
            }
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DataBrokerConnection(viewModel: ConnectionViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val connectionInfoState by viewModel.connectionInfoFlow.collectAsStateWithLifecycle(initialValue = ConnectionInfo())

    var connectionInfo by remember(connectionInfoState) {
        mutableStateOf(connectionInfoState)
    }

    Headline("Connection")
    Column {
        AnimatedVisibility(visible = viewModel.isDisconnected) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = DefaultEdgePadding, end = DefaultEdgePadding),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TextField(
                        value = connectionInfo.host,
                        onValueChange = {
                            val newConnectionInfo = connectionInfoState.copy(host = it)
                            connectionInfo = newConnectionInfo
                        },
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.updateConnectionInfo(connectionInfo)
                                keyboardController?.hide()
                            },
                        ),
                        modifier = Modifier
                            .weight(2f),
                        singleLine = true,
                        label = {
                            Text(text = "Host")
                        },
                    )
                    Text(
                        text = ":",
                        modifier = Modifier
                            .padding(start = 5.dp, end = 5.dp)
                            .align(Alignment.CenterVertically),
                    )
                    TextField(
                        value = connectionInfo.port.toString(),
                        onValueChange = { value ->
                            try {
                                val port = value.toInt()
                                val newConnectionInfo = connectionInfo.copy(port = port)
                                connectionInfo = newConnectionInfo
                            } catch (e: NumberFormatException) {
                                // ignore gracefully
                            }
                        },
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.updateConnectionInfo(connectionInfo)
                                keyboardController?.hide()
                            },
                        ),
                        modifier = Modifier
                            .weight(1f),
                        singleLine = true,
                        label = {
                            Text(text = "Port")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = DefaultEdgePadding, end = DefaultEdgePadding),
                ) {
                    Text(text = "TLS:")
                    Checkbox(checked = connectionInfo.isTlsEnabled, onCheckedChange = { isChecked ->
                        val newConnectionInfo = connectionInfo.copy(isTlsEnabled = isChecked)
                        viewModel.updateConnectionInfo(newConnectionInfo)
                    })
                    TextField(
                        value = connectionInfo.certificate.overrideAuthority,
                        onValueChange = {
                            val certificate = connectionInfo.certificate.copy(overrideAuthority = it)
                            val newConnectionInfo = connectionInfo.copy(certificate = certificate)
                            connectionInfo = newConnectionInfo
                        },
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.updateConnectionInfo(connectionInfo)
                                keyboardController?.hide()
                            },
                        ),
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                        enabled = connectionInfo.isTlsEnabled,
                        label = {
                            Text(text = "Authority override")
                        },
                    )
                }
                Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AnimatedContent(
                targetState = viewModel.connectionViewState,
                label = "ConnectAnimation",
            ) { connectionViewState ->
                when (connectionViewState) {
                    ConnectionViewState.DISCONNECTED ->
                        Button(
                            onClick = {
                                viewModel.onConnect(connectionInfo)

                                keyboardController?.hide()
                            },
                            modifier = Modifier.width(MinimumButtonWidth),
                        ) {
                            Text(text = "Connect", textAlign = TextAlign.Center)
                        }

                    ConnectionViewState.CONNECTING ->
                        Button(
                            onClick = { },
                            modifier = Modifier.requiredWidth(MinimumButtonWidth),
                        ) {
                            val timeout by rememberCountdown(initialMillis = viewModel.connectionTimeoutMillis)

                            @Suppress("MagicNumber") // To seconds
                            val timeoutSeconds = timeout / 1000
                            Text(text = "Connecting... ($timeoutSeconds)", textAlign = TextAlign.Center)
                        }

                    ConnectionViewState.CONNECTED ->
                        Button(
                            onClick = { viewModel.onDisconnect() },
                            modifier = Modifier.requiredWidth(MinimumButtonWidth),
                        ) {
                            Text(text = "Disconnect")
                        }
                }
            }
        }
    }
}

@Composable
fun DataBrokerSpecifications(viewModel: VssSpecificationsViewModel) {
    Column {
        Headline(name = "Specifications")

        var selectedIndex by remember { mutableStateOf(0) }
        LazyDropdownMenu(
            modifier = Modifier
                .padding(start = DefaultEdgePadding, end = DefaultEdgePadding),
            items = viewModel.specifications,
            selectedIndex = selectedIndex,
            itemToString = { it.vssPath.substringAfter(".") },
            onItemSelected = { index, item ->
                selectedIndex = index
                viewModel.updateSpecification(item)
            },
        )
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = {
                    viewModel.onGetSpecification(viewModel.specification)
                },
                modifier = Modifier.requiredWidth(80.dp),
            ) {
                Text(text = "Get")
            }
            Button(onClick = {
                viewModel.onSubscribeSpecification(viewModel.specification)
            }) {
                Text(text = "Subscribe")
            }
        }
    }
}

@Composable
fun DataBrokerProperties(viewModel: VSSPropertiesViewModel) {
    val vssProperties = viewModel.vssProperties
    var expanded by remember { mutableStateOf(false) }

    Column {
        Headline(name = "Properties")
        TextField(
            value = viewModel.vssProperties.vssPath,
            onValueChange = {
                val newVssProperties = viewModel.vssProperties.copy(
                    vssPath = it,
                    valueType = ValueCase.VALUE_NOT_SET,
                )
                viewModel.updateVssProperties(newVssProperties)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DefaultEdgePadding, end = DefaultEdgePadding),
            singleLine = true,
            label = {
                Text(text = "VSS Path")
            },
            suffix = {
                Row(modifier = Modifier.offset(x = 15.dp)) {
                    ClickableText(
                        text = AnnotatedString(": ${viewModel.vssProperties.valueType}"),
                        onClick = { expanded = !expanded },
                        style = TextStyle(fontStyle = FontStyle.Italic),
                    )
                    Box(modifier = Modifier.requiredHeight(23.dp)) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                modifier = Modifier.size(23.dp),
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "More",
                            )
                        }
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

                                        val newVssProperties = viewModel.vssProperties.copy(valueType = it)
                                        viewModel.updateVssProperties(newVssProperties)
                                    },
                                )
                            }
                        }
                    }
                }
            },
        )
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        Row {
            TextField(
                value = vssProperties.value,
                onValueChange = {
                    val newVssProperties = viewModel.vssProperties.copy(value = it)
                    viewModel.updateVssProperties(newVssProperties)
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
                    val newVssProperties = viewModel.vssProperties.copy(fieldType = it)
                    viewModel.updateVssProperties(newVssProperties)
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
                    viewModel.onGetProperty(viewModel.property)
                },
                modifier = Modifier.requiredWidth(80.dp),
            ) {
                Text(text = "Get")
            }
            Button(
                enabled = viewModel.vssProperties.valueType != ValueCase.VALUE_NOT_SET,
                onClick = {
                    viewModel.onSetProperty(viewModel.property, viewModel.datapoint)
                },
                modifier = Modifier.requiredWidth(80.dp),
            ) {
                Text(text = "Set")
            }
            Button(onClick = {
                viewModel.onSubscribeProperty(viewModel.property)
            }) {
                Text(text = "Subscribe")
            }
        }
    }
}

@Composable
fun DataBrokerOutput(viewModel: OutputViewModel, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)
    val scrollState = rememberScrollState(0)

    val output = viewModel.output

    Surface(
        modifier = modifier.height(500.dp),
        color = MaterialTheme.colorScheme.primary,
        shape = shape,
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            Headline(name = "Output", color = Color.White)
            output.forEach { outputElement ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = DefaultElementPadding, end = DefaultElementPadding),
                    text = outputElement,
                    textAlign = TextAlign.Start,
                    onTextLayout = {
                        scope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    KuksaAppAndroidTheme {
        DataBrokerView(
            TopAppBarViewModel(),
            ConnectionViewModel(LocalContext.current.applicationContext as Application),
            VSSPropertiesViewModel(),
            VssSpecificationsViewModel(),
            OutputViewModel(),
        )
    }
}
