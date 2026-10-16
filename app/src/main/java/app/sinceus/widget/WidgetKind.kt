package app.sinceus.widget

import androidx.annotation.DrawableRes
import app.sinceus.R
import app.sinceus.data.RelationKind

/** Kleines Symbol zwischen den Namen: Herz, Menschen oder Haus */
@get:DrawableRes
val RelationKind.widgetSmall: Int
    get() = when (this) {
        RelationKind.LOVE -> R.drawable.ic_heart_small
        RelationKind.FRIENDS -> R.drawable.ic_friends_small
        RelationKind.FAMILY -> R.drawable.ic_family_small
    }

/** Großes, kaum sichtbares Symbol unten rechts auf der Karte */
@get:DrawableRes
val RelationKind.widgetDeco: Int
    get() = when (this) {
        RelationKind.LOVE -> R.drawable.widget_heart_deco
        RelationKind.FRIENDS -> R.drawable.widget_friends_deco
        RelationKind.FAMILY -> R.drawable.widget_family_deco
    }
