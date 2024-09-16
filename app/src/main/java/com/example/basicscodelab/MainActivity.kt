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
import androidx.compose.ui.util.lerp
import com.example.basicscodelab.ui.theme.BasicsCodelabTheme
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
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

fun diagonal(r: Float) : Offset {
    return Offset(r, r)
}

@Composable
fun MyApp(
    modifier: Modifier = Modifier,
) {
    val terrain by remember {
        mutableStateOf(Terrain(124))
    }
    var offset by remember {
        mutableStateOf(Offset(0f,0f))
    }
    val defaultTileSize = Size(100f, 100f)
    var zoom by remember {
        mutableFloatStateOf(1.0f)
    }
    val tileSize = defaultTileSize * zoom
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
        val rowWidth = size.width / tileSize.width
        val colHeight = size.height / tileSize.height
        val iOffset : Int = (offset.x / tileSize.width).toInt()
        val jOffset : Int = (offset.y / tileSize.height).toInt()
        drawRect(color = Color.Black, size = size)
        for (i in (-1..rowWidth.toInt() + 1)) {
            val idx = i + iOffset
            for (j in (-1..colHeight.toInt() + 1)) {
                val jdx = j + jOffset
                val boxOffset = Offset(
                    x = i.toFloat() * tileSize.width - (offset.x.rem(tileSize.width)) ,
                    y = j.toFloat() * tileSize.height - (offset.y.rem(tileSize.height))
                )
                val color = if (idx * jdx != 0) terrain.getCellColor(idx, jdx) else Color.Black
                drawRect(color = color, topLeft = boxOffset, size = tileSize * 0.9f)
            }
        }
    }
}

// a color gradient from black, to pure green, to white.
private fun altitudeToColor(altitude: Float): Color {
    val green = restrictToUnitInterval(altitude * 2)
    val nonGreen = restrictToUnitInterval((altitude - 0.5f) * 2)
    return Color(red = nonGreen, green = green, blue = nonGreen)
}

fun restrictToUnitInterval (x : Float) : Float {
    return min(1f, max(0f, x))
}

// a fixed mod/rem function to handle negative numbers correctly.
fun wrapAround (i : Int, size : Int) : Int {
    return if (i >= 0) i % size else size + (i % size)
}

class Terrain (val size : Int,
    ) {

    @OptIn(ExperimentalUnsignedTypes::class)
    private val altitudes: FloatArray


    init {
        val noise = perlinNoiseFractal2D(
            size = size,
            // only the relative amplitudes mater because we are going to rescale the whole thing
            //      when we convert it to ints between 0 and 255
            amplitudesAndPeriods = listOf (
                Pair(1f, diagonal(size * 3f)),
                Pair(0.5f, diagonal(size/2.1f)),
                Pair(0.25f, diagonal(size/5.1f)),
            ))
        val maximum : Float = noise.maxOfOrNull { x -> x.max() } ?: 1f
        val minimum : Float = noise.minOfOrNull { x -> x.min() } ?: 0f
        val scale : Float = 1f / (maximum - minimum)
        altitudes = FloatArray(size * size) { i ->
            val ret = (noise[i / size][i % size] - minimum) * scale
            if (ret in 0.0..1.0) ret else error("Terrain generation failed")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Terrain

        if (size != other.size) return false
        if (!altitudes.contentEquals(other.altitudes)) return false

        return true
    }

    fun getCell (x_ : Int, y_ : Int) : Float {
        val x = wrapAround(x_, size)
        val y = wrapAround(y_, size)
        return altitudes[x * size + y]
    }

    fun getCellColor (x : Int, y : Int) : Color {
        return altitudeToColor(getCell(x, y))
    }

    override fun hashCode(): Int {
        return 31 * size + altitudes.contentHashCode()
    }
}

// combine "perlinNoise"s of listed amplitudes and scales, to generate noise at multiple scales.
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

// Creates an array of floats, randomly varying in swells/dips of either amplitude height
// and period period, or of half that size in both directions.
// See http://web.archive.org/web/20240913053357/https://gpfault.net/posts/perlin-noise.txt.html
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

// Using this as the input to the last argument of lerp makes sure the first three derivatives are continuous, i.e. that the result looks smooth.
private fun fade (t : Float ) : Float {
    return t*t*t*(t*(t*6.0f - 15.0f) + 10.0f)
}

fun perlinNoiseFractal2D (size : Int, amplitudesAndPeriods : List<Pair<Float, Offset>> ) : Array<FloatArray> {
    val ret = Array(size){FloatArray(size, init = {0f})}
    for (p in amplitudesAndPeriods) {
        val noise : Array<FloatArray> = perlinNoise2D (size, p.first, p.second)
        for (i in ret.indices) {
            for (j in ret[i].indices) {
                ret[i][j] += noise[i][j]
            }
        }
    }
    return ret
}

fun perlinNoise2D  (size : Int, amplitude : Float, period : Offset) : Array<FloatArray> {
    val ret : Array<FloatArray> = Array(size, init = {FloatArray(size)})
    // The "+2" is because we need an extra node at each end so that every data point is between two nodes.
    val maxNodeIndexI = (size / period.x).toInt() + 2
    val maxNodeIndexJ = (size / period.y).toInt() + 2
    val nodes : Array<Array<Offset>> = Array (maxNodeIndexI + 1,
        init = {Array(maxNodeIndexJ + 1, init = {randomDirection2D()})})
    for (i in ret.indices) {
        val pIndexI = (i / period.x).toInt()
        for (j in ret[i].indices) {
            val pIndexJ = (j / period.y).toInt()
            val gradients : TwoByTwo<Offset> = TwoByTwo(
                topLeft     = nodes[pIndexI][pIndexJ],
                topRight    = nodes[pIndexI + 1][pIndexJ],
                bottomLeft  = nodes[pIndexI][pIndexJ + 1],
                bottomRight = nodes[pIndexI + 1][pIndexJ + 1])
            val fraction = Offset((i / period.x).rem(1f), (j / period.y).rem(1f))
            ret[i][j] = perlinInterpolation2D(gradients, fraction) * amplitude
        }
    }
    return ret
}

// returns a unit length Offset in a random direction
fun randomDirection2D () : Offset {
    val theta = Random.nextFloat() * 2 * Math.PI
    return Offset(sin(theta).toFloat(), cos(theta).toFloat())
}

// generate a smoothly varying function with the given gradients at the 4 bounding points.
fun perlinInterpolation2D (gradients : TwoByTwo<Offset>, fraction : Offset) : Float {
    val fadeHorizontal : Float = fade(fraction.x)
    val fadeVertical : Float = fade(fraction.y)
    val topLine = lerp (gradients[0, 0].dot(fraction), gradients[1, 0].dot(fraction), fadeHorizontal)
    val bottomLine = lerp (gradients[0, 0].dot(fraction), gradients[1, 0].dot(fraction), fadeHorizontal)

    return lerp (topLine, bottomLine, fadeVertical)
}

private fun Offset.dot(other : Offset): Float {
    return x * other.x + y * other.y
}

data class TwoByTwo<T> (val topLeft : T, val topRight : T, val bottomLeft : T, val bottomRight : T) {
    operator fun get (x : Boolean, y : Boolean) : T {
        return if (x)
            if (y) bottomRight else topRight
        else if (y) bottomLeft else topLeft
    }
    operator fun get(x : Int, y : Int) : T {
        return get(x == 1, y == 1)
    }
}
