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

public final class FloydSteinbergDitherer implements Ditherer {
  public static final FloydSteinbergDitherer INSTANCE = new FloydSteinbergDitherer();

  private static final ErrorComponent[] ERROR_DISTRIBUTION = {
      new ErrorComponent(1, 0, 7.0 / 16.0),
      new ErrorComponent(-1, 1, 3.0 / 16.0),
      new ErrorComponent(0, 1, 5.0 / 16.0),
      new ErrorComponent(1, 1, 1.0 / 16.0)
  };

  private FloydSteinbergDitherer() {
  }

  @Override public Image dither(Image image, Set<Color> newColors, Color transColor) {
    int width = image.getWidth();
    int height = image.getHeight();
    newColors.remove(transColor);

    Color[][] colors = new Color[height][width];
    for (int y = 0; y < height; ++y) {
      for (int x = 0; x < width; ++x) {
        colors[y][x] = image.getColor(x, y);
      }
    }

    if (transColor == null) {
      for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
          Color originalColor = colors[y][x];

          Color replacementColor = originalColor.getNearestColor(newColors);
          colors[y][x] = replacementColor;
          Color error = originalColor.minus(replacementColor);

          for (ErrorComponent component : ERROR_DISTRIBUTION) {
            int siblingX = x + component.deltaX, siblingY = y + component.deltaY;
            if (siblingX >= 0 && siblingY >= 0 && siblingX < width && siblingY < height) {
              Color errorComponent = error.scaled(component.errorFraction);
              colors[siblingY][siblingX] = colors[siblingY][siblingX].plus(errorComponent);
            }
          }
        }
      }
    } else {
      for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
          Color originalColor = colors[y][x];
          if (originalColor.equals(transColor)) continue;

          Color replacementColor = originalColor.getNearestColor(newColors);
          colors[y][x] = replacementColor;
          Color error = originalColor.minus(replacementColor);

          for (ErrorComponent component : ERROR_DISTRIBUTION) {
            int siblingX = x + component.deltaX, siblingY = y + component.deltaY;
            if (siblingX >= 0 && siblingY >= 0 && siblingX < width && siblingY < height) {
              if (colors[siblingY][siblingX].equals(transColor)) continue;
              Color errorComponent = error.scaled(component.errorFraction);
              colors[siblingY][siblingX] = colors[siblingY][siblingX].plus(errorComponent);
            }
          }
        }
      }
    }
    return Image.fromColors(colors);
  }

  private static final class ErrorComponent {
    final int deltaX, deltaY;
    final double errorFraction;

    ErrorComponent(int deltaX, int deltaY, double errorFraction) {
      this.deltaX = deltaX;
      this.deltaY = deltaY;
      this.errorFraction = errorFraction;
    }
  }
}
