/**
 * TriangleMeshHelper.java
 *
 * Copyright (c) 2013-2019, F(X)yz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of F(X)yz, any associated website, nor the
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

package fxyz3d.shapes.primitives.helper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import fxyz3d.collections.FloatCollector;
import fxyz3d.geometry.Face3;
import fxyz3d.geometry.Point3F;
import fxyz3d.scene.paint.Palette;
import fxyz3d.scene.paint.Palette.ColorPalette;
import fxyz3d.tools.NormalMap;
import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

/**
 *
 * @author jpereda
 */
public class TriangleMeshHelper {

    private double textureOpacity;

    public static enum TextureType {
        NONE, // without texture, simple colored
        IMAGE, // an image is loaded
        PATTERN, // an image from a pattern
        COLORED_FACES, // a palette is used to color faces
        COLORED_VERTICES_3D, // a palette is used to color vertices with a density map (point 3D)
        COLORED_VERTICES_1D // a palette is used to color vertices with a function map (x 1D)
    }
    public static final TextureType DEFAULT_TEXTURE_TYPE= TextureType.NONE;
    private TextureType textureType=DEFAULT_TEXTURE_TYPE;
    private boolean reverseTexture=false;

    public enum SectionType {
        CIRCLE(0),
        TRIANGLE(3),
        QUADRILATERAL(4),
        PENTAGON(5),
        HEXAGON(6),
        HEPTAGON(7),
        OCTAGON(8),
        NONAGON(9),
        DECAGON(10),
        DODECAGON(12);

        private final int sides;

        SectionType(int sides){
            this.sides=sides;
        }

        public int getSides() { return sides; }

    }

    public static final SectionType DEFAULT_SECTION_TYPE= SectionType.CIRCLE;
    private SectionType sectionType=DEFAULT_SECTION_TYPE;

    public TriangleMeshHelper(){
        material.setDiffuseColor(DEFAULT_DIFFUSE_COLOR);
        textureOpacity = 1.0;
    }

    public void setSectionType(SectionType sectionType){
        this.sectionType = sectionType;
    }
    public SectionType getSectionType() { return sectionType; }

    public void setTextureType(TextureType textureType){
        this.textureType = textureType;

        switch(textureType){
            case COLORED_FACES:
            case COLORED_VERTICES_1D:
                createPalette();
                function=DEFAULT_UNIDIM_FUNCTION;
                break;
            case COLORED_VERTICES_3D:
                createPalette();
                density=DEFAULT_DENSITY_FUNCTION;
                break;
        }
    }

    public TextureType getTextureType() { return textureType; }

    public void setTextureOpacity(double value) {
        this.textureOpacity = value;
    }

    /*
    Material
    */
    private final PhongMaterial material = new PhongMaterial();

    public final PhongMaterial getMaterial() {
        return material;
    }


    /*
    Colors, palette
    */
    public final static Color DEFAULT_DIFFUSE_COLOR = Color.WHITE;
    public final static Color DEFAULT_SPECULAR_COLOR = Color.BLACK;
    public final static int DEFAULT_COLORS = 1530;
    public final static boolean DEFAULT_SAVE_PALETTE = false;
    private Palette palette;
    private int colors;

    public final void createPalette(){
        createPalette(DEFAULT_COLORS,DEFAULT_SAVE_PALETTE, Palette.Companion.getDEFAULT_COLOR_PALETTE());
    }
    public void createPalette(int colors){
        createPalette(colors,DEFAULT_SAVE_PALETTE, Palette.Companion.getDEFAULT_COLOR_PALETTE());
    }
    public void createPalette(boolean save){
        createPalette(DEFAULT_COLORS,save, Palette.Companion.getDEFAULT_COLOR_PALETTE());
    }
    public void createPalette(int colors,boolean save){
        createPalette(DEFAULT_COLORS,save, Palette.Companion.getDEFAULT_COLOR_PALETTE());
    }
    public void createPalette(int colors, boolean save, ColorPalette palette_colors){
        this.colors=colors;
        palette = new Palette(colors, palette_colors, textureOpacity);
        palette.createPalette(save);
    }

    public Image getPaletteImage() {
        if(palette==null){
            createPalette();
        }
        return palette.getPaletteImage();
    }

    public void getMaterialWithPalette(){
        Image img = getPaletteImage();
        clearMaterialAndSetDiffMap(material, img);
    }

    public void getMaterialWithColor(Color color){
        clearMaterialAndSetColor(material, color);
    }

    public Material getMaterialWithColor(Color color, String image){
        PhongMaterial mat = new PhongMaterial(color);
        if(image!=null && !image.isEmpty()){
            Image img = new Image(image);
            mat.setDiffuseMap(img);
            NormalMap normal = new NormalMap(img);
//            normal.setIntensity(10);
//            normal.setIntensityScale(2);
            mat.setBumpMap(normal);
        }
        mat.setSpecularPower(32);
        mat.setSpecularColor(Color.WHITE);
        return mat;
    }

    public float[] getTexturePaletteArray(){
        if(palette==null){
            createPalette();
        }
        return IntStream.range(0,colors).boxed()
                .flatMapToDouble(palette::getTextureLocation)
                .collect(()->new FloatCollector(2*colors), FloatCollector::add, FloatCollector::join)
                .toArray();
    }

    private void clearMaterialAndSetDiffMap(PhongMaterial mat, Image diff){
        mat.setBumpMap(null);
        mat.setSpecularMap(null);
        mat.setSelfIlluminationMap(null);

        mat.setDiffuseColor(DEFAULT_DIFFUSE_COLOR);
        mat.setSpecularColor(DEFAULT_SPECULAR_COLOR);

        mat.setDiffuseMap(diff);
    }

    private void clearMaterialAndSetColor(PhongMaterial mat, Color col){
        mat.setBumpMap(null);
        mat.setSpecularMap(null);
        mat.setSelfIlluminationMap(null);
        mat.setDiffuseMap(null);

        mat.setDiffuseColor(col);
    }

    /*
    density functions
    */
    public final static Function<Point3F,Number> DEFAULT_DENSITY_FUNCTION= p->0d;
    private Function<Point3F,Number> density;
    private double min = 0d;
    private double max = 1d;

    public void setDensity(Function<Point3F,Number> density){
        this.density=density;
    }

    public int mapDensity(Point3F p){
        int f=(int)(((density.apply(p).doubleValue()-min)/(max-min))*colors);
        if(f<0){
            f=0;
        }
        if(f>=colors){
            f=(colors-1);
        }
        return f;
    }

    public final static Function<Number,Number> DEFAULT_UNIDIM_FUNCTION= x->0d;
    private Function<Number,Number> function;

    public void setFunction(Function<Number,Number> function){
        this.function=function;
    }

    public int mapFunction(double x){
        int f=(int)(((function.apply(x).doubleValue()-min)/(max-min))*colors);
        if(f<0){
            f=0;
        }
        if(f>=colors){
            f=colors-1;
        }
        return f;
    }

    public int mapFaces(int face, int numFaces){
        int f=(int)((((double)face)/((double)numFaces)) * colors);
        if(f<0){
            f=0;
        }
        if(f>=colors){
            f=colors-1;
        }
        return f;
    }

    public void updateExtremes(double min, double max){
        this.max=max;
        this.min=min;
        if(max==min){
            this.max=1.0+min;
        }
    }

    public void updateExtremes(List<Point3F> points){
        max=points.parallelStream().mapToDouble(p->density.apply(p).doubleValue()).max().orElse(1.0);
        min=points.parallelStream().mapToDouble(p->density.apply(p).doubleValue()).min().orElse(0.0);
        max=(float)Math.round(max*1e6)/1e6;
        min=(float)Math.round(min*1e6)/1e6;
        if(max==min){
            max=1.0+min;
        }
//        System.out.println("Min: "+min+", max: "+max);  
    }

    public void updateExtremesByFunction(List<Point3F> points){
        max=points.parallelStream().mapToDouble(p->function.apply((double) p.getF()).doubleValue()).max().orElse(1.0);
        min=points.parallelStream().mapToDouble(p->function.apply((double) p.getF()).doubleValue()).min().orElse(0.0);
        max=(float)Math.round(max*1e6)/1e6;
        min=(float)Math.round(min*1e6)/1e6;
        if(max==min){
            max=1.0+min;
        }
//        System.out.println("Min: "+min+", max: "+max);  
    }
    /*
    image
    */
    public void getMaterialWithImage(String image){
        clearMaterialAndSetDiffMap(material, new Image(image));
    }

    /*
    Mesh updating
    */
    public float[] updateVertices(List<Point3F> points){
        return points.stream()
                .flatMapToDouble(Point3F::getCoordinates)
                .collect(()->new FloatCollector(points.size()*3), FloatCollector::add, FloatCollector::join)
                .toArray();
    }

    public float[] updateVertices(List<Point3F> points, float factor){
        return points.stream()
                .flatMapToDouble(p->p.getCoordinates(factor))
                .collect(()->new FloatCollector(points.size()*3), FloatCollector::add, FloatCollector::join)
                .toArray();
    }

    public float[] createTexCoords(int width, int height){
        reverseTexture=false;
        int index=0;
        float[] textureCoords = new float[(width+1)*(height+1)*2];
        for (int v = 0; v <= height; v++) {
            float dv = (float) v / ((float)(height));
            for (int u = 0; u <= width; u++) {
                textureCoords[index] = (float) u /((float)(width));
                textureCoords[index + 1] = dv;
                index+=2;
            }
        }
        return textureCoords;
    }

    public float[] createReverseTexCoords(int width, int height){
        reverseTexture=true;
        int index=0;
        float[] textureCoords = new float[(width+1)*(height+1)*2];
        for (int x = 0; x <= width; x++) {
            float dx = (float) x /((float)(width));
            for (int y = 0; y <= height; y++) {
                float dy = (float) y / ((float)(height));
                textureCoords[index] = dx;
                textureCoords[index + 1] = dy;
                index+=2;
            }
        }
        return textureCoords;
    }

    public int[] updateFacesWithoutTexture(List<Face3> faces){
        return faces.parallelStream().map(Face3::getFace).flatMapToInt(i->i).toArray();
    }

    public int[] updateFacesWithVertices(List<Face3> faces){
        return faces.parallelStream().map(f->f.getFace(f)).flatMapToInt(i->i).toArray();
    }

    public int[] updateFacesWithTextures(List<Face3> faces, List<Face3> textures){
        if(faces.size()>textures.size()){
            return null;
        }
        AtomicInteger count=new AtomicInteger();
        return faces.stream().map(f->f.getFace(textures.get(count.getAndIncrement()))).flatMapToInt(i->i).toArray();
    }

    public int[] updateFacesWithDensityMap(List<Point3F> points, List<Face3> faces){
        updateExtremes(points);
        return faces.parallelStream().map(f->{
            int t0=mapDensity(points.get(f.p0));
            int t1=mapDensity(points.get(f.p1));
            int t2=mapDensity(points.get(f.p2));
            return f.getFace(t0, t1, t2);
        }).flatMapToInt(i->i).toArray();
    }

    public int[] updateFacesWithDensityMap(List<Point3F> points, List<Face3> faces, double min, double max){
        updateExtremes(min, max);
        return faces.parallelStream().map(f->{
            int t0=mapDensity(points.get(f.p0));
            int t1=mapDensity(points.get(f.p1));
            int t2=mapDensity(points.get(f.p2));
            return f.getFace(t0, t1, t2);
        }).flatMapToInt(i->i).toArray();
    }

    public int[] updateFacesWithFunctionMap(List<Point3F> points, List<Face3> faces){
        updateExtremesByFunction(points);
        return faces.parallelStream().map(f->{
            int t0=mapFunction(points.get(f.p0).getF());
            int t1=mapFunction(points.get(f.p1).getF());
            int t2=mapFunction(points.get(f.p2).getF());
            return f.getFace(t0, t1, t2);
        }).flatMapToInt(i->i).toArray();
    }
    public int[] updateFacesWithFunctionMap(List<Point3F> points, List<Face3> faces, double min, double max){
        updateExtremes(min, max);
        return faces.parallelStream().map(f->{
            int t0=mapFunction(points.get(f.p0).getF());
            int t1=mapFunction(points.get(f.p1).getF());
            int t2=mapFunction(points.get(f.p2).getF());
            return f.getFace(t0, t1, t2);
        }).flatMapToInt(i->i).toArray();
    }
    public int[] updateFacesWithFaces(List<Face3> faces){
        AtomicInteger count=new AtomicInteger();
        return faces.stream().map(f->f.getFace(mapFaces(count.getAndIncrement(),faces.size()))).flatMapToInt(i->i).toArray();
    }

    /*
    utils
    */
    public double getMeshArea(List<Point3F> points, List<Face3> faces){
        return faces.parallelStream().mapToDouble(f->{
            Point3F a = points.get(f.p0);
            Point3F b = points.get(f.p1);
            Point3F c = points.get(f.p2);
            return b.substract(a).crossProduct((c.substract(a))).magnitude()/2.0;
        }).reduce(Double::sum).getAsDouble();
    }

    private final float EPS = 0.000001f;
    /*
    Based on Fast, Minimum Storage Ray/Triangle Intersection
    Tomas Möller & Ben Trumbore
    http://www.graphics.cornell.edu/pubs/1997/MT97.pdf

    * origin and direction of a ray, in local coordinates of the shape, 
      to avoid transformation all the triangles to scene coordinates
      
    * This sets the texture of every face: 0 without intersection, 1 intersected
    */
    public int[] updateFacesWithIntersections(Point3F origin, Point3F direction,List<Point3F> points, List<Face3> faces){
        return faces.parallelStream().map(f->{
            Point3F a = points.get(f.p0);
            Point3F b = points.get(f.p1);
            Point3F c = points.get(f.p2);

            Point3F edge1 = b.substract(a);
            Point3F edge2 = c.substract(a);
            Point3D pvec=direction.crossProduct(edge2);
            double det=edge1.dotProduct(pvec);

            int t0=0;
            if(det<=-EPS || det>=EPS){
                double inv_det=1f/det;
                Point3F tvec=origin.substract(a);
                double u = tvec.dotProduct(pvec)*inv_det;
                if(u>=0f && u<=1f){
                    Point3D qvec=tvec.crossProduct(edge1);
                    double v = direction.dotProduct(qvec)*inv_det;
                    if(v>=0 && u+v<=1f){
//                            float t = c.dotProduct(qvec)*inv_det;
                        t0=6;
//                            System.out.println("t: "+t+", u: "+u+", v: "+v+" (a: "+a+", b: "+b+", c: "+c+")");
                    }
                }
            }
            return f.getFace(t0);
        }).flatMapToInt(i->i).toArray();
    }

    /*
    Return a list of interesected faces (with their 3 vertices)
    */
    public List<Face3> getListIntersections(Point3F origin, Point3F direction,List<Point3F> points, List<Face3> faces){
        return faces.parallelStream().filter(f->{
            Point3F a = points.get(f.p0);
            Point3F b = points.get(f.p1);
            Point3F c = points.get(f.p2);

            Point3F edge1 = b.substract(a);
            Point3F edge2 = c.substract(a);
            Point3D pvec=direction.crossProduct(edge2);
            double det=edge1.dotProduct(pvec);

            if(det<=-EPS || det>=EPS){
                double inv_det=1f/det;
                Point3F tvec=origin.substract(a);
                double u = tvec.dotProduct(pvec)*inv_det;
                if(u>=0f && u<=1f){
                    Point3D qvec=tvec.crossProduct(edge1);
                    double v = direction.dotProduct(qvec)*inv_det;
                    if(v>=0 && u+v<=1f){
                        return true;
                    }
                }
            }
            return false;
        }).collect(Collectors.toList());
    }
}