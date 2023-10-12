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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.eclipse.kuksa.proto.v1.Types.Datapoint.ValueCase
import org.eclipse.kuksa.testapp.databroker.viewmodel.ConnectionViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.ConnectionViewModel.*
import org.eclipse.kuksa.testapp.databroker.viewmodel.OutputViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.TopAppBarViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.VSSPropertiesViewModel
import org.eclipse.kuksa.testapp.preferences.ConnectionInfoRepository
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
                DataBrokerConnection(connectionViewModel)
                DataBrokerProperties(vssPropertiesViewModel, connectionViewModel.isConnected)
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
                            val newValue = !topAppBarViewModel.isCompatibilityModeEnabled.value
                            topAppBarViewModel.isCompatibilityModeEnabled.value = newValue
                        }
                        .padding(horizontal = 16.dp),
                ) {
                    Checkbox(
                        checked = topAppBarViewModel.isCompatibilityModeEnabled.value,
                        onCheckedChange = null,
                        enabled = connectionViewModel.isDisconnected,
                    )
                    Text(text = "Java Compatibility Mode", modifier = Modifier.padding(start = 16.dp))
                }
            }
        },
    )
}

@Composable
fun OverflowMenu(content: @Composable () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    IconButton(onClick = {
        showMenu = !showMenu
    }) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = "Options",
        )
    }
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
    ) {
        content()
    }
}

@Composable
fun Headline(name: String, modifier: Modifier = Modifier, color: Color = Color.Black) {
    Text(
        text = name,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 15.dp, bottom = 15.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge,
        color = color,
    )
}

@Composable
fun rememberCountdown(
    initialMillis: Long,
    step: Long = 1000,
): MutableState<Long> {
    val timeLeft = remember { mutableStateOf(initialMillis) }

    LaunchedEffect(initialMillis, step) {
        while (isActive && timeLeft.value > 0) {
            val newTimeLeft = (timeLeft.value - step).coerceAtLeast(0)
            timeLeft.value = newTimeLeft

            val maximumDelay = step.coerceAtMost(newTimeLeft)
            delay(maximumDelay)
        }
    }

    return timeLeft
}

@Composable
fun DataBrokerProperties(viewModel: VSSPropertiesViewModel, isVisible: Boolean = true) {
    val vssProperties = viewModel.vssProperties
    var expanded by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Enum<T>> SimpleExposedDropdownMenuBox(
    modifier: Modifier = Modifier,
    label: String = "",
    list: List<T> = emptyList(),
    onValueChange: (T) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val firstElement = list.firstOrNull() ?: "No element"
    var selectedText by remember { mutableStateOf(firstElement) }

    ExposedDropdownMenuBox(
        expanded = false,
        onExpandedChange = {
            expanded = !expanded
        },
        modifier,
    ) {
        TextField(
            value = selectedText.toString(),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = {
                Text(text = label)
            },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { }) {
            list.forEach {
                DropdownMenuItem(
                    text = {
                        Text(text = it.name)
                    },
                    onClick = {
                        expanded = false
                        selectedText = it
                        onValueChange(it)
                    },
                )
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
                        .fillMaxHeight()
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
        val connectionInfoRepository = ConnectionInfoRepository(LocalContext.current)
        DataBrokerView(
            TopAppBarViewModel(),
            ConnectionViewModel(connectionInfoRepository),
            VSSPropertiesViewModel(),
            OutputViewModel(),
        )
    }
}
