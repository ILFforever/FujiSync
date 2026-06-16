package com.ilfforever.fujisync.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.R
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

private const val BMC_URL = "https://buymeacoffee.com/ilfforever"

@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current

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
                text = "SUPPORT",
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

            Image(
                painter = painterResource(R.drawable.developer_photo),
                contentDescription = "Developer",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1437f / 1080f)
                    .clip(RoundedCornerShape(14.dp)),
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(18.dp),
            ) {
                Text(
                    text = "FujiSync is free and always will be.",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "If you decide to support this app — thank you, that means so much to me. And if you don't, just spreading the word is plenty good. Honestly, just seeing people enjoy something I made is more than enough.",
                    fontFamily = SansFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = TextMuted,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "BUY ME A COFFEE",
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
                    .border(1.dp, Border, RoundedCornerShape(14.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BMC_URL))) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "buymeacoffee.com/ilfforever",
                            fontFamily = SansFamily,
                            fontSize = 14.5.sp,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Opens in browser",
                            fontFamily = SansFamily,
                            fontSize = 12.sp,
                            color = TextDim,
                        )
                    }
                    Text(
                        text = "›",
                        fontFamily = MonoFamily,
                        fontSize = 14.sp,
                        color = Gold,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
