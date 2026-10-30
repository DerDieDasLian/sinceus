package app.sinceus.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.PedalBike
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector
import app.sinceus.R
import app.sinceus.data.Travel

/** Name des Verkehrsmittels für die Auswahl, z. B. „Zug“ */
@get:StringRes
val Travel.title: Int
    get() = when (this) {
        Travel.PLANE -> R.string.travel_plane
        Travel.TRAIN -> R.string.travel_train
        Travel.CAR -> R.string.travel_car
        Travel.BUS -> R.string.travel_bus
        Travel.MOTORCYCLE -> R.string.travel_motorcycle
        Travel.BIKE -> R.string.travel_bike
        Travel.BOAT -> R.string.travel_boat
        Travel.LUGGAGE -> R.string.travel_luggage
    }

/** Symbol der Fernbeziehung auf der Startseite und in den Einstellungen */
val Travel.icon: ImageVector
    get() = when (this) {
        Travel.PLANE -> Icons.Rounded.Flight
        Travel.TRAIN -> Icons.Rounded.Train
        Travel.CAR -> Icons.Rounded.DirectionsCar
        Travel.BUS -> Icons.Rounded.DirectionsBus
        Travel.MOTORCYCLE -> Icons.Rounded.TwoWheeler
        Travel.BIKE -> Icons.Rounded.PedalBike
        Travel.BOAT -> Icons.Rounded.DirectionsBoat
        Travel.LUGGAGE -> Icons.Rounded.Luggage
    }
