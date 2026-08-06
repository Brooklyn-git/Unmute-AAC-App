package com.unmute.app.tts

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OutputDedupTest {

    @Test
    fun wiredHeadsetAndHeadphonesShareOneOutputOption() {
        assertEquals(
            outputDedupKey(AudioDeviceInfo.TYPE_WIRED_HEADSET, "Headset"),
            outputDedupKey(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, "Wired headphones"),
        )
    }

    @Test
    fun usbHeadsetAndUsbDeviceShareOneOutputOption() {
        assertEquals(
            outputDedupKey(AudioDeviceInfo.TYPE_USB_HEADSET, null),
            outputDedupKey(AudioDeviceInfo.TYPE_USB_DEVICE, null),
        )
    }

    @Test
    fun bluetoothA2dpAndScoForSameDeviceShareOneOutputOption() {
        assertEquals(
            outputDedupKey(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "JBL Flip 5"),
            outputDedupKey(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, "JBL Flip 5"),
        )
    }

    @Test
    fun bluetoothDevicesWithDifferentNamesStaySeparate() {
        assertNotEquals(
            outputDedupKey(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "JBL Flip 5"),
            outputDedupKey(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "Sony WH-1000"),
        )
    }

    @Test
    fun speakerIsDistinctFromWired() {
        assertNotEquals(
            outputDedupKey(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "Speaker"),
            outputDedupKey(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, "Wired headphones"),
        )
    }
}
