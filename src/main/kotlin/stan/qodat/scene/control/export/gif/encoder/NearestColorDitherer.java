/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package stan.qodat.scene.control.export.gif.encoder;

import java.util.Set;

/**
 * A trivial "ditherer" which always picks the closest color for each pixel, with no error
 * propagation.
 */
public final class NearestColorDitherer implements Ditherer {
    public static final NearestColorDitherer INSTANCE = new NearestColorDitherer();

    private NearestColorDitherer() {
    }

    @Override
    public Image dither(Image image, Set<Color> newColors, Color transColor) {
        int width = image.getWidth(), height = image.getHeight();
        newColors.remove(transColor);
        Color[][] colors = new Color[height][width];
        if (transColor != null) {
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    if (colors[y][x].equals(transColor)) continue;
                    colors[y][x] = image.getColor(x, y).getNearestColor(newColors);
                }
            }
        } else {
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    colors[y][x] = image.getColor(x, y).getNearestColor(newColors);
                }
            }
        }
        return Image.fromColors(colors);
    }
}
