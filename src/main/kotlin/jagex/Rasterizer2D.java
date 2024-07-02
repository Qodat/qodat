package jagex;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.*;
import java.util.Hashtable;

public class Rasterizer2D {

    private static final ColorModel COLOR_MODEL = new DirectColorModel(32, 0xff0000, 0xff00, 0xff);

    public static float[] depthBuffer;

    public static int[] Rasterizer2D_pixels;

    public static int Rasterizer2D_width;
    public static int Rasterizer2D_height;
    public static int Rasterizer2D_yClipStart;
    public static int Rasterizer2D_yClipEnd;
    public static int Rasterizer2D_xClipStart;
    public static int Rasterizer2D_xClipEnd;
    public static int lastX;
    public static int viewportCenterX;
    public static int viewportCenterY;

    static {
        Rasterizer2D_yClipStart = 0;
        Rasterizer2D_yClipEnd = 0;
        Rasterizer2D_xClipStart = 0;
        Rasterizer2D_xClipEnd = 0;
    }

    /**
     * Sets the Rasterizer2D in the upper left corner with height, width and pixels set.
     *
     * @param pixels The array of pixels (RGBColours) in the drawingArea.
     * @param width  The width of the drawingArea.
     * @param height The height of the drawingArea.
     */
    public static void Rasterizer2D_replace(int[] pixels, float[] depth, int width, int height) {
        if(depth != null)
            depthBuffer = depth;
        Rasterizer2D_pixels = pixels;
        Rasterizer2D_width = width;
        Rasterizer2D_height = height;
        Rasterizer2D_setClip(0, height, width, 0);
    }
    public static void Rasterizer2D_clear() {
        int var0 = 0;

        int var1;
        for(var1 = Rasterizer2D_width * Rasterizer2D_height - 7; var0 < var1; Rasterizer2D_pixels[var0++] = 0) {
            Rasterizer2D_pixels[var0++] = 0;
            Rasterizer2D_pixels[var0++] = 0;
            Rasterizer2D_pixels[var0++] = 0;
            Rasterizer2D_pixels[var0++] = 0;
            Rasterizer2D_pixels[var0++] = 0;
            Rasterizer2D_pixels[var0++] = 0;
            Rasterizer2D_pixels[var0++] = 0;
        }

        for(var1 += 7; var0 < var1; Rasterizer2D_pixels[var0++] = 0) {
            ;
        }

    }
    /**
     * Sets default size and position for 2D raster
     */
    public static void Rasterizer2D_resetClip() {
        Rasterizer2D_xClipStart = 0;
        Rasterizer2D_yClipStart = 0;
        Rasterizer2D_xClipEnd = Rasterizer2D_width;
        Rasterizer2D_yClipEnd = Rasterizer2D_height;
        lastX = Rasterizer2D_xClipEnd;
        viewportCenterX = Rasterizer2D_xClipEnd / 2;
    }

    /**
     * Sets the drawingArea based on the coordinates of the edges.
     * @param leftX   The left edge X-Coordinate.
     * @param bottomY The bottom edge Y-Coordinate.
     * @param rightX  The right edge X-Coordinate.
     * @param topY    The top edge Y-Coordinate.
     */
    public static void Rasterizer2D_setClip(int leftX, int bottomY, int rightX, int topY) {
        if (leftX < 0)
            leftX = 0;

        if (topY < 0)
            topY = 0;

        if (rightX > Rasterizer2D_width)
            rightX = Rasterizer2D_width;

        if (bottomY > Rasterizer2D_height)
            bottomY = Rasterizer2D_height;

        Rasterizer2D.Rasterizer2D_xClipStart = leftX;
        Rasterizer2D.Rasterizer2D_yClipStart = topY;
        Rasterizer2D.Rasterizer2D_xClipEnd = rightX;
        Rasterizer2D.Rasterizer2D_yClipEnd = bottomY;
        lastX = Rasterizer2D.Rasterizer2D_xClipEnd;
        viewportCenterX = Rasterizer2D.Rasterizer2D_xClipEnd / 2;
        viewportCenterY = Rasterizer2D.Rasterizer2D_yClipEnd / 2;
    }

	/* Graphics2D methods */
    public static void Rasterizer2D_fillRectangle(int x, int y, int var2, int var3, int color) {
        if(x < Rasterizer2D_xClipStart) {
            var2 -= Rasterizer2D_xClipStart - x;
            x = Rasterizer2D_xClipStart;
        }

        if(y < Rasterizer2D_yClipStart) {
            var3 -= Rasterizer2D_yClipStart - y;
            y = Rasterizer2D_yClipStart;
        }

        if(x + var2 > Rasterizer2D_xClipEnd) {
            var2 = Rasterizer2D_xClipEnd - x;
        }

        if(var3 + y > Rasterizer2D_yClipEnd) {
            var3 = Rasterizer2D_yClipEnd - y;
        }

        int var5 = Rasterizer2D_width - var2;
        int var6 = x + Rasterizer2D_width * y;

        for(int var7 = -var3; var7 < 0; ++var7) {
            for(int var8 = -var2; var8 < 0; ++var8) {
                Rasterizer2D_pixels[var6++] = color;
            }

            var6 += var5;
        }
        if(depthBuffer != null) {
            for(int var7 = -var3; var7 < 0; ++var7) {
                for(int var8 = -var2; var8 < 0; ++var8) {
                    if(var6 >= depthBuffer.length)
                        return;
                    depthBuffer[var6++] = Float.MAX_VALUE;
                }
                var6 += var5;
            }
        }
    }
    /**
     * Clears the drawingArea by setting every pixel to 0 (black).
     */
    public static void clear() {

        int i = 0;

        int count;
        for(count = Rasterizer2D_width * Rasterizer2D_height - 7; i < count; Rasterizer2D_pixels[i++] = 0) {
            Rasterizer2D_pixels[i++] = 0;
            Rasterizer2D_pixels[i++] = 0;
            Rasterizer2D_pixels[i++] = 0;
            Rasterizer2D_pixels[i++] = 0;
            Rasterizer2D_pixels[i++] = 0;
            Rasterizer2D_pixels[i++] = 0;
            Rasterizer2D_pixels[i++] = 0;
        }

        for(count += 7; i < count; Rasterizer2D_pixels[i++] = 0) {
            ;
        }
        if(depthBuffer != null) {
            for (count = Rasterizer2D_width * Rasterizer2D_height - 7; i < count; depthBuffer[i++] = Float.MAX_VALUE) {
                depthBuffer[i++] = Float.MAX_VALUE;
                depthBuffer[i++] = Float.MAX_VALUE;
                depthBuffer[i++] = Float.MAX_VALUE;
                depthBuffer[i++] = Float.MAX_VALUE;
                depthBuffer[i++] = Float.MAX_VALUE;
                depthBuffer[i++] = Float.MAX_VALUE;
                depthBuffer[i++] = Float.MAX_VALUE;
            }
            for(count += 7; i < count; depthBuffer[i++] = Float.MAX_VALUE) {
                ;
            }
        }

    }

    /**
     * Draws a box filled with a certain colour.
     *
     * @param leftX     The left edge X-Coordinate of the box.
     * @param topY      The top edge Y-Coordinate of the box.
     * @param width     The width of the box.
     * @param height    The height of the box.
     * @param rgbColour The RGBColour of the box.
     */
    public static void drawBox(int leftX, int topY, int width, int height, int rgbColour) {
        if (leftX < Rasterizer2D.Rasterizer2D_xClipStart) {
            width -= Rasterizer2D.Rasterizer2D_xClipStart - leftX;
            leftX = Rasterizer2D.Rasterizer2D_xClipStart;
        }
        if (topY < Rasterizer2D.Rasterizer2D_yClipStart) {
            height -= Rasterizer2D.Rasterizer2D_yClipStart - topY;
            topY = Rasterizer2D.Rasterizer2D_yClipStart;
        }
        if (leftX + width > Rasterizer2D_xClipEnd)
            width = Rasterizer2D_xClipEnd - leftX;
        if (topY + height > Rasterizer2D_yClipEnd)
            height = Rasterizer2D_yClipEnd - topY;
        int leftOver = Rasterizer2D.Rasterizer2D_width - width;
        int pixelIndex = leftX + topY * Rasterizer2D.Rasterizer2D_width;
        for (int rowIndex = 0; rowIndex < height; rowIndex++) {
            for (int columnIndex = 0; columnIndex < width; columnIndex++)
                Rasterizer2D_pixels[pixelIndex++] = rgbColour;
            pixelIndex += leftOver;
        }
    }

    /**
     * Draws a transparent box.
     *
     * @param leftX     The left edge X-Coordinate of the box.
     * @param topY      The top edge Y-Coordinate of the box.
     * @param width     The box width.
     * @param height    The box height.
     * @param rgbColour The box colour.
     * @param opacity   The opacity value ranging from 0 to 256.
     */
    public static void drawTransparentBox(int leftX, int topY, int width, int height, int rgbColour, int opacity) {
        if (leftX < Rasterizer2D.Rasterizer2D_xClipStart) {
            width -= Rasterizer2D.Rasterizer2D_xClipStart - leftX;
            leftX = Rasterizer2D.Rasterizer2D_xClipStart;
        }
        if (topY < Rasterizer2D.Rasterizer2D_yClipStart) {
            height -= Rasterizer2D.Rasterizer2D_yClipStart - topY;
            topY = Rasterizer2D.Rasterizer2D_yClipStart;
        }
        if (leftX + width > Rasterizer2D_xClipEnd)
            width = Rasterizer2D_xClipEnd - leftX;
        if (topY + height > Rasterizer2D_yClipEnd)
            height = Rasterizer2D_yClipEnd - topY;
        int transparency = 256 - opacity;
        int red = (rgbColour >> 16 & 0xff) * opacity;
        int green = (rgbColour >> 8 & 0xff) * opacity;
        int blue = (rgbColour & 0xff) * opacity;
        int leftOver = Rasterizer2D.Rasterizer2D_width - width;
        int pixelIndex = leftX + topY * Rasterizer2D.Rasterizer2D_width;
        for (int rowIndex = 0; rowIndex < height; rowIndex++) {
            for (int columnIndex = 0; columnIndex < width; columnIndex++) {
                int otherRed = (Rasterizer2D_pixels[pixelIndex] >> 16 & 0xff) * transparency;
                int otherGreen = (Rasterizer2D_pixels[pixelIndex] >> 8 & 0xff) * transparency;
                int otherBlue = (Rasterizer2D_pixels[pixelIndex] & 0xff) * transparency;
                int transparentColour = ((red + otherRed >> 8) << 16) + ((green + otherGreen >> 8) << 8) + (blue + otherBlue >> 8);
                Rasterizer2D_pixels[pixelIndex++] = transparentColour;
            }
            pixelIndex += leftOver;
        }
    }

    public static void drawPixels(int height, int posY, int posX, int color, int w) {
        if (posX < Rasterizer2D_xClipStart) {
            w -= Rasterizer2D_xClipStart - posX;
            posX = Rasterizer2D_xClipStart;
        }
        if (posY < Rasterizer2D_yClipStart) {
            height -= Rasterizer2D_yClipStart - posY;
            posY = Rasterizer2D_yClipStart;
        }
        if (posX + w > Rasterizer2D_xClipEnd) {
            w = Rasterizer2D_xClipEnd - posX;
        }
        if (posY + height > Rasterizer2D_yClipEnd) {
            height = Rasterizer2D_yClipEnd - posY;
        }
        int k1 = Rasterizer2D_width - w;
        int l1 = posX + posY * Rasterizer2D_width;
        for (int i2 = -height; i2 < 0; i2++) {
            for (int j2 = -w; j2 < 0; j2++) {
                Rasterizer2D_pixels[l1++] = color;
            }

            l1 += k1;
        }
    }

    /**
     * Draws a 1 pixel thick box outline in a certain colour.
     *
     * @param leftX     The left edge X-Coordinate.
     * @param topY      The top edge Y-Coordinate.
     * @param width     The width.
     * @param height    The height.
     * @param rgbColour The RGB-Colour.
     */
    public static void drawBoxOutline(int leftX, int topY, int width, int height, int rgbColour) {
        drawHorizontalLine(leftX, topY, width, rgbColour);
        drawHorizontalLine(leftX, (topY + height) - 1, width, rgbColour);
        drawVerticalLine(leftX, topY, height, rgbColour);
        drawVerticalLine((leftX + width) - 1, topY, height, rgbColour);
    }

    /**
     * Draws a coloured horizontal line in the drawingArea.
     *
     * @param xPosition The start X-Position of the line.
     * @param yPosition The Y-Position of the line.
     * @param width     The width of the line.
     * @param rgbColour The colour of the line.
     */
    public static void drawHorizontalLine(int xPosition, int yPosition, int width, int rgbColour) {
        if (yPosition < Rasterizer2D_yClipStart || yPosition >= Rasterizer2D_yClipEnd)
            return;
        if (xPosition < Rasterizer2D_xClipStart) {
            width -= Rasterizer2D_xClipStart - xPosition;
            xPosition = Rasterizer2D_xClipStart;
        }
        if (xPosition + width > Rasterizer2D_xClipEnd)
            width = Rasterizer2D_xClipEnd - xPosition;
        int pixelIndex = xPosition + yPosition * Rasterizer2D.Rasterizer2D_width;
        for (int i = 0; i < width; i++)
            Rasterizer2D_pixels[pixelIndex + i] = rgbColour;
    }

    public static void drawHorizontalLine(int x, int y, int width, int color, int alpha) {
    	if (y < Rasterizer2D_yClipStart || y >= Rasterizer2D_yClipEnd)
            return;
        if (x < Rasterizer2D_xClipStart) {
            width -= Rasterizer2D_xClipStart - x;
            x = Rasterizer2D_xClipStart;
        }
        if (x + width > Rasterizer2D_xClipEnd)
            width = Rasterizer2D_xClipEnd - x;
        int transparency = 256 - alpha;
        int red = (color >> 16 & 0xff) * alpha;
        int green = (color >> 8 & 0xff) * alpha;
        int blue = (color & 0xff) * alpha;
        int pixelIndex = x + y * Rasterizer2D.Rasterizer2D_width;
        for (int j3 = 0; j3 < width; j3++) {
            int otherRed = (Rasterizer2D_pixels[pixelIndex] >> 16 & 0xff) * transparency;
            int otherGreen = (Rasterizer2D_pixels[pixelIndex] >> 8 & 0xff) * transparency;
            int otherBlue = (Rasterizer2D_pixels[pixelIndex] & 0xff) * transparency;
            int transparentColour = ((red + otherRed >> 8) << 16) + ((green + otherGreen >> 8) << 8) + (blue + otherBlue >> 8);
            Rasterizer2D_pixels[pixelIndex++] = transparentColour;
        }
    }

    public static void fillRectangle(int xPos, int yPos, int w, int h, int color) {
        if (xPos < Rasterizer2D_xClipStart) {
            w -= Rasterizer2D_xClipStart - xPos;
            xPos = Rasterizer2D_xClipStart;
        }
        if (yPos < Rasterizer2D_yClipStart) {
            h -= Rasterizer2D_yClipStart - yPos;
            yPos = Rasterizer2D_yClipStart;
        }
        if (xPos + w > Rasterizer2D_xClipEnd)
            w = Rasterizer2D_xClipEnd - xPos;
        if (yPos + h > Rasterizer2D_yClipEnd)
            h = Rasterizer2D_yClipEnd - yPos;
        int k1 = Rasterizer2D.Rasterizer2D_width - w;
        int l1 = xPos + yPos * Rasterizer2D.Rasterizer2D_width;
        for (int i2 = -h; i2 < 0; i2++) {
            for (int j2 = -w; j2 < 0; j2++)
            	Rasterizer2D_pixels[l1++] = color;

            l1 += k1;
        }
    }

    public static void fillRectangle(int x, int y, int w, int h, int color, int alpha) {
        if (x < Rasterizer2D_xClipStart) {
            w -= Rasterizer2D_xClipStart - x;
            x = Rasterizer2D_xClipStart;
        }
        if (y < Rasterizer2D_yClipStart) {
            h -= Rasterizer2D_yClipStart - y;
            y = Rasterizer2D_yClipStart;
        }
        if (x + w > Rasterizer2D_xClipEnd)
            w = Rasterizer2D_xClipEnd - x;
        if (y + h > Rasterizer2D_yClipEnd)
            h = Rasterizer2D_yClipEnd - y;
        int a2 = 256 - alpha;
        int r1 = (color >> 16 & 0xff) * alpha;
        int g1 = (color >> 8 & 0xff) * alpha;
        int b1 = (color & 0xff) * alpha;
        int k3 = Rasterizer2D.Rasterizer2D_width - w;
        int pixel = x + y * Rasterizer2D.Rasterizer2D_width;
        for (int i4 = 0; i4 < h; i4++) {
            for (int index = -w; index < 0; index++) {
                int r2 = (Rasterizer2D_pixels[pixel] >> 16 & 0xff) * a2;
                int g2 = (Rasterizer2D_pixels[pixel] >> 8 & 0xff) * a2;
                int b2 = (Rasterizer2D_pixels[pixel] & 0xff) * a2;
                int rgb = ((r1 + r2 >> 8) << 16) + ((g1 + g2 >> 8) << 8) + (b1 + b2 >> 8);
                Rasterizer2D_pixels[pixel++] = rgb;
            }
            pixel += k3;
        }
    }

    /**
     * Draws a coloured vertical line in the drawingArea.
     *
     * @param xPosition The X-Position of the line.
     * @param yPosition The start Y-Position of the line.
     * @param height    The height of the line.
     * @param rgbColour The colour of the line.
     */
    public static void drawVerticalLine(int xPosition, int yPosition, int height, int rgbColour) {
        if (xPosition < Rasterizer2D_xClipStart || xPosition >= Rasterizer2D_xClipEnd)
            return;
        if (yPosition < Rasterizer2D_yClipStart) {
            height -= Rasterizer2D_yClipStart - yPosition;
            yPosition = Rasterizer2D_yClipStart;
        }
        if (yPosition + height > Rasterizer2D_yClipEnd)
            height = Rasterizer2D_yClipEnd - yPosition;
        int pixelIndex = xPosition + yPosition * Rasterizer2D_width;
        for (int rowIndex = 0; rowIndex < height; rowIndex++)
            Rasterizer2D_pixels[pixelIndex + rowIndex * Rasterizer2D_width] = rgbColour;
    }

    /**
     * Draws a 1 pixel thick transparent box outline in a certain colour.
     *
     * @param leftX     The left edge X-Coordinate
     * @param topY      The top edge Y-Coordinate.
     * @param width     The width.
     * @param height    The height.
     * @param rgbColour The RGB-Colour.
     * @param opacity   The opacity value ranging from 0 to 256.
     */
    public static void drawTransparentBoxOutline(int leftX, int topY, int width, int height, int rgbColour, int opacity) {
        drawTransparentHorizontalLine(leftX, topY, width, rgbColour, opacity);
        drawTransparentHorizontalLine(leftX, topY + height - 1, width, rgbColour, opacity);
        if (height >= 3) {
            drawTransparentVerticalLine(leftX, topY + 1, height - 2, rgbColour, opacity);
            drawTransparentVerticalLine(leftX + width - 1, topY + 1, height - 2, rgbColour, opacity);
        }
    }

    /**
     * Draws a transparent coloured horizontal line in the drawingArea.
     *
     * @param xPosition The start X-Position of the line.
     * @param yPosition The Y-Position of the line.
     * @param width     The width of the line.
     * @param rgbColour The colour of the line.
     * @param opacity   The opacity value ranging from 0 to 256.
     */
    public static void drawTransparentHorizontalLine(int xPosition, int yPosition, int width, int rgbColour, int opacity) {
        if (yPosition < Rasterizer2D_yClipStart || yPosition >= Rasterizer2D_yClipEnd) {
            return;
        }
        if (xPosition < Rasterizer2D_xClipStart) {
            width -= Rasterizer2D_xClipStart - xPosition;
            xPosition = Rasterizer2D_xClipStart;
        }
        if (xPosition + width > Rasterizer2D_xClipEnd) {
            width = Rasterizer2D_xClipEnd - xPosition;
        }
        final int transparency = 256 - opacity;
        final int red = (rgbColour >> 16 & 0xff) * opacity;
        final int green = (rgbColour >> 8 & 0xff) * opacity;
        final int blue = (rgbColour & 0xff) * opacity;
        int pixelIndex = xPosition + yPosition * Rasterizer2D.Rasterizer2D_width;
        for (int i = 0; i < width; i++) {
            final int otherRed = (Rasterizer2D_pixels[pixelIndex] >> 16 & 0xff) * transparency;
            final int otherGreen = (Rasterizer2D_pixels[pixelIndex] >> 8 & 0xff) * transparency;
            final int otherBlue = (Rasterizer2D_pixels[pixelIndex] & 0xff) * transparency;
            final int transparentColour = (red + otherRed >> 8 << 16) + (green + otherGreen >> 8 << 8) + (blue + otherBlue >> 8);
            Rasterizer2D_pixels[pixelIndex++] = transparentColour;
        }
    }

    /**
     * Draws a transparent coloured vertical line in the drawingArea.
     *
     * @param xPosition The X-Position of the line.
     * @param yPosition The start Y-Position of the line.
     * @param height    The height of the line.
     * @param rgbColour The colour of the line.
     * @param opacity   The opacity value ranging from 0 to 256.
     */
    public static void drawTransparentVerticalLine(int xPosition, int yPosition, int height, int rgbColour, int opacity) {
        if (xPosition < Rasterizer2D_xClipStart || xPosition >= Rasterizer2D_xClipEnd) {
            return;
        }
        if (yPosition < Rasterizer2D_yClipStart) {
            height -= Rasterizer2D_yClipStart - yPosition;
            yPosition = Rasterizer2D_yClipStart;
        }
        if (yPosition + height > Rasterizer2D_yClipEnd) {
            height = Rasterizer2D_yClipEnd - yPosition;
        }
        final int transparency = 256 - opacity;
        final int red = (rgbColour >> 16 & 0xff) * opacity;
        final int green = (rgbColour >> 8 & 0xff) * opacity;
        final int blue = (rgbColour & 0xff) * opacity;
        int pixelIndex = xPosition + yPosition * Rasterizer2D_width;
        for (int i = 0; i < height; i++) {
            final int otherRed = (Rasterizer2D_pixels[pixelIndex] >> 16 & 0xff) * transparency;
            final int otherGreen = (Rasterizer2D_pixels[pixelIndex] >> 8 & 0xff) * transparency;
            final int otherBlue = (Rasterizer2D_pixels[pixelIndex] & 0xff) * transparency;
            final int transparentColour = (red + otherRed >> 8 << 16) + (green + otherGreen >> 8 << 8) + (blue + otherBlue >> 8);
            Rasterizer2D_pixels[pixelIndex] = transparentColour;
            pixelIndex += Rasterizer2D_width;
        }
    }

    public static void drawFilledCircle(int x, int y, int radius, int color, int alpha) {
        int y1 = y - radius;
        if (y1 < 0) {
            y1 = 0;
        }
        int y2 = y + radius;
        if (y2 >= Rasterizer2D_height) {
            y2 = Rasterizer2D_height - 1;
        }
        int a2 = 256 - alpha;
        int r1 = (color >> 16 & 0xff) * alpha;
        int g1 = (color >> 8 & 0xff) * alpha;
        int b1 = (color & 0xff) * alpha;
        for (int iy = y1; iy <= y2; iy++) {
            int dy = iy - y;
            int dist = (int) Math.sqrt(radius * radius - dy * dy);
            int x1 = x - dist;
            if (x1 < 0) {
                x1 = 0;
            }
            int x2 = x + dist;
            if (x2 >= Rasterizer2D_width) {
                x2 = Rasterizer2D_width - 1;
            }
            int pos = x1 + iy * Rasterizer2D_width;
            for (int ix = x1; ix <= x2; ix++) {
                /*  Tried replacing all pixels[pos] with:
                    Client.instance.gameScreenImageProducer.canvasRaster[pos]
					AND Rasterizer3D.pixels[pos] */
                int r2 = (Rasterizer2D_pixels[pos] >> 16 & 0xff) * a2;
                int g2 = (Rasterizer2D_pixels[pos] >> 8 & 0xff) * a2;
                int b2 = (Rasterizer2D_pixels[pos] & 0xff) * a2;
                Rasterizer2D_pixels[pos++] = ((r1 + r2 >> 8) << 16)
                        + ((g1 + g2 >> 8) << 8)
                        + (b1 + b2 >> 8);
            }
        }
    }

    public static void fillGradientRectangle(int x, int y, int w, int h, int startColour, int endColour) {
		int k1 = 0;
		int l1 = 0x10000 / h;
		if (x < Rasterizer2D_xClipStart) {
			w -= Rasterizer2D_xClipStart - x;
			x = Rasterizer2D_xClipStart;
		}
		if (y < Rasterizer2D_yClipStart) {
			k1 += (Rasterizer2D_yClipStart - y) * l1;
			h -= Rasterizer2D_yClipStart - y;
			y = Rasterizer2D_yClipStart;
		}
		if (x + w > Rasterizer2D_xClipEnd)
			w = Rasterizer2D_xClipEnd - x;
		if (y + h > Rasterizer2D_yClipEnd)
			h = Rasterizer2D_yClipEnd - y;
		int lineGap = Rasterizer2D_width - w;
		int pixelOffset = x + y * Rasterizer2D_width;
		for (int yi = -h; yi < 0; yi++) {
			int blendAmount = 0x10000 - k1 >> 8;
			int blendInverse = k1 >> 8;
			int combinedColour = ((startColour & 0xff00ff) * blendAmount + (endColour & 0xff00ff) * blendInverse & 0xff00ff00) + ((startColour & 0xff00) * blendAmount + (endColour & 0xff00) * blendInverse & 0xff0000) >>> 8;
			int alpha = ((((startColour >> 24) & 0xff) * blendAmount + ((endColour >> 24) & 0xff) * blendInverse) >>> 8) + 5;
			for (int index = -w; index < 0; index++) {
				int backingPixel = Rasterizer2D_pixels[pixelOffset];
				Rasterizer2D_pixels[pixelOffset++] = ((backingPixel & 0xff00ff) * (256 - alpha) + (combinedColour & 0xff00ff) * alpha & 0xff00ff00) + ((backingPixel & 0xff00) * (256 - alpha) + (combinedColour & 0xff00) * alpha & 0xff0000) >>> 8;
			}
			pixelOffset += lineGap;
			k1 += l1;
		}
	}

    public static void fillGradientHorizontalLine(int x, int y, int w, int startColour, int endColour) {
        if (y < Rasterizer2D_yClipStart || y >= Rasterizer2D_yClipEnd)
            return;
        int k1 = 0;
        int l1 = 0x10000 / w;
        if (x < Rasterizer2D_xClipStart) {
            w -= Rasterizer2D_xClipStart - x;
            x = Rasterizer2D_xClipStart;
        }
        if (x + w > Rasterizer2D_xClipEnd)
            w = Rasterizer2D_xClipEnd - x;
        int pixelOffset = x + y * Rasterizer2D_width;

        for (int index = -w; index < 0; index++) {
            int blendAmount = 0x10000 - k1 >> 8;
            int blendInverse = k1 >> 8;
            int combinedColour = ((startColour & 0xff00ff) * blendAmount + (endColour & 0xff00ff) * blendInverse & 0xff00ff00) + ((startColour & 0xff00) * blendAmount + (endColour & 0xff00) * blendInverse & 0xff0000) >>> 8;
            int alpha = ((((startColour >> 24) & 0xff) * blendAmount + ((endColour >> 24) & 0xff) * blendInverse) >>> 8) + 5;

            int backingPixel = Rasterizer2D_pixels[pixelOffset];
            Rasterizer2D_pixels[pixelOffset++] = ((backingPixel & 0xff00ff) * (256 - alpha) + (combinedColour & 0xff00ff) * alpha & 0xff00ff00) + ((backingPixel & 0xff00) * (256 - alpha) + (combinedColour & 0xff00) * alpha & 0xff0000) >>> 8;
            k1 += l1;
        }
    }

    public static Graphics2D createGraphics(boolean renderingHints) {
        Graphics2D g2d = createGraphics(Rasterizer2D_pixels, Rasterizer2D_width, Rasterizer2D_height);
        if (renderingHints) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        return g2d;
    }

    public static Graphics2D createGraphics(int[] pixels, int width, int height) {
        return new BufferedImage(COLOR_MODEL, Raster.createWritableRaster(COLOR_MODEL.createCompatibleSampleModel(width, height), new DataBufferInt(pixels, width * height), null), false, new Hashtable<Object, Object>()).createGraphics();
    }

    public static Shape createSector(int x, int y, int r, int angle) {
        return new Arc2D.Double(x, y, r, r, 90, -angle, Arc2D.PIE);
    }

    public static Shape createCircle(int x, int y, int r) {
        return new Ellipse2D.Double(x, y, r, r);
    }

    public static Shape createRing(Shape sector, Shape innerCircle) {
        Area ring = new Area(sector);
        ring.subtract(new Area(innerCircle));
        return ring;
    }

	public static void drawDarkBox(int xPos, int yPos, int width, int height) {
		drawTransparentBox(xPos, yPos, width, height, 0x000000, 220);
		drawBoxOutline(xPos, yPos, width, height, 0x726451);
		drawBoxOutline(xPos + 1, yPos + 1, width - 2, height - 2, 0x2E2B23);
	}

    public static final int STYLISH_BOX_OUTLINE_OUTLINE_COLOR = 0x383023;
    public static final int STYLISH_BOX_OUTLINE_COLOR = 0x5a5245;
    public static final int STYLISH_BOX_BACKGROUND_COLOR = 0x463D32;

    public static void drawStylishBox(int x, int y, int width, int height) {
        Rasterizer2D.drawBoxOutline(x, y, width, height, STYLISH_BOX_OUTLINE_OUTLINE_COLOR);
        Rasterizer2D.drawBoxOutline(x + 1, y + 1, width - 2, height - 2, STYLISH_BOX_OUTLINE_COLOR);
        Rasterizer2D.drawTransparentBox(x + 2, y + 2, width - 4, height - 4, STYLISH_BOX_BACKGROUND_COLOR, 156);
    }

    public static void drawRoundedRectangle(int x, int y, int width, int height, int color, int alpha, boolean filled, boolean shadowed) {
        if (shadowed)
            drawRoundedRectangle(x + 1, y + 1, width, height, 0, alpha, filled, false);
        if (alpha == -1) {
            if (filled) {
                drawHorizontalLine(x + 2, y + 1, width - 4, color);//method339
                drawHorizontalLine(x + 2, y + height - 2, width - 4, color);//method339
                drawBox(x + 1, y + 2, height - 4, color, width - 2);//method336
            }
            drawHorizontalLine(x + 2, y, width - 4, color);//method339
            drawHorizontalLine(x + 2, y + height - 1, width - 4, color);//method339
            drawVerticalLine(x, y + 2, height - 4, color);//method341
            drawVerticalLine(x + width - 1, y + 2, height - 4, color);//method341
            drawBox(x + 1, y + 1, 1, 1, color);//method336
            drawBox(x + width - 2, y + 1, 1, 1, color);//method336
            drawBox(x + width - 2, y + height - 2, 1, 1, color);//method336
            drawBox(x + 1, y + height - 2, 1, 1, color);//method336
        } else if (alpha != -1) {
            if (filled) {
                drawTransparentHorizontalLine(x + 2, y + 1, width - 4, color, alpha);//method340
                drawTransparentHorizontalLine(x + 2, y + height - 2, width - 4, color, alpha);//method340
                drawTransparentBox(x + 1, y + 2, width - 2, height - 4, color, alpha);//method335
            }
            drawTransparentHorizontalLine(x + 2, y, width - 4, color, alpha);//method340
            drawTransparentHorizontalLine(x + 2, y + height - 1, width - 4, color, alpha);//method340
            drawTransparentVerticalLine(x, y + 2, height - 4, color, alpha);//method342
            drawTransparentVerticalLine(x + width - 1, y + 2, height - 4, color, alpha);//method342
            drawTransparentBox(x + 1, y + 1, 1, 1, color, alpha);//method335
            drawTransparentBox(x + width - 2, y + 1, 1, 1, color, alpha);//method335
            drawTransparentBox(x + 1, y + height - 2, 1, 1, color, alpha);//method335
            drawTransparentBox(x + width - 2, y + height - 2, 1, 1, color, alpha);//method335
        }
    }

    public static void drawSaturationBrightnessMap(int leftX, int topY, int width, int height, float hue) {
        if (leftX < Rasterizer2D.Rasterizer2D_xClipStart) {
            width -= Rasterizer2D.Rasterizer2D_xClipStart - leftX;
            leftX = Rasterizer2D.Rasterizer2D_xClipStart;
        }
        if (topY < Rasterizer2D.Rasterizer2D_yClipStart) {
            height -= Rasterizer2D.Rasterizer2D_yClipStart - topY;
            topY = Rasterizer2D.Rasterizer2D_yClipStart;
        }
        if (leftX + width > Rasterizer2D_xClipEnd)
            width = Rasterizer2D_xClipEnd - leftX;
        if (topY + height > Rasterizer2D_yClipEnd)
            height = Rasterizer2D_yClipEnd - topY;
        int leftOver = Rasterizer2D.Rasterizer2D_width - width;
        int pixelIndex = leftX + topY * Rasterizer2D.Rasterizer2D_width;

        float saturation = 0.0f;
        float brightness = 1.0f;
        float incrementX = (0.01f / width) * 100;
        float incrementY = (0.01f / height) * 100;

        for (int rowIndex = 0; rowIndex < height; rowIndex++) {
            for (int columnIndex = 0; columnIndex < width; columnIndex++) {
                Rasterizer2D_pixels[pixelIndex++] = Color.HSBtoRGB(hue, saturation + (incrementX * columnIndex), brightness - (incrementY * rowIndex));
            }
            pixelIndex += leftOver;
        }
    }

    public static void drawHueMap(int leftX, int topY, int width, int height) {
        if (leftX < Rasterizer2D.Rasterizer2D_xClipStart) {
            width -= Rasterizer2D.Rasterizer2D_xClipStart - leftX;
            leftX = Rasterizer2D.Rasterizer2D_xClipStart;
        }
        if (topY < Rasterizer2D.Rasterizer2D_yClipStart) {
            height -= Rasterizer2D.Rasterizer2D_yClipStart - topY;
            topY = Rasterizer2D.Rasterizer2D_yClipStart;
        }
        if (leftX + width > Rasterizer2D_xClipEnd)
            width = Rasterizer2D_xClipEnd - leftX;
        if (topY + height > Rasterizer2D_yClipEnd)
            height = Rasterizer2D_yClipEnd - topY;
        int leftOver = Rasterizer2D.Rasterizer2D_width - width;
        int pixelIndex = leftX + topY * Rasterizer2D.Rasterizer2D_width;

        float hue = 1.0f;
        float saturation = 1.0f;
        float brightness = 1.0f;
        float incrementY = (0.01f / height) * 100;

        for (int rowIndex = 0; rowIndex < height; rowIndex++) {
            for (int columnIndex = 0; columnIndex < width; columnIndex++) {
                Rasterizer2D_pixels[pixelIndex++] = Color.HSBtoRGB(hue - (incrementY * rowIndex), saturation, brightness);;
            }
            pixelIndex += leftOver;
        }
    }

    /**
     * Draws an arc (from https://www.rune-server.ee/runescape-development/rs2-client/snippets/666519-draw-arc.html)
     */

    public static void drawArc(int x, int y, int width, int height, int stroke, int start, int sweep, int color, int alpha, int closure, boolean fill) {
        Graphics2D graphics = createGraphics(Rasterizer2D.Rasterizer2D_pixels, Rasterizer2D.Rasterizer2D_width, Rasterizer2D.Rasterizer2D_height);
        graphics.setColor(new Color((color >> 16 & 0xff), (color >> 8 & 0xff), (color & 0xff), ((alpha >= 256 || alpha < 0) ? 255 : alpha)));

        RenderingHints render = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        render.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);//fix the 'jittering'

        graphics.setRenderingHints(render);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        if(!fill) {
            graphics.setStroke(new BasicStroke((Math.max(stroke, 1))));
        }
        graphics.setClip(Rasterizer2D.Rasterizer2D_xClipStart, Rasterizer2D.Rasterizer2D_yClipStart, Rasterizer2D.Rasterizer2D_xClipEnd - Rasterizer2D.Rasterizer2D_xClipStart, Rasterizer2D.Rasterizer2D_yClipEnd - Rasterizer2D.Rasterizer2D_yClipStart);
        // Closure types - OPEN(0), CHORD(1), PIE(2)
        Arc2D.Double arc = new Arc2D.Double(x + stroke, y + stroke, width, height, start, sweep, closure);
        if(fill) {
            graphics.fill(arc);
        } else {
            graphics.draw(arc);
        }
    }

    // copied to work with Ellipse2D for circles

    public static Graphics2D createEllipseGraphics(){
        Graphics2D graphics = createGraphics(Rasterizer2D.Rasterizer2D_pixels, Rasterizer2D.Rasterizer2D_width, Rasterizer2D.Rasterizer2D_height);

        RenderingHints render = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        render.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);//fix the 'jittering'

        graphics.setRenderingHints(render);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        graphics.setClip(Rasterizer2D.Rasterizer2D_xClipStart, Rasterizer2D.Rasterizer2D_yClipStart, Rasterizer2D.Rasterizer2D_xClipEnd - Rasterizer2D.Rasterizer2D_xClipStart, Rasterizer2D.Rasterizer2D_yClipEnd - Rasterizer2D.Rasterizer2D_yClipStart);
        return graphics;
    }

    public static void drawEllipse(Graphics2D graphics, int x, int y, int width, int height, int stroke, int color, int alpha, boolean fill) {
        graphics.setColor(new Color((color >> 16 & 0xff), (color >> 8 & 0xff), (color & 0xff), ((alpha >= 256 || alpha < 0) ? 255 : alpha)));

        Ellipse2D.Double ellipse = new Ellipse2D.Double(x + stroke, y + stroke, width, height);
        if(fill) {
            graphics.fill(ellipse);
        } else {
            graphics.draw(ellipse);
        }
    }

    public static void drawBlackBox(int xPos, int yPos) {
        drawBox(xPos - 2, yPos - 1, 1, 71, 0x726451);
        drawBox(xPos + 174, yPos, 1, 69, 0x726451);
        drawBox(xPos - 2, yPos - 2, 178, 1, 0x726451);
        drawBox(xPos, yPos + 68, 174, 1, 0x726451);
        drawBox(xPos - 1, yPos - 1, 1, 71, 0x2E2B23);
        drawBox(xPos + 175, yPos - 1, 1, 71, 0x2E2B23);
        drawBox(xPos, yPos - 1, 175, 1, 0x2E2B23);
        drawBox(xPos, yPos + 69, 175, 1, 0x2E2B23);
        drawTransparentBox(xPos, yPos, 174, 68, 0, 220);
    }
}
