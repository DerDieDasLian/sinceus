@file:OptIn(ExperimentalTextApi::class)

package app.sinceus.ui

import androidx.annotation.StringRes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.sinceus.R

/**
 * Eine Schrift zur Auswahl in den Einstellungen: [heading] für Überschriften und die großen Zahlen,
 * [body] für den übrigen Text (null = Systemschrift). Schreibschriften wirken kleiner, daher
 * werden ihre Überschriften um [scale] vergrößert. Alle Schriften stehen unter der SIL Open Font License.
 */
class AppFont(
    val id: String,
    val name: String,
    @StringRes val group: Int,
    val heading: FontFamily,
    val body: FontFamily? = null,
    val scale: Float = 1f,
    /** Übersetzter Name statt [name], z. B. für die klassische Schrift */
    @StringRes val nameRes: Int? = null,
)

/** Anzeigename der Schrift in der gewählten Sprache */
@Composable
fun AppFont.label(): String = nameRes?.let { stringResource(it) } ?: name

object AppFonts {
    /** Schrift für alle, die noch keine eigene gewählt haben */
    const val DEFAULT_ID = "space_grotesk"

    /** Die frühere Schrift der App (Systemschrift mit Serifen-Überschriften) */
    const val CLASSIC_ID = "standard"

    private val weights = listOf(400, 500, 600, 700)

    /** Variable Schrift: eine Datei, alle Stärken über die Achse "wght" */
    private fun variable(res: Int) = FontFamily(
        weights.map { w -> Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) },
    )

    /** Schrift mit nur einer Stärke, fett wird von Android berechnet */
    private fun single(res: Int) = FontFamily(Font(res))

    private val nunito by lazy { variable(R.font.nunito) }
    private val quicksand by lazy { variable(R.font.quicksand) }

    val all: List<AppFont> by lazy {
        listOf(
            AppFont(CLASSIC_ID, "", R.string.font_group_standard, FontFamily.Serif, nameRes = R.string.font_classic),
            // Rund und weich
            AppFont("quicksand", "Quicksand", R.string.font_group_round, quicksand, quicksand),
            AppFont("nunito", "Nunito", R.string.font_group_round, nunito, nunito),
            AppFont("comfortaa", "Comfortaa", R.string.font_group_round, variable(R.font.comfortaa), variable(R.font.comfortaa)),
            AppFont("fredoka", "Fredoka", R.string.font_group_round, variable(R.font.fredoka), variable(R.font.fredoka)),
            AppFont("varela_round", "Varela Round", R.string.font_group_round, single(R.font.varela_round), single(R.font.varela_round)),
            AppFont("baloo2", "Baloo 2", R.string.font_group_round, variable(R.font.baloo2), variable(R.font.baloo2)),
            AppFont("rubik", "Rubik", R.string.font_group_round, variable(R.font.rubik), variable(R.font.rubik)),
            // Modern und klar
            AppFont("funnel", "Funnel", R.string.font_group_modern, variable(R.font.funnel_display), variable(R.font.funnel_sans)),
            AppFont("outfit", "Outfit", R.string.font_group_modern, variable(R.font.outfit), variable(R.font.outfit)),
            AppFont(
                "poppins",
                "Poppins",
                R.string.font_group_modern,
                FontFamily(
                    Font(R.font.poppins_regular, FontWeight.Normal),
                    Font(R.font.poppins_semibold, FontWeight.SemiBold),
                    Font(R.font.poppins_semibold, FontWeight.Bold),
                ),
                FontFamily(Font(R.font.poppins_regular, FontWeight.Normal), Font(R.font.poppins_semibold, FontWeight.SemiBold)),
            ),
            AppFont("lexend", "Lexend", R.string.font_group_modern, variable(R.font.lexend), variable(R.font.lexend)),
            AppFont("montserrat", "Montserrat", R.string.font_group_modern, variable(R.font.montserrat), variable(R.font.montserrat)),
            AppFont("raleway", "Raleway", R.string.font_group_modern, variable(R.font.raleway), variable(R.font.raleway)),
            AppFont("josefin_sans", "Josefin Sans", R.string.font_group_modern, variable(R.font.josefin_sans), variable(R.font.josefin_sans)),
            AppFont("urbanist", "Urbanist", R.string.font_group_modern, variable(R.font.urbanist), variable(R.font.urbanist)),
            AppFont("plus_jakarta_sans", "Plus Jakarta Sans", R.string.font_group_modern, variable(R.font.plus_jakarta_sans), variable(R.font.plus_jakarta_sans)),
            AppFont("sora", "Sora", R.string.font_group_modern, variable(R.font.sora), variable(R.font.sora)),
            AppFont("space_grotesk", "Space Grotesk", R.string.font_group_modern, variable(R.font.space_grotesk), variable(R.font.space_grotesk)),
            AppFont("familjen_grotesk", "Familjen Grotesk", R.string.font_group_modern, variable(R.font.familjen_grotesk), variable(R.font.familjen_grotesk)),
            AppFont("bricolage_grotesque", "Bricolage Grotesque", R.string.font_group_modern, variable(R.font.bricolage_grotesque), variable(R.font.bricolage_grotesque)),
            AppFont("schibsted_grotesk", "Schibsted Grotesk", R.string.font_group_modern, variable(R.font.schibsted_grotesk), variable(R.font.schibsted_grotesk)),
            AppFont("host_grotesk", "Host Grotesk", R.string.font_group_modern, variable(R.font.host_grotesk), variable(R.font.host_grotesk)),
            // Edel, mit Serifen
            AppFont("fraunces", "Fraunces", R.string.font_group_serif, variable(R.font.fraunces), nunito),
            AppFont("playfair", "Playfair Display", R.string.font_group_serif, variable(R.font.playfair), variable(R.font.nunito_sans)),
            AppFont("lora", "Lora", R.string.font_group_serif, variable(R.font.lora), nunito),
            AppFont("cormorant", "Cormorant Garamond", R.string.font_group_serif, variable(R.font.cormorant_garamond), nunito, 1.1f),
            AppFont("young_serif", "Young Serif", R.string.font_group_serif, single(R.font.young_serif), nunito),
            AppFont("gloock", "Gloock", R.string.font_group_serif, single(R.font.gloock), nunito),
            AppFont("abril_fatface", "Abril Fatface", R.string.font_group_serif, single(R.font.abril_fatface), nunito),
            AppFont("italiana", "Italiana", R.string.font_group_serif, single(R.font.italiana), variable(R.font.raleway), 1.05f),
            // Schreibschrift und Handschrift
            AppFont("caveat", "Caveat", R.string.font_group_script, variable(R.font.caveat), quicksand, 1.2f),
            AppFont("pacifico", "Pacifico", R.string.font_group_script, single(R.font.pacifico), nunito),
            AppFont("dancing_script", "Dancing Script", R.string.font_group_script, variable(R.font.dancing_script), nunito, 1.1f),
            AppFont("great_vibes", "Great Vibes", R.string.font_group_script, single(R.font.great_vibes), quicksand, 1.2f),
            AppFont("sacramento", "Sacramento", R.string.font_group_script, single(R.font.sacramento), quicksand, 1.2f),
            AppFont("kalam", "Kalam", R.string.font_group_script, single(R.font.kalam), nunito),
            AppFont("patrick_hand", "Patrick Hand", R.string.font_group_script, single(R.font.patrick_hand), single(R.font.patrick_hand), 1.05f),
        )
    }

    /**
     * Schriftdateien für die Widgets (Überschrift, Text), die nur Bilder statt Compose-Schriften kennen.
     * null = Systemschrift.
     */
    private val files: Map<String, Pair<Int?, Int?>> = mapOf(
            CLASSIC_ID to (null to null),
            "quicksand" to (R.font.quicksand to R.font.quicksand),
            "nunito" to (R.font.nunito to R.font.nunito),
            "comfortaa" to (R.font.comfortaa to R.font.comfortaa),
            "fredoka" to (R.font.fredoka to R.font.fredoka),
            "varela_round" to (R.font.varela_round to R.font.varela_round),
            "baloo2" to (R.font.baloo2 to R.font.baloo2),
            "rubik" to (R.font.rubik to R.font.rubik),
            "funnel" to (R.font.funnel_display to R.font.funnel_sans),
            "outfit" to (R.font.outfit to R.font.outfit),
            "poppins" to (R.font.poppins_semibold to R.font.poppins_regular),
            "lexend" to (R.font.lexend to R.font.lexend),
            "montserrat" to (R.font.montserrat to R.font.montserrat),
            "raleway" to (R.font.raleway to R.font.raleway),
            "josefin_sans" to (R.font.josefin_sans to R.font.josefin_sans),
            "urbanist" to (R.font.urbanist to R.font.urbanist),
            "plus_jakarta_sans" to (R.font.plus_jakarta_sans to R.font.plus_jakarta_sans),
            "sora" to (R.font.sora to R.font.sora),
            "space_grotesk" to (R.font.space_grotesk to R.font.space_grotesk),
            "familjen_grotesk" to (R.font.familjen_grotesk to R.font.familjen_grotesk),
            "bricolage_grotesque" to (R.font.bricolage_grotesque to R.font.bricolage_grotesque),
            "schibsted_grotesk" to (R.font.schibsted_grotesk to R.font.schibsted_grotesk),
            "host_grotesk" to (R.font.host_grotesk to R.font.host_grotesk),
            "fraunces" to (R.font.fraunces to R.font.nunito),
            "playfair" to (R.font.playfair to R.font.nunito_sans),
            "lora" to (R.font.lora to R.font.nunito),
            "cormorant" to (R.font.cormorant_garamond to R.font.nunito),
            "young_serif" to (R.font.young_serif to R.font.nunito),
            "gloock" to (R.font.gloock to R.font.nunito),
            "abril_fatface" to (R.font.abril_fatface to R.font.nunito),
            "italiana" to (R.font.italiana to R.font.raleway),
            "caveat" to (R.font.caveat to R.font.quicksand),
            "pacifico" to (R.font.pacifico to R.font.nunito),
            "dancing_script" to (R.font.dancing_script to R.font.nunito),
            "great_vibes" to (R.font.great_vibes to R.font.quicksand),
            "sacramento" to (R.font.sacramento to R.font.quicksand),
            "kalam" to (R.font.kalam to R.font.nunito),
            "patrick_hand" to (R.font.patrick_hand to R.font.patrick_hand),
    )

    fun files(font: AppFont): Pair<Int?, Int?> = files[font.id] ?: (null to null)

    /** Die Schrift zur gespeicherten ID; leer oder unbekannt = Standard */
    fun find(id: String): AppFont {
        val wanted = id.ifEmpty { DEFAULT_ID }
        return all.firstOrNull { it.id == wanted } ?: all.first { it.id == DEFAULT_ID }
    }

    /** Material-Schriftstile mit dieser Schrift */
    fun typography(font: AppFont): Typography {
        val base = Typography()
        fun TextStyle.head(weight: FontWeight? = null) = copy(
            fontFamily = font.heading,
            fontWeight = weight ?: fontWeight,
            fontSize = fontSize * font.scale,
            lineHeight = lineHeight * font.scale,
        )
        fun TextStyle.text() = if (font.body == null) this else copy(fontFamily = font.body)
        return base.copy(
            displayLarge = base.displayLarge.head(FontWeight.SemiBold),
            displayMedium = base.displayMedium.head(FontWeight.SemiBold),
            displaySmall = base.displaySmall.head(),
            headlineLarge = base.headlineLarge.head(),
            headlineMedium = base.headlineMedium.head(),
            headlineSmall = base.headlineSmall.head(),
            titleLarge = base.titleLarge.head(),
            titleMedium = base.titleMedium.text(),
            titleSmall = base.titleSmall.text(),
            bodyLarge = base.bodyLarge.text(),
            bodyMedium = base.bodyMedium.text(),
            bodySmall = base.bodySmall.text(),
            labelLarge = base.labelLarge.text(),
            labelMedium = base.labelMedium.text(),
            labelSmall = base.labelSmall.text(),
        )
    }
}
