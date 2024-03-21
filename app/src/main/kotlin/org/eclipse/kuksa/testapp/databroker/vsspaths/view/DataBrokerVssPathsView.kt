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

package org.eclipse.kuksa.testapp.databroker.vsspaths.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.testapp.databroker.view.DefaultEdgePadding
import org.eclipse.kuksa.testapp.databroker.view.DefaultElementPadding
import org.eclipse.kuksa.testapp.databroker.view.suggestions.SuggestionTextView
import org.eclipse.kuksa.testapp.databroker.vsspaths.viewmodel.VSSPathsViewModel
import org.eclipse.kuksa.testapp.extension.compose.Headline
import org.eclipse.kuksa.testapp.extension.compose.SimpleExposedDropdownMenuBox
import org.eclipse.kuksa.testapp.ui.theme.KuksaAppAndroidTheme

@Composable
fun DataBrokerVssPathsView(viewModel: VSSPathsViewModel) {
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
                    valueType = Types.Datapoint.ValueCase.VALUE_NOT_SET,
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
                enabled = dataBrokerProperty.valueType != Types.Datapoint.ValueCase.VALUE_NOT_SET,
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

@Preview(showBackground = true)
@Composable
fun Preview() {
    KuksaAppAndroidTheme {
        DataBrokerVssPathsView(VSSPathsViewModel())
    }
}
