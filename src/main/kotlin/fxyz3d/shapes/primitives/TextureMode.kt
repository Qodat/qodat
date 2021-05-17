/**
 * TextureMode.java
 *
 * Copyright (c) 2013-2019, F(X)yz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of F(X)yz, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL F(X)yz BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package fxyz3d.shapes.primitives

import fxyz3d.geometry.Point3F
import fxyz3d.scene.paint.ColorPalette
import fxyz3d.scene.paint.Palette
import javafx.scene.paint.Color
import java.util.function.Function

/**
 *
 * @author jpereda
 * @author Stan van der Bend
 */
interface TextureMode {
    fun setTextureModeNone()
    fun setTextureModeNone(color: Color)
    fun setTextureModeNone(color: Color, image: String)
    fun setTextureModeImage(image: String)
    fun setTextureModeVertices3D(colors: Int, dens: Function<Point3F, Number>)
    fun setTextureModeVertices3D(palette: ColorPalette, dens: Function<Point3F, Number>)
    fun setTextureModeVertices3D(colors: Int, dens: Function<Point3F, Number>, min: Double, max: Double)
    fun setTextureModeVertices1D(colors: Int, function: Function<Number, Number>)
    fun setTextureModeVertices1D(palette: ColorPalette, function: Function<Number, Number>)
    fun setTextureModeVertices1D(colors: Int, function: Function<Number, Number>, min: Double, max: Double)
    fun setTextureModeFaces(colors: Int)
    fun setTextureModeFaces(palette: ColorPalette)
    fun setTextureOpacity(value: Double)
}