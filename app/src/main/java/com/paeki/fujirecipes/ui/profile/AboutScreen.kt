package com.paeki.fujirecipes.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.ui.components.Wordmark
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

private const val VERSION = "0.1.0"

private val MIT_LICENSE = """
MIT License

Copyright (c) 2026 FujiSync Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
""".trimIndent()

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "‹",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Gold,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 12.dp, top = 2.dp, bottom = 2.dp),
            )
            Text(
                text = "ABOUT",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // App identity block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(18.dp),
            ) {
                Wordmark()
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Manage Fujifilm X-series film simulation recipes from your phone over USB-C OTG.",
                    fontFamily = SansFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = TextMuted,
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AboutChip(label = "VERSION", value = VERSION)
                }
            }

            Spacer(Modifier.height(24.dp))

            // License
            Text(
                text = "LICENSE",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = TextDim,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = "MIT",
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                    color = Gold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Text(
                    text = MIT_LICENSE,
                    fontFamily = MonoFamily,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = TextDim,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AboutChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Gold.copy(alpha = 0.10f))
            .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Row {
            Text(
                text = "$label  ",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = TextDim,
            )
            Text(
                text = value,
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = Gold,
            )
        }
    }
}
