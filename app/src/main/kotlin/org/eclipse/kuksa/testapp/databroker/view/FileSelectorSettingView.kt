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

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import org.eclipse.kuksa.testapp.R

@Suppress("SameParameterValue") // re-usability
@Composable
fun FileSelectorSettingView(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onResult: (Uri) -> Unit,
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        val uri = it ?: return@rememberLauncherForActivityResult

        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        onResult(uri)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val fileTypes = arrayOf("*/*")
                launcher.launch(fileTypes)
            },
    ) {
        ConstraintLayout {
            val (labelRef, valueRef, imageRef) = createRefs()

            Text(
                text = label,
                modifier = Modifier.constrainAs(labelRef) {
                    width = Dimension.fillToConstraints
                    start.linkTo(parent.start)
                    end.linkTo(imageRef.start)
                },
            )
            Text(
                text = value,
                fontSize = 13.sp,
                modifier = Modifier.constrainAs(valueRef) {
                    width = Dimension.fillToConstraints
                    start.linkTo(parent.start)
                    end.linkTo(imageRef.start)
                    top.linkTo(labelRef.bottom)
                },
            )
            Image(
                painter = painterResource(id = R.drawable.baseline_upload_file_24),
                contentDescription = "Select Certifcate",
                modifier = Modifier.constrainAs(imageRef) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.fillToConstraints
                },
            )
        }
    }
}

@Preview
@Composable
fun FileSelectorSettingPreview() {
    FileSelectorSettingView(label = "Certificate", value = "CA.pem") {
        // unused in preview
    }
}
