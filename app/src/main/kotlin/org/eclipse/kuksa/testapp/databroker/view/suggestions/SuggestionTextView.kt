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

package org.eclipse.kuksa.testapp.databroker.view.suggestions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import org.eclipse.kuksa.testapp.R

@Composable
fun <T : Any> SuggestionTextView(
    adapter: SuggestionAdapter<T> = DefaultSuggestionAdapter(),
    onItemSelected: ((T?) -> Unit)? = null,
    onValueChanged: ((String) -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    modifier: Modifier,
) {
    val suggestions = adapter.items

    var text by remember {
        mutableStateOf(adapter.toString(adapter.startingItem))
    }

    val heightTextFields by remember {
        mutableStateOf(55.dp)
    }

    var textFieldSize by remember {
        mutableStateOf(Size.Zero)
    }

    var expanded by remember {
        mutableStateOf(false)
    }
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    expanded = false
                },
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    label = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heightTextFields)
                        .onGloballyPositioned { coordinates ->
                            textFieldSize = coordinates.size.toSize()
                        }
                        .background(Color.Transparent),
                    value = text,
                    onValueChange = {
                        text = it
                        expanded = true
                        onValueChanged?.invoke(it)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.Black,
                    ),
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 16.sp,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            expanded = false
                            focusManager.clearFocus()
                        },
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = singleLine,
                    trailingIcon = {
                        Row {
                            if (text.isNotEmpty()) {
                                IconButton(onClick = {
                                    text = ""
                                    expanded = false
                                    onValueChanged?.invoke(text)
                                    onItemSelected?.invoke(null)
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "clear",
                                        tint = Color.Black,
                                    )
                                }
                            }
                            if (suggestions.isNotEmpty()) {
                                IconButton(onClick = { expanded = !expanded }) {
                                    val drawableRes = if (expanded) {
                                        R.drawable.baseline_arrow_drop_up_24
                                    } else {
                                        R.drawable.baseline_arrow_drop_down_24
                                    }

                                    Icon(
                                        painter = painterResource(id = drawableRes),
                                        contentDescription = "suggestions",
                                        tint = Color.Black,
                                    )
                                }
                            }
                        }
                    },
                )
            }

            AnimatedVisibility(visible = expanded) {
                Card(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .width(textFieldSize.width.dp),
                    elevation = CardDefaults.elevatedCardElevation(),
                    shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 150.dp),
                    ) {
                        items(
                            suggestions.filter { item ->
                                adapter.toString(item).lowercase().contains(text.lowercase())
                            },
                        ) { item ->
                            SuggestionItem(
                                item = item,
                                itemText = adapter.toString(item),
                            ) { suggestionItem ->
                                text = adapter.toString(suggestionItem)
                                expanded = false
                                focusManager.clearFocus()
                                onValueChanged?.invoke(text)
                                onItemSelected?.invoke(suggestionItem)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T : Any> SuggestionItem(
    item: T,
    itemText: String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelect(item)
            }
            .padding(10.dp),
    ) {
        Text(text = itemText, fontSize = 16.sp)
    }
}
