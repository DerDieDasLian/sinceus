package app.sinceus.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.ui.graphics.vector.ImageVector
import app.sinceus.R
import app.sinceus.data.RelationKind

/** Name der Art für die Auswahl, z. B. „Freundschaft“ */
@get:StringRes
val RelationKind.title: Int
    get() = when (this) {
        RelationKind.LOVE -> R.string.kind_love
        RelationKind.FRIENDS -> R.string.kind_friends
        RelationKind.FAMILY -> R.string.kind_family
    }

/** Überschrift über dem Tageszähler, z. B. „Together for“ oder „Friends for“ */
@get:StringRes
val RelationKind.forLabel: Int
    get() = when (this) {
        RelationKind.LOVE -> R.string.together_for
        RelationKind.FRIENDS -> R.string.friends_for
        RelationKind.FAMILY -> R.string.family_for
    }

/** „Together since %1$s“ und die Gegenstücke für Freundschaft und Familie */
@get:StringRes
val RelationKind.sinceLong: Int
    get() = when (this) {
        RelationKind.LOVE -> R.string.together_since_long
        RelationKind.FRIENDS -> R.string.friends_since_long
        RelationKind.FAMILY -> R.string.family_since_long
    }

/** Symbol zwischen den Namen: Herz, Menschen oder Haus (wie im Widget) */
val RelationKind.icon: ImageVector
    get() = when (this) {
        RelationKind.LOVE -> Icons.Rounded.Favorite
        RelationKind.FRIENDS -> Icons.Rounded.People
        RelationKind.FAMILY -> Icons.Rounded.Home
    }
