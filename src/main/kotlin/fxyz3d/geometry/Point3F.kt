package fxyz3d.geometry

import javafx.geometry.Point3D
import java.util.stream.DoubleStream

class Point3F(x: Float, y: Float, z: Float, var f: Float = 0F) : Point3D(x.toDouble(), y.toDouble(), z.toDouble()) {

    fun getCoordinates(): DoubleStream = DoubleStream.of(x, y, z)

    fun getCoordinates(factor: Float): DoubleStream = DoubleStream.of(factor * x, factor * y, factor * z)

    fun substract(point: Point3F) = substract(point.x.toFloat(), point.y.toFloat(), point.z.toFloat())

    fun substract(x: Float, y: Float, z: Float) = Point3F(this.x.toFloat() - x, this.y.toFloat() - y, this.z.toFloat() - z)

    override fun add(point: Point3D): Point3F {
        return Point3F((x + point.x).toFloat(), (y + point.y).toFloat(), (z + point.z).toFloat())
    }

    override fun multiply(factor: Double): Point3F {
        return Point3F((x * factor).toFloat(), (y * factor).toFloat(), (z * factor).toFloat())
    }
}