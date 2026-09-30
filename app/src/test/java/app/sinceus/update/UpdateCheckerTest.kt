package app.sinceus.update

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdateCheckerTest {
    @Test
    fun comparesVersionsNumerically() {
        assertTrue(UpdateChecker.isNewer("2.10.0", "2.9.3"))
        assertTrue(UpdateChecker.isNewer("2.0.1", "2.0"))
        assertTrue(UpdateChecker.isNewer("3", "2.99"))
        assertFalse(UpdateChecker.isNewer("2.0.1", "2.0.1"))
        assertFalse(UpdateChecker.isNewer("2.0", "2.0.1"))
        assertFalse(UpdateChecker.isNewer("1.9", "2.0"))
    }

    @Test
    fun findsApkInRelease() {
        val apk = "https://github.com/DerDieDasLian/sinceus/releases/download/v2.3.0/SinceUs-v2.3.0.apk"
        val release = UpdateChecker.parse(
            JSONObject(
                """{"tag_name":"v2.3.0","html_url":"https://github.com/DerDieDasLian/sinceus/releases/tag/v2.3.0",
                "assets":[{"browser_download_url":"https://github.com/DerDieDasLian/sinceus/releases/download/v2.3.0/notes.txt"},
                {"browser_download_url":"$apk"}]}""",
            ),
        )
        assertEquals("2.3.0", release.version)
        assertEquals(apk, release.apkUrl)
    }

    @Test
    fun ignoresForeignOrMissingApk() {
        val release = UpdateChecker.parse(
            JSONObject(
                """{"tag_name":"v2.3.0","html_url":"https://example.org",
                "assets":[{"browser_download_url":"https://example.org/releases/download/SinceUs.apk"}]}""",
            ),
        )
        assertNull(release.apkUrl)
        assertNull(UpdateChecker.parse(JSONObject("""{"tag_name":"v1","html_url":"x"}""")).apkUrl)
        assertFalse(UpdateInstaller.isAllowed("https://github.com/someone/else/releases/download/v1/a.apk"))
    }
}
