package com.example.basicscodelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    terrain_: Terrain = Terrain(1024)
) {
    val terrain by remember {
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
        val idx : Int = (offset.x / boxSize.width).toInt()
        val jdx : Int = (offset.y / boxSize.height).toInt()

        for (i in (-1..rowWidth.toInt() + 1)) {
            for (j in (-1..colHeight.toInt() + 1)) {
                val boxOffset = Offset(
                    x = i.toFloat() * boxSize.width + (offset.x.rem(boxSize.width)) ,
                    y = j.toFloat() * boxSize.height + (offset.y.rem(boxSize.height))
                )
                drawRect(
                    color = terrain.getCellColor(i - idx, j - jdx),
                    topLeft = boxOffset, size = boxSize
                )
            }
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