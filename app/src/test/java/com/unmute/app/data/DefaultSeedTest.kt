package com.unmute.app.data

import com.unmute.app.domain.model.ImageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSeedTest {

    @Test
    fun `every preset category has an emoji symbol`() {
        DefaultSeed.categories.forEach { category ->
            assertEquals("Category ${category.nameEn} must be an emoji", ImageType.EMOJI, category.symbolType)
            assertTrue("Category ${category.nameEn} must have a symbol", category.symbolValue.isNotBlank())
        }
    }
}
