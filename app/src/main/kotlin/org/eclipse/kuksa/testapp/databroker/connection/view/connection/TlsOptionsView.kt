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

package org.eclipse.kuksa.testapp.databroker.connection.view.connection

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import org.eclipse.kuksa.testapp.databroker.connection.model.ConnectionInfo
import org.eclipse.kuksa.testapp.databroker.view.DefaultEdgePadding
import org.eclipse.kuksa.testapp.databroker.view.DefaultElementPadding
import org.eclipse.kuksa.testapp.databroker.view.FileSelectorSettingView
import org.eclipse.kuksa.testapp.extension.fetchFileName

@Composable
fun TlsOptionsView(
    connectionInfo: ConnectionInfo,
    modifier: Modifier = Modifier,
    onTlsStateChanged: (Boolean) -> Unit = {},
    onCertificateSelected: (Uri) -> Unit = {},
    onAuthorityOverrideChanged: (String) -> Unit = {},
    onKeyboardDone: (KeyboardActionScope) -> Unit = {},
) {
    val context = LocalContext.current

    Column(modifier = modifier.padding(start = DefaultEdgePadding, end = DefaultEdgePadding)) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onTlsStateChanged(!connectionInfo.isTlsEnabled)
                },
        ) {
            val (text, checkbox) = createRefs()
            Text(
                text = "TLS",
                modifier = Modifier.constrainAs(text) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            )
            Checkbox(
                checked = connectionInfo.isTlsEnabled,
                onCheckedChange = onTlsStateChanged,
                modifier = Modifier
                    .constrainAs(checkbox) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)

                        end.linkTo(parent.end)
                    },
            )
        }

        if (connectionInfo.isTlsEnabled) {
            val uri = connectionInfo.certificate.uri
            val fileName = uri.fetchFileName(context) ?: "Select certificate..."

            FileSelectorSettingView(
                label = "Certificate",
                value = fileName,
                onResult = onCertificateSelected,
                modifier = Modifier.padding(end = 10.dp),
            )

            Spacer(modifier = Modifier.padding(top = DefaultElementPadding))

            TextField(
                value = connectionInfo.certificate.overrideAuthority,
                onValueChange = onAuthorityOverrideChanged,
                keyboardActions = KeyboardActions(
                    onDone = onKeyboardDone,
                ),
                modifier = Modifier
                    .fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(text = "Authority override")
                },
            )
        }
    }
}

@Preview
@Composable
private fun DisabledTlsOptionsViewPreview() {
    val connectionInfo = ConnectionInfo(isTlsEnabled = false)

    Surface {
        TlsOptionsView(connectionInfo)
    }
}

@Preview
@Composable
private fun EnabledTlsOptionsViewPreview() {
    val connectionInfo = ConnectionInfo(isTlsEnabled = true)

    Surface {
        TlsOptionsView(connectionInfo)
    }
}
