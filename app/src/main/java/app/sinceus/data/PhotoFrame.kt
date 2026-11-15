package app.sinceus.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Gewählter Bildausschnitt eines Moment-Fotos (Issue #49), gleiche Logik wie beim Paarfoto:
 * [x] und [y] von -1 (links/oben) bis 1 (rechts/unten), [zoom] ab 1.
 * Gilt für jedes Seitenverhältnis: Zeitleiste, Fotobuch, Teilen-Bild, Rückblick und Widget.
 */
data class PhotoFrame(val x: Float = 0f, val y: Float = 0f, val zoom: Float = 1f) {
    /** Werte außerhalb des erlaubten Bereichs (z. B. aus fremden Dateien) zurückholen */
    fun clean(): PhotoFrame = PhotoFrame(
        x.takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f,
        y.takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f,
        zoom.takeIf { it.isFinite() }?.coerceIn(1f, MAX_ZOOM) ?: 1f,
    )

    companion object {
        const val MAX_ZOOM = 4f

        /** Mittig, ohne Zoom: so sahen Moment-Fotos bisher aus */
        val CENTER = PhotoFrame()

        /**
         * Speicherform: eine Liste passend zur Reihenfolge der Fotos, null = mittig.
         * So bleiben die Ausschnitte auch erhalten, wenn Backup oder Abgleich die Pfade ändern.
         */
        fun encode(photos: List<String>, frames: Map<String, PhotoFrame>): JSONArray? {
            if (photos.none { frames[it] != null }) return null
            return JSONArray().apply {
                photos.forEach { path ->
                    val f = frames[path]
                    put(if (f == null) JSONObject.NULL else JSONObject().put("x", f.x.toDouble()).put("y", f.y.toDouble()).put("z", f.zoom.toDouble()))
                }
            }
        }

        fun decode(photos: List<String>, array: JSONArray?): Map<String, PhotoFrame> {
            if (array == null) return emptyMap()
            return photos.withIndex().mapNotNull { (i, path) ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                path to PhotoFrame(
                    o.optDouble("x", 0.0).toFloat(),
                    o.optDouble("y", 0.0).toFloat(),
                    o.optDouble("z", 1.0).toFloat(),
                ).clean()
            }.filter { it.second != CENTER }.toMap()
        }

        /** Für den Zwischenstand im Editor (überlebt Drehen des Handys): Pfad und Ausschnitt als Text */
        fun encodeMap(frames: Map<String, PhotoFrame>): String {
            val keys = frames.keys.toList()
            return JSONObject().put("p", JSONArray(keys)).put("f", encode(keys, frames) ?: JSONArray()).toString()
        }

        fun decodeMap(text: String?): Map<String, PhotoFrame> = runCatching {
            val o = JSONObject(text ?: return emptyMap())
            val keys = o.getJSONArray("p").let { a -> (0 until a.length()).map { a.getString(it) } }
            decode(keys, o.optJSONArray("f"))
        }.getOrDefault(emptyMap())
    }
}
