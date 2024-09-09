package com.example.basicscodelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.basicscodelab.ui.theme.BasicsCodelabTheme
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ///testing
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BasicsCodelabTheme {
                MyApp(modifier = Modifier.fillMaxSize())
        }
            }
    }
}

@Composable
fun MyApp(
    modifier: Modifier = Modifier,
    terrain: Array<IntArray> = mkNewTerrain(1024)
) {
    val x = rememberSaveable() {
        mutableFloatStateOf(0f)
    }
    val y = rememberSaveable() {
        mutableFloatStateOf(0f)
    }
    val boxSize = Size (200f, 200f)
    Canvas (modifier = modifier.fillMaxSize()) {
        val rowWidth = size.width / boxSize.width
        val colHeight = size.height / boxSize.height

        val startX : Int = 0 + x.floatValue.toInt()
        val endX : Int = (rowWidth + x.floatValue).toInt() + 1
        val startY : Int = 0 + y.floatValue.toInt()
        val endY : Int = (colHeight + y.floatValue).toInt() + 1

        for (i in (startX .. endX))
            for (j in (startY .. endY)) {
                drawRect(
                    color = altitudeToColor(terrain[i][j]),
                    topLeft = Offset(
                        i.toFloat()*boxSize.width - x.floatValue,
                        j.toFloat()*boxSize.height - y.floatValue),
                    size = boxSize
                    )
            }
    }
}

private fun altitudeToColor(altitude : Int): Color {
    val nonGreen : Int = 2 * max((altitude - 128), 0)
    val green : Int = 2 * min (255, altitude + 128)
    return Color(
        green = green,
        red = nonGreen,
        blue = nonGreen
    )
}

fun mkNewTerrain(size : Int): Array<IntArray> {
    return Array(size) {
        IntArray (size) {
            (0..255).random()
        }
    }
}