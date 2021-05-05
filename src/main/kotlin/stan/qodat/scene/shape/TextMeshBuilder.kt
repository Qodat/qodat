package stan.qodat.scene.shape

import fxyz3d.geometry.Point3F
import fxyz3d.shapes.primitives.helper.LineSegment
import javafx.geometry.Point2D
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.scene.text.Text
import kotlin.math.absoluteValue
import kotlin.math.pow

class TextMeshBuilder(private val text: Text) {

    private lateinit var pointList: ArrayList<Point3F>
    private lateinit var firstPoint: Point3F

    val segments = ArrayList<LineSegment>()

    init {
        val textPath = Shape.subtract(text, Rectangle(0.0, 0.0)) as Path
        extractPoints(textPath)
        val groupedSegments = segments.groupBy { it.isHole }
        val holeSegments = groupedSegments[true]?: emptyList()
        val notHoleSegments = groupedSegments[false]?: emptyList()
        for (holeSegment in holeSegments) {
            val origin = holeSegment.origen.let { Point2D(it.x, it.y) }
            notHoleSegments
                .filter { !(Shape.intersect(it.path, holeSegment.path) as Path).elements.isEmpty() }
                .filter { it.path.contains(origin) }
                .forEach { it.addHole(holeSegment) }
        }
        segments.removeAll(holeSegments)
    }

    fun getOffset() = segments
        .sortedWith { p1, p2 -> (p1.origen.x - p2.origen.x).toInt() }
        .map { it.origen }

    private fun extractPoints(textPath: Path) {
        textPath.elements.map { element ->
            when (element) {
                is MoveTo -> {
                    pointList = ArrayList()
                    firstPoint = Point3F(element.x.toFloat(), element.y.toFloat(), 0F)
                    pointList.add(firstPoint)
                }
                is LineTo -> pointList.add(Point3F(element.x.toFloat(), element.y.toFloat(), 0F))
                is CubicCurveTo,
                is QuadCurveTo -> evalBezier(element)
                is ClosePath -> {
                    pointList.add(firstPoint)
                    var double = 0.0
                    for (index in 0 until pointList.size-1) {
                        val point = pointList[index]
                        val nextPoint = pointList[index + 1]
                        double += point.crossProduct(nextPoint).z
                    }
                    val area = double.div(2.0)
                    if (area.absoluteValue > 0.001) {
                        val line = LineSegment(text.text)
                        line.isHole = area > 0
                        line.points = pointList
                        line.path = Path(MoveTo(firstPoint.x, firstPoint.y)).apply {
                            pointList.stream().skip(1).forEach {
                                elements.add(LineTo(it.x, it.y))
                            }
                            elements.add(ClosePath())
                            stroke = Color.GREEN
                            fill = Color.RED
                        }
                        line.origen = firstPoint
                        segments.add(line)
                    } else Unit
                }
                else -> throw Exception("Invalid element {$element}")
            }
        }
    }

    private fun evalBezier(element: PathElement) {
        val startPoint = pointList.lastOrNull()?:firstPoint
        return when (element) {
            is CubicCurveTo -> (1..POINTS_CURVE).forEach {
                evalCubicBezier(element, startPoint, it.div(POINTS_CURVE.toDouble()))
            }
            is QuadCurveTo -> (1..POINTS_CURVE).forEach {
                pointList.add(evalQuadBezier(element, startPoint, it.div(POINTS_CURVE.toDouble())))
            }
            else -> throw Exception("Element must be CubicCurveTo or QuadCurveTo but is $this!")
        }
    }
    private fun evalCubicBezier(c: CubicCurveTo, ini: Point3F, t: Double) = Point3F(
        ((1 - t).pow(3.0) * ini.x + 3 * t * (1 - t).pow(2.0) * c.controlX1
                + 3 * (1 - t) * t * t * c.controlX2 + t.pow(3.0) * c.x).toFloat(),
        ((1 - t).pow(3.0) * ini.y + 3 * t * (1 - t).pow(2.0) * c.controlY1
                + 3 * (1 - t) * t * t * c.controlY2 + Math.pow(t, 3.0) * c.y).toFloat(),
        0F)

    private fun evalQuadBezier(c: QuadCurveTo, ini: Point3F, t: Double) = Point3F(
        ((1 - t).pow(2.0) * ini.x + 2 * (1 - t) * t * c.controlX + t.pow(2.0) * c.x).toFloat(),
        ((1 - t).pow(2.0) * ini.y + 2 * (1 - t) * t * c.controlY + t.pow(2.0) * c.y).toFloat(),
        0F)

    companion object {
        private const val POINTS_CURVE = 10
    }
}