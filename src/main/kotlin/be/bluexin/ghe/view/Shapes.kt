package be.bluexin.ghe.view

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LocalShapes = staticCompositionLocalOf { Shapes() }
val AppShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalShapes.current

class Shapes {
    val roundStart = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
    val roundEnd = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
}