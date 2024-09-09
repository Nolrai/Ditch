package com.example.basicscodelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.example.basicscodelab.ui.theme.BasicsCodelabTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KProperty

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
    terrain_: Terrain = Terrain(1024)
) {
    var terrain by remember {
        mutableStateOf(terrain_)
    }
    var offset by remember {
        mutableStateOf(Offset(0f,0f))
    }
    val boxSize = Size (200f, 200f)
    Canvas (
        modifier = modifier.fillMaxSize().pointerInput(Unit){
            detectDragGestures { change, dragAmount ->
                change.consume()
                offset += dragAmount
            }
        }
    ) {
    val rowWidth = size.width / boxSize.width
    val colHeight = size.height / boxSize.height
    val x = offset.x / boxSize.width
    val y = offset.y / boxSize.height

    for (i in (0..rowWidth.toInt()+1))
        for (j in (0 .. colHeight.toInt() + 1)) {
            drawRect(
                color = terrain.getCellColor(i - x.toInt(), j - y.toInt()),
                topLeft = Offset(
                    x = i.toFloat() * boxSize.width - x,
                    y = j.toFloat() * boxSize.height - x
                ),
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

fun wrapArround (i : Int, size : Int) : Int {
    return (i % size) + if (i < 0) size else 0
}

data class Terrain (val size : Int) {
    private val altitudes : Array<IntArray> = Array(size) {
        IntArray(size) {
            (0..255).random()
        }
    }

    fun getCell (x_ : Int, y_ : Int) : Int {
        val x = wrapArround(x_, size)
        val y = wrapArround(y_, size)
        return altitudes[x][y]
    }

    fun getCellColor (x : Int, y : Int) : Color {
        return altitudeToColor(getCell(x, y))
    }
}