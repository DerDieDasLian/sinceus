package app.sinceus.data

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Kopplung zweier Handys für den Abgleich: ein gemeinsamer geheimer Schlüssel, der einmal per
 * QR-Code übertragen wird. Er liegt nur auf den beiden Handys (nicht in Sicherungen) und
 * verschlüsselt jede Abgleich-Datei.
 */
data class Pairing(
    /** 32 zufällige Bytes */
    val key: ByteArray,
    /** Wer auf diesem Handy die App nutzt (ID einer [Person]), null = nicht angegeben */
    val me: String?,
    /** Zeitpunkt der Kopplung in Millisekunden */
    val since: Long,
    /** Letzter erfolgreicher Abgleich in Millisekunden, 0 = noch keiner */
    val lastSync: Long = 0,
) {
    /** Kurzer Fingerabdruck des Schlüssels, steht unverschlüsselt in jeder Abgleich-Datei */
    val fingerprint: ByteArray get() = SyncCrypto.fingerprint(key)

    override fun equals(other: Any?) = other is Pairing && key.contentEquals(other.key) &&
        me == other.me && since == other.since && lastSync == other.lastSync

    override fun hashCode() = key.contentHashCode() * 31 + (me?.hashCode() ?: 0)

    companion object {
        private const val PREFIX = "sinceus:pair:1:"

        fun create(me: String?): Pairing =
            Pairing(ByteArray(32).also { SecureRandom().nextBytes(it) }, me, System.currentTimeMillis())

        /** Inhalt des QR-Codes */
        fun qrText(p: Pairing): String =
            PREFIX + Base64.encodeToString(p.key, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

        /** Schlüssel aus einem gescannten QR-Code, null = kein Kopplungscode von Since Us */
        fun keyFromQr(text: String?): ByteArray? {
            if (text == null || !text.startsWith(PREFIX)) return null
            val key = runCatching { Base64.decode(text.removePrefix(PREFIX), Base64.URL_SAFE) }.getOrNull()
            return key?.takeIf { it.size == 32 }
        }
    }
}

/** Speichert die Kopplung in eigenen Einstellungen, die bewusst nicht gesichert werden */
object PairingStore {
    private const val PREFS = "pairing"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): Pairing? {
        val p = prefs(context)
        val key = p.getString("key", null)
            ?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
            ?.takeIf { it.size == 32 } ?: return null
        return Pairing(key, p.getString("me", null), p.getLong("since", 0), p.getLong("last_sync", 0))
    }

    fun save(context: Context, pairing: Pairing) {
        prefs(context).edit()
            .putString("key", Base64.encodeToString(pairing.key, Base64.NO_WRAP))
            .putString("me", pairing.me)
            .putLong("since", pairing.since)
            .putLong("last_sync", pairing.lastSync)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

/**
 * Verschlüsselung der Abgleich-Datei mit AES-256-GCM in Blöcken, damit auch große Dateien mit
 * vielen Fotos nicht komplett in den Speicher müssen.
 *
 * Aufbau: MAGIC, 8 Bytes Fingerabdruck, 8 Bytes zufälliger Nonce-Anfang, dann Blöcke aus
 * Länge (4 Bytes) und verschlüsseltem Inhalt. Die Nonce jedes Blocks ist Nonce-Anfang plus
 * Blocknummer; ob ein Block der letzte ist, fließt mit in die Prüfung ein. So fällt jede
 * Veränderung, Vertauschung oder abgeschnittene Datei auf.
 */
object SyncCrypto {
    private val MAGIC = "SINCEUS-SYNC1".toByteArray(Charsets.US_ASCII)
    private const val CHUNK = 64 * 1024
    private const val TAG_BITS = 128
    private const val MAX_BLOCK = CHUNK + 16

    fun fingerprint(key: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest("sinceus-pair".toByteArray() + key).copyOf(8)

    /** true, wenn [header] (die ersten Bytes einer Datei) zu einer Abgleich-Datei mit diesem Schlüssel gehört */
    fun belongsTo(header: ByteArray, key: ByteArray): Boolean =
        header.size >= MAGIC.size + 8 &&
            header.copyOfRange(0, MAGIC.size).contentEquals(MAGIC) &&
            header.copyOfRange(MAGIC.size, MAGIC.size + 8).contentEquals(fingerprint(key))

    fun encrypt(input: InputStream, output: OutputStream, key: ByteArray) {
        val out = DataOutputStream(output)
        val prefix = ByteArray(8).also { SecureRandom().nextBytes(it) }
        out.write(MAGIC)
        out.write(fingerprint(key))
        out.write(prefix)
        val buffer = ByteArray(CHUNK)
        var counter = 0
        var current = readFully(input, buffer)
        while (true) {
            // Einen Block vorauslesen, um zu wissen, ob dieser der letzte ist
            val next = ByteArray(CHUNK)
            val nextLength = if (current < CHUNK) 0 else readFully(input, next)
            val last = nextLength == 0
            val sealed = cipher(Cipher.ENCRYPT_MODE, key, prefix, counter, last).doFinal(buffer, 0, current)
            out.writeInt(sealed.size)
            out.write(sealed)
            if (last) break
            System.arraycopy(next, 0, buffer, 0, nextLength)
            current = nextLength
            counter++
        }
        out.flush()
    }

    /** Entschlüsselt nach [output]; wirft eine Exception bei falschem Schlüssel oder veränderter Datei */
    fun decrypt(input: InputStream, output: OutputStream, key: ByteArray) {
        val data = DataInputStream(input)
        val header = ByteArray(MAGIC.size + 8)
        data.readFully(header)
        if (!belongsTo(header, key)) throw IOException("not paired with this key")
        val prefix = ByteArray(8)
        data.readFully(prefix)
        var counter = 0
        while (true) {
            val length = try {
                data.readInt()
            } catch (_: EOFException) {
                throw IOException("file truncated")
            }
            if (length !in 16..MAX_BLOCK) throw IOException("bad block")
            val block = ByteArray(length)
            data.readFully(block)
            // Erst als „nicht letzter Block“ prüfen, sonst als letzter
            val plain = runCatching { cipher(Cipher.DECRYPT_MODE, key, prefix, counter, false).doFinal(block) }.getOrNull()
            if (plain != null) {
                output.write(plain)
                counter++
                continue
            }
            output.write(cipher(Cipher.DECRYPT_MODE, key, prefix, counter, true).doFinal(block))
            if (data.read() != -1) throw IOException("data after last block")
            return
        }
    }

    private fun cipher(mode: Int, key: ByteArray, prefix: ByteArray, counter: Int, last: Boolean): Cipher {
        val nonce = ByteBuffer.allocate(12).put(prefix).putInt(counter).array()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(byteArrayOf(if (last) 1 else 0))
        }
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val n = input.read(buffer, total, buffer.size - total)
            if (n < 0) break
            total += n
        }
        return total
    }
}

/** Was ein Handy dem anderen schickt: Menschen, Beziehungen und Momente samt gelöschter Momente */
data class SyncData(
    val people: List<Person>,
    val relationships: List<Relationship>,
    val moments: List<Moment>,
    /** ID gelöschter Momente mit Zeitpunkt des Löschens */
    val deleted: Map<String, Long>,
    /** Wer die Datei geschickt hat (ID einer [Person]) */
    val from: String?,
)

object SyncCodec {
    private const val APP = "app.sinceus"
    const val FORMAT = 1
    const val JSON = "sync.json"
    const val MIME = "application/vnd.sinceus.sync"
    const val EXTENSION = "sinceus"

    fun fileName(today: java.time.LocalDate) = "since-us-$today.$EXTENSION"

    fun encode(d: SyncData): String = JSONObject()
        .put("app", APP)
        .put("format", FORMAT)
        .put("from", d.from ?: JSONObject.NULL)
        .put("people", JSONArray(PeopleCodec.encodePeople(d.people)))
        .put("relationships", JSONArray(PeopleCodec.encodeRelationships(d.relationships)))
        .put(
            "moments",
            JSONArray(MomentCodec.encode(d.moments.map { m -> m.copy(photoPath = m.photoPath?.let { java.io.File(it).name }) })),
        )
        .put("deleted", JSONObject().apply { d.deleted.forEach { (id, time) -> put(id, time) } })
        .toString()

    fun decode(json: String): SyncData {
        val o = JSONObject(json)
        require(o.optString("app") == APP) { "not a Since Us sync file" }
        require(o.optInt("format", 0) in 1..FORMAT) { "unsupported sync format" }
        val deleted = o.optJSONObject("deleted")
        return SyncData(
            people = PeopleCodec.decodePeople(o.optJSONArray("people")?.toString()).orEmpty(),
            relationships = PeopleCodec.decodeRelationships(o.optJSONArray("relationships")?.toString()).orEmpty(),
            moments = MomentCodec.decode(o.optJSONArray("moments")?.toString())
                .map { m -> m.copy(photoPath = m.photoPath?.takeIf { Backup.isSafeName(it) }) },
            deleted = deleted?.keys()?.asSequence()?.associateWith { deleted.optLong(it) }.orEmpty(),
            from = if (o.isNull("from")) null else o.optString("from").ifBlank { null },
        )
    }
}

/** Ergebnis des Zusammenführens */
data class MergeResult(
    val people: List<Person>,
    val relationships: List<Relationship>,
    val moments: List<Moment>,
    val deleted: Map<String, Long>,
    /** Neue oder geänderte Momente aus der Datei (ihre Fotos müssen übernommen werden) */
    val taken: List<Moment>,
    val added: Int,
    val changed: Int,
    val removed: Int,
)

object SyncMerge {
    /**
     * Führt die eigenen Daten mit denen des anderen Handys zusammen. Bei Momenten gewinnt die
     * jüngere Änderung, gelöschte Momente bleiben gelöscht. Menschen und Beziehungen, die hier
     * noch fehlen, kommen dazu; bestehende bleiben, wie sie hier eingestellt sind.
     */
    fun merge(
        people: List<Person>,
        relationships: List<Relationship>,
        moments: List<Moment>,
        deleted: Map<String, Long>,
        remote: SyncData,
    ): MergeResult {
        val allDeleted = (deleted.keys + remote.deleted.keys).associateWith { maxOf(deleted[it] ?: 0, remote.deleted[it] ?: 0) }
        val byId = moments.associateBy { it.id }.toMutableMap()
        val taken = mutableListOf<Moment>()
        var added = 0
        var changed = 0
        remote.moments.forEach { m ->
            val gone = allDeleted[m.id]
            if (gone != null && gone >= m.updatedAt) return@forEach
            val local = byId[m.id]
            if (local == null) {
                added++
            } else if (local.updatedAt >= m.updatedAt) {
                return@forEach
            } else {
                changed++
            }
            byId[m.id] = m
            taken += m
        }
        var removed = 0
        allDeleted.forEach { (id, time) ->
            val local = byId[id]
            if (local != null && local.updatedAt <= time) {
                byId.remove(id)
                removed++
            }
        }
        // Bekannte Menschen bleiben wie hier eingetragen, nur ein fehlender Geburtstag kommt vom anderen Handy
        val mergedPeople = (
            people.map { p -> p.copy(birthday = p.birthday ?: remote.people.firstOrNull { it.id == p.id }?.birthday) } +
                remote.people.filter { r -> people.none { it.id == r.id } }
            ).take(MAX_PEOPLE)
        val mergedRelationships = (relationships + remote.relationships.filter { r -> relationships.none { it.id == r.id } })
            .take(MAX_RELATIONSHIPS)
        return MergeResult(
            people = mergedPeople,
            relationships = mergedRelationships,
            moments = byId.values.sortedBy { it.date },
            // Alte Löschmerker nach einem Jahr vergessen, damit die Liste nicht endlos wächst
            deleted = allDeleted.filterValues { it > System.currentTimeMillis() - YEAR_MS },
            taken = taken,
            added = added,
            changed = changed,
            removed = removed,
        )
    }

    private const val YEAR_MS = 365L * 24 * 3600 * 1000
}
