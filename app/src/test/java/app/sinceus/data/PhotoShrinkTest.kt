package app.sinceus.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoShrinkTest {
    @Test
    fun bigPhotoIsScaledToMaxSide() {
        assertEquals(2048 to 1536, PhotoShrink.targetSize(4000, 3000))
        assertEquals(1536 to 2048, PhotoShrink.targetSize(3000, 4000))
    }

    @Test
    fun smallPhotoStaysTheSame() {
        assertEquals(1200 to 800, PhotoShrink.targetSize(1200, 800))
    }
}
