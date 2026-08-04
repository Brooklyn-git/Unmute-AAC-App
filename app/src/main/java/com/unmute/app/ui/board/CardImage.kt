package com.unmute.app.ui.board

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unmute.app.data.local.CardEntity
import com.unmute.app.domain.model.ImageType
import java.io.File

@Composable
fun CardImage(card: CardEntity, modifier: Modifier = Modifier) {
    SymbolImage(
        imageType = card.imageType,
        imageValue = card.imageValue,
        modifier = modifier,
    )
}

/** Renders any card symbol (SVG, photo or emoji) at the given size. */
@Composable
fun SymbolImage(
    imageType: ImageType,
    imageValue: String,
    modifier: Modifier = Modifier,
    symbolPadding: Dp = 12.dp,
    emojiFontSize: TextUnit = 56.sp,
) {
    when (imageType) {
        ImageType.SYMBOL -> AsyncImage(
            model = "file:///android_asset/$imageValue",
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxSize()
                .padding(symbolPadding),
        )

        ImageType.PHOTO -> {
            val model = remember(imageValue) {
                if (imageValue.startsWith("content://") || imageValue.startsWith("http")) {
                    imageValue
                } else {
                    File(imageValue)
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
            text = imageValue,
            fontSize = emojiFontSize,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxSize()
                .padding(8.dp),
        )
    }
}
