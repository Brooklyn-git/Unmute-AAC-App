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
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.domain.model.ImageType
import java.io.File

@Composable
fun SectionImage(category: CategoryEntity, modifier: Modifier = Modifier) {
    when (category.symbolType) {
        ImageType.SYMBOL -> AsyncImage(
            model = "file:///android_asset/${category.symbolValue}",
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxSize()
                .padding(4.dp),
        )

        ImageType.PHOTO -> {
            val model = remember(category.symbolValue) {
                if (category.symbolValue.startsWith("content://") || category.symbolValue.startsWith("http")) {
                    category.symbolValue
                } else {
                    File(category.symbolValue)
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
            text = category.symbolValue,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxSize()
                .padding(4.dp),
        )
    }
}
