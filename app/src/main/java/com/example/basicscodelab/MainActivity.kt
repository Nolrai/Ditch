package com.example.basicscodelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import kotlin.random.Random

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
) {
    val terrain by remember {
        val size = 1024
        val rowRaw : FloatArray = perlinNoiseFractal(size,
                listOf(
                    Pair(1f, size*3f),
                    Pair(3f/4f, size*1f),
                    Pair(1f/4f, size/2f),
                    Pair(1/5f, size/25f)
        ))
        val row = IntArray(size) {i -> ((rowRaw[i] - rowRaw.min())/(rowRaw.max() - rowRaw.min()) * 255).toInt()}
        val t = Terrain(size = 1024, Array(size){row})
        mutableStateOf(t)
    }
    var offset by remember {
        mutableStateOf(Offset(0f,0f))
    }
    val rawBoxSize = Size(20f, 20f)
    var zoom by remember {
        mutableFloatStateOf(1.0f)
    }
    val boxSize = rawBoxSize * zoom
    Canvas (
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures(panZoomLock = true) { centroid, pan, gestureZoom, gestureRotate ->
                    val newScale = zoom * gestureZoom
                    offset -= pan
                    zoom = newScale
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
                    x = i.toFloat() * boxSize.width - (offset.x.rem(boxSize.width)) ,
                    y = j.toFloat() * boxSize.height - (offset.y.rem(boxSize.height))
                )
                drawRect(
                    color = terrain.getCellColor(i + idx, j + jdx),
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

fun wrapAround (i : Int, size : Int) : Int {
    return (i % size) + if (i < 0) size else 0
}

data class Terrain (val size : Int,
                    private val altitudes : Array<IntArray> = Array(size) { i ->
    IntArray(size) { j ->
        (0..255).random()
    }
}
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Terrain

        if (size != other.size) return false
        if (!altitudes.contentDeepEquals(other.altitudes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + altitudes.contentDeepHashCode()
        return result
    }

    fun getCell (x_ : Int, y_ : Int) : Int {
        val x = wrapAround(x_, size)
        val y = wrapAround(y_, size)
        return altitudes[x][y]
    }

    fun getCellColor (x : Int, y : Int) : Color {
        return altitudeToColor(getCell(x, y))
    }
}

fun perlinNoiseFractal (size : Int, amplitudesAndPeriods : List<Pair<Float, Float>> ) : FloatArray {
    val ret = FloatArray(size, init = {0f})
    for (p in amplitudesAndPeriods) {
        val noise : FloatArray = perlinNoise (size, p.first, p.second)
        for (i in ret.indices) {
            ret[i] += noise[i]
        }
    }
    return ret
}

fun perlinNoise (size : Int, amplitude : Float, period : Float) : FloatArray {
    var leftNode : Int = (-1..1).random()
    var rightNode : Int = (-1..1).random()
    val offset : Float = Random.nextFloat() * period
    val ret = FloatArray (size)
    for (i in ret.indices) {
        val offSetX : Float = i/period + offset
        val perlinIndex : Int = offSetX.toInt()
        val lastIndex : Int = ((i-1)/period + offset).toInt()
        val perlinFraction : Float = offSetX - perlinIndex
        // if this is a case then we have passed a node, so we need to generate a new node
        if (perlinIndex > lastIndex) {
            leftNode = rightNode
            rightNode = (-1..1).random()
        }
        ret[i] = amplitude * perlinInterpolation(leftNode, rightNode, perlinFraction)
    }
    return ret
}

// Used for generating perlin noise, generates a curve that has values of zero,
// and slopes of leftNode, rightNode at 0 and 1 respectively.
fun perlinInterpolation (leftNode : Int, rightNode : Int, p : Float) : Float {
    val p0 = p.toInt() // the index to the node before p
    val p1 = p0 + 1 // the index to the node after p
    val pFractional0 = p - p0
    val pFractional1 = p - p1
    return lerp(leftNode * pFractional0, rightNode * pFractional1, fade(pFractional0))
}

// this makes sure the first three derivatives are continuous, i.e. that the result looks smooth.
private fun fade (t : Float ) : Float {
    return t*t*t*(t*(t*6.0f - 15.0f) + 10.0f)
}

// Returns the linear interpolation between start and stop,
// lerp(start,stop, 0) = start
// lerp(start,stop, 1) = stop
// note that values above 1 or below 0 for fraction will give valid extrapolations
private fun lerp (start : Float, stop : Float, fraction : Float) : Float {
    return (1 - fraction) * start + fraction * stop
}
