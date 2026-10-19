package com.unmute.app.ui.board

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.unmute.app.domain.model.CardFontSize

internal val CardFontSize.labelTextSize: TextUnit
    get() = when (this) {
        CardFontSize.EXTRA_SMALL -> 12.sp
        CardFontSize.SMALL -> 14.sp
        CardFontSize.NORMAL -> 16.sp
        CardFontSize.LARGE -> 20.sp
        CardFontSize.EXTRA_LARGE -> 24.sp
    }
