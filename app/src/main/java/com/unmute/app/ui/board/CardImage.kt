package com.unmute.app.ui.board

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unmute.app.data.local.CardEntity
import com.unmute.app.domain.model.ImageType
import java.io.File

@Composable
fun CardImage(card: CardEntity, modifier: Modifier = Modifier) {
    when (card.imageType) {
        ImageType.SYMBOL -> AsyncImage(
            model = "file:///android_asset/${card.imageValue}",
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp),
        )

        ImageType.PHOTO -> {
            val model = remember(card.imageValue) {
                if (card.imageValue.startsWith("content://") || card.imageValue.startsWith("http")) {
                    card.imageValue
                } else {
                    File(card.imageValue)
                }
            }
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize(),
            )
        }

        ImageType.EMOJI -> Text(
            text = card.imageValue,
            fontSize = 56.sp,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxSize()
                .padding(8.dp),
        )
    }
}
