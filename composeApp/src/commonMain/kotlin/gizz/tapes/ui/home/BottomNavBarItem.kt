package gizz.tapes.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Festival
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import gizz.tapes.nav.Destination

enum class BottomNavBarItem(
    val label: String,
    val icon: ImageVector,
    val destination: Destination,
) {
    Home("Home", Icons.Filled.Home, Destination.Home),
    Years("Years", Icons.Filled.CalendarToday, Destination.YearSelection),
    Venues("Venues", Icons.Filled.Festival, Destination.Venues),
    Search("Search", Icons.Filled.Search, Destination.Search),
}
