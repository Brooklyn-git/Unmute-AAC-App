package com.unmute.app.ui.board

import androidx.compose.ui.unit.sp
import com.unmute.app.domain.model.CardFontSize
import org.junit.Assert.assertEquals
import org.junit.Test

class CardSizingTest {

    @Test
    fun `maps every card font size to its label text size`() {
        assertEquals(12.sp, CardFontSize.EXTRA_SMALL.labelTextSize)
        assertEquals(14.sp, CardFontSize.SMALL.labelTextSize)
        assertEquals(16.sp, CardFontSize.NORMAL.labelTextSize)
        assertEquals(20.sp, CardFontSize.LARGE.labelTextSize)
        assertEquals(24.sp, CardFontSize.EXTRA_LARGE.labelTextSize)
    }
}
