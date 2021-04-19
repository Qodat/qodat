package stan.qodat.util

import kotlin.math.cos
import kotlin.math.sin

private const val UNIT = Math.PI.div(1024.0)

val SINE = IntArray(2048) { (65536.0 * sin(UNIT.times(it))).toInt() }
val COSINE = IntArray(2048) { (65536.0 * cos(UNIT.times(it))).toInt() }