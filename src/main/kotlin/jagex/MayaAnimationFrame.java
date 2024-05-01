package jagex;

import qodat.cache.definition.AnimationFrameDefinition;

public class MayaAnimationFrame implements AnimationFrameDefinition {

   boolean isInitialised;
   boolean isProcessed;
   AnimationState initialAnimationState;
   AnimationState finalAnimationState;
   KeyFrame[] keyFrames;
   boolean isActive;
   float initialX;
   float field1527;
   float startY;
   float controlW;
   float controlZ;
   float controlY;
   float adjustedX;
   float adjustedW;
   float adjustedZ;
   float adjustedY;
   boolean needsUpdate = true;
   int currentKeyframeIndex = 0;
   float[] frameValues;
   int startFrame;
   int endFrame;
   float lowerBoundFrameValue;
   float upperBoundFrameValue;

   MayaAnimationFrame() {
   }

   static class134 validate(int var0) {
      class134[] var1 = new class134[]{class134.field1621, class134.field1607, class134.field1608, class134.field1609, class134.field1610, class134.field1617, class134.field1612, class134.field1611, class134.field1614};
      class134 var2 = (class134)class4.findEnumerated(var1, var0);
      if (var2 == null) {
         var2 = class134.field1614;
      }

      return var2;
   }

   static float calculateFrameValue(MayaAnimationFrame frame, float currentFrame) {
      if (frame != null && frame.getLastFrameIndex() != 0) {
         if (currentFrame < (float)frame.keyFrames[0].frameNumber) {
            return frame.initialAnimationState == AnimationState.DEFAULT ? frame.keyFrames[0].value : interpolate(frame, currentFrame, true);
         } else if (currentFrame > (float)frame.keyFrames[frame.getLastFrameIndex() - 1].frameNumber) {
            return frame.finalAnimationState == AnimationState.DEFAULT ? frame.keyFrames[frame.getLastFrameIndex() - 1].value : interpolate(frame, currentFrame, false);
         } else if (frame.isProcessed) {
            return frame.keyFrames[0].value;
         } else {
            KeyFrame keyFrame = frame.getKeyFrame(currentFrame);
            boolean isLinearInterpolation = false;
            boolean isInfiniteControlPoints = false;
            if (keyFrame == null) {
               return 0.0F;
            } else {
               if (0.0 == (double)keyFrame.controlPoint1 && 0.0 == (double)keyFrame.controlPoint2) {
                  isLinearInterpolation = true;
               } else if (keyFrame.controlPoint1 == Float.MAX_VALUE && keyFrame.controlPoint2 == Float.MAX_VALUE) {
                  isInfiniteControlPoints = true;
               } else if (keyFrame.next != null) {
                  if (frame.needsUpdate) {
                     float frameStart = (float)keyFrame.frameNumber;
                     float valueStart = keyFrame.value;
                     float controlPointsStart = frameStart + 0.33333334F * keyFrame.controlPoint1;
                     float valueControlStart = keyFrame.controlPoint2 * 0.33333334F + valueStart;
                     float frameEnd = (float)keyFrame.next.frameNumber;
                     float valueEnd = keyFrame.next.value;
                     float controlPointEnd = frameEnd - 0.33333334F * keyFrame.next.field1469;
                     float valueControlEnd = valueEnd - keyFrame.next.field1470 * 0.33333334F;
                     if (frame.isInitialised) {
                        float interpolatedStart = valueControlStart;
                        float interpolatedEnd = valueControlEnd;
                        if (frame != null) {
                           float frameLength = frameEnd - frameStart;
                           if (0.0 != (double)frameLength) {
                              float normalizedStart = controlPointsStart - frameStart;
                              float normalizedEnd = controlPointEnd - frameStart;
                              float[] normalizedValues = new float[]{normalizedStart / frameLength, normalizedEnd / frameLength};
                              frame.isActive = normalizedValues[0] == 0.33333334F && normalizedValues[1] == 0.6666667F;
                              float var21 = normalizedValues[0];
                              float var22 = normalizedValues[1];
                              if ((double)normalizedValues[0] < 0.0) {
                                 normalizedValues[0] = 0.0F;
                              }

                              if ((double)normalizedValues[1] > 1.0) {
                                 normalizedValues[1] = 1.0F;
                              }

                              if ((double)normalizedValues[0] > 1.0 || normalizedValues[1] < -1.0F) {
                                 normalizedValues[1] = 1.0F - normalizedValues[1];
                                 if (normalizedValues[0] < 0.0F) {
                                    normalizedValues[0] = 0.0F;
                                 }

                                 if (normalizedValues[1] < 0.0F) {
                                    normalizedValues[1] = 0.0F;
                                 }

                                 if (normalizedValues[0] > 1.0F || normalizedValues[1] > 1.0F) {
                                    float var23 = (float)(((double)normalizedValues[1] - 2.0) * (double)normalizedValues[1] + (double)((normalizedValues[1] + (normalizedValues[0] - 2.0F)) * normalizedValues[0]) + 1.0);
                                    if (var23 + class121.field1479 > 0.0F) {
                                       if (class121.field1479 + normalizedValues[0] < 1.3333334F) {
                                          float var24 = normalizedValues[0] - 2.0F;
                                          float var25 = normalizedValues[0] - 1.0F;
                                          float var26 = (float)Math.sqrt((double)(var24 * var24 - 4.0F * var25 * var25));
                                          float var27 = 0.5F * (-var24 + var26);
                                          if (normalizedValues[1] + class121.field1479 > var27) {
                                             normalizedValues[1] = var27 - class121.field1479;
                                          } else {
                                             var27 = (-var24 - var26) * 0.5F;
                                             if (normalizedValues[1] < var27 + class121.field1479) {
                                                normalizedValues[1] = class121.field1479 + var27;
                                             }
                                          }
                                       } else {
                                          normalizedValues[0] = 1.3333334F - class121.field1479;
                                          normalizedValues[1] = 0.33333334F - class121.field1479;
                                       }
                                    }
                                 }

                                 normalizedValues[1] = 1.0F - normalizedValues[1];
                              }

                              float var10000;
                              if (var21 != normalizedValues[0]) {
                                 var10000 = frameStart + frameLength * normalizedValues[0];
                                 if ((double)var21 != 0.0) {
                                    interpolatedStart = valueStart + (valueControlStart - valueStart) * normalizedValues[0] / var21;
                                 }
                              }

                              if (var22 != normalizedValues[1]) {
                                 var10000 = frameStart + frameLength * normalizedValues[1];
                                 if ((double)var22 != 1.0) {
                                    interpolatedEnd = (float)((double)valueEnd - (1.0 - (double)normalizedValues[1]) * (double)(valueEnd - valueControlEnd) / (1.0 - (double)var22));
                                 }
                              }

                              frame.initialX = frameStart;
                              frame.field1527 = frameEnd;
                              calculateBezierCoefficients(0.0F, normalizedValues[0], normalizedValues[1], 1.0F, frame);
                              adjustBezierValues(valueStart, interpolatedStart, interpolatedEnd, valueEnd, frame);
                           }
                        }
                     } else {
                        interpolateValues(frame, frameStart, controlPointsStart, controlPointEnd, frameEnd, valueStart, valueControlStart, valueControlEnd, valueEnd);
                     }

                     frame.needsUpdate = false;
                  }
               } else {
                  isLinearInterpolation = true;
               }

               if (isLinearInterpolation) {
                  return keyFrame.value;
               } else if (isInfiniteControlPoints) {
                  return (float)keyFrame.frameNumber != currentFrame && keyFrame.next != null ? keyFrame.next.value : keyFrame.value;
               } else {
                  return frame.isInitialised ? advancedCalculate(frame, currentFrame) : simpleCalculate(frame, currentFrame);
               }
            }
         }
      } else {
         return 0.0F;
      }
   }

   static float interpolate(MayaAnimationFrame animationFrame, float currentFrame, boolean useInitial) {
      float interpolateValue = 0.0F;
      if (animationFrame != null && animationFrame.getLastFrameIndex() != 0) {
         float firstFrameNumber = (float)animationFrame.keyFrames[0].frameNumber;
         float lastFrameNumber = (float)animationFrame.keyFrames[animationFrame.getLastFrameIndex() - 1].frameNumber;
         float frameRange = lastFrameNumber - firstFrameNumber;
         if ((double)frameRange == 0.0) {
            return animationFrame.keyFrames[0].value;
         } else {
            float relativePosition = 0.0F;
            if (currentFrame > lastFrameNumber) {
               relativePosition = (currentFrame - lastFrameNumber) / frameRange;
            } else {
               relativePosition = (currentFrame - firstFrameNumber) / frameRange;
            }

            double integerPart = (double)((int)relativePosition);
            float fractionalPart = Math.abs((float)((double)relativePosition - integerPart));
            float adjustedFrame = fractionalPart * frameRange;
            integerPart = Math.abs(1.0 + integerPart);
            double halfIntegerPart = integerPart / 2.0;
            double floorHalfIntegerPart = (double)((int)halfIntegerPart);
            fractionalPart = (float)(halfIntegerPart - floorHalfIntegerPart);
            float adjustedValue;
            float scale;
            if (useInitial) {
               if (animationFrame.initialAnimationState == AnimationState.MIRROR) {
                  if ((double)fractionalPart != 0.0) {
                     adjustedFrame += firstFrameNumber;
                  } else {
                     adjustedFrame = lastFrameNumber - adjustedFrame;
                  }
               } else if (animationFrame.initialAnimationState != AnimationState.CUSTOM2 && animationFrame.initialAnimationState != AnimationState.REPEAT) {
                  if (animationFrame.initialAnimationState == AnimationState.CUSTOM1) {
                     adjustedFrame = firstFrameNumber - currentFrame;
                     adjustedValue = animationFrame.keyFrames[0].field1469;
                     scale = animationFrame.keyFrames[0].field1470;
                     interpolateValue = animationFrame.keyFrames[0].value;
                     if (0.0 != (double)adjustedValue) {
                        interpolateValue -= scale * adjustedFrame / adjustedValue;
                     }

                     return interpolateValue;
                  }
               } else {
                  adjustedFrame = lastFrameNumber - adjustedFrame;
               }
            } else if (animationFrame.finalAnimationState == AnimationState.MIRROR) {
               if (0.0 != (double)fractionalPart) {
                  adjustedFrame = lastFrameNumber - adjustedFrame;
               } else {
                  adjustedFrame += firstFrameNumber;
               }
            } else if (animationFrame.finalAnimationState != AnimationState.CUSTOM2 && animationFrame.finalAnimationState != AnimationState.REPEAT) {
               if (animationFrame.finalAnimationState == AnimationState.CUSTOM1) {
                  adjustedFrame = currentFrame - lastFrameNumber;
                  adjustedValue = animationFrame.keyFrames[animationFrame.getLastFrameIndex() - 1].controlPoint1;
                  scale = animationFrame.keyFrames[animationFrame.getLastFrameIndex() - 1].controlPoint2;
                  interpolateValue = animationFrame.keyFrames[animationFrame.getLastFrameIndex() - 1].value;
                  if (0.0 != (double)adjustedValue) {
                     interpolateValue += scale * adjustedFrame / adjustedValue;
                  }

                  return interpolateValue;
               }
            } else {
               adjustedFrame += firstFrameNumber;
            }

            interpolateValue = calculateFrameValue(animationFrame, adjustedFrame);
            float rangeDifference;
            if (useInitial && animationFrame.initialAnimationState == AnimationState.REPEAT) {
               rangeDifference = animationFrame.keyFrames[animationFrame.getLastFrameIndex() - 1].value - animationFrame.keyFrames[0].value;
               interpolateValue = (float)((double)interpolateValue - integerPart * (double)rangeDifference);
            } else if (!useInitial && animationFrame.finalAnimationState == AnimationState.REPEAT) {
               rangeDifference = animationFrame.keyFrames[animationFrame.getLastFrameIndex() - 1].value - animationFrame.keyFrames[0].value;
               interpolateValue = (float)((double)interpolateValue + integerPart * (double)rangeDifference);
            }

            return interpolateValue;
         }
      } else {
         return interpolateValue;
      }
   }

   static void calculateBezierCoefficients(float start, float control1, float control2, float end, MayaAnimationFrame animationFrame) {
      float diff1 = control1 - start;
      float diff2 = control2 - control1;
      float diff3 = end - control2;
      float delta1 = diff2 - diff1;
      animationFrame.controlY = diff3 - diff2 - delta1;
      animationFrame.controlZ = delta1 + delta1 + delta1;
      animationFrame.controlW = diff1 + diff1 + diff1;
      animationFrame.startY = start;
   }

   static void adjustBezierValues(float start, float control1, float control2, float end, MayaAnimationFrame animationFrame) {
      float diff1 = control1 - start;
      float diff2 = control2 - control1;
      float diff3 = end - control2;
      float delta1 = diff2 - diff1;
      animationFrame.adjustedY = diff3 - diff2 - delta1;
      animationFrame.adjustedZ = delta1 + delta1 + delta1;
      animationFrame.adjustedW = diff1 + diff1 + diff1;
      animationFrame.adjustedX = start;
   }

   static void interpolateValues(MayaAnimationFrame animationFrame, float start, float controlStart, float var3, float end, float valueStart, float var6, float var7, float valueEnd) {
      if (animationFrame != null) {
         animationFrame.initialX = start;
         float span = end - start;
         float valueSpan = valueEnd - valueStart;
         float normalizedStart = controlStart - start;
         float normalizedEnd = 0.0F;
         float adjustedStart = 0.0F;
         if (0.0 != (double)normalizedStart) {
            normalizedEnd = (var6 - valueStart) / normalizedStart;
         }

         normalizedStart = end - var3;
         if (0.0 != (double)normalizedStart) {
            adjustedStart = (valueEnd - var7) / normalizedStart;
         }

         float squareSpan = 1.0F / (span * span);
         float squareStart = normalizedEnd * span;
         float squareEnd = span * adjustedStart;
         animationFrame.startY = (squareEnd + squareStart - valueSpan - valueSpan) * squareSpan / span;
         animationFrame.controlW = (valueSpan + valueSpan + valueSpan - squareStart - squareStart - squareEnd) * squareSpan;
         animationFrame.controlZ = normalizedEnd;
         animationFrame.controlY = valueStart;
      }
   }

   static float advancedCalculate(MayaAnimationFrame animationFrame, float currentFrame) {
      if (animationFrame == null) {
         return 0.0F;
      } else {
         float normalizedFrame;
         if (currentFrame == animationFrame.initialX) {
            normalizedFrame = 0.0F;
         } else if (currentFrame == animationFrame.field1527) {
            normalizedFrame = 1.0F;
         } else {
            normalizedFrame = (currentFrame - animationFrame.initialX) / (animationFrame.field1527 - animationFrame.initialX);
         }

         float calculatedValue;
         if (animationFrame.isActive) {
            calculatedValue = normalizedFrame;
         } else {
            class121.field1477[3] = animationFrame.controlY;
            class121.field1477[2] = animationFrame.controlZ;
            class121.field1477[1] = animationFrame.controlW;
            class121.field1477[0] = animationFrame.startY - normalizedFrame;
            class121.field1485[0] = 0.0F;
            class121.field1485[1] = 0.0F;
            class121.field1485[2] = 0.0F;
            class121.field1485[3] = 0.0F;
            class121.field1485[4] = 0.0F;
            int var4 = solvePolynomialEquation(class121.field1477, 3, 0.0F, true, 1.0F, true, class121.field1485);
            if (var4 == 1) {
               calculatedValue = class121.field1485[0];
            } else {
               calculatedValue = 0.0F;
            }
         }

         return animationFrame.adjustedX + calculatedValue * (animationFrame.adjustedW + (animationFrame.adjustedY * calculatedValue + animationFrame.adjustedZ) * calculatedValue);
      }
   }

   static float simpleCalculate(MayaAnimationFrame animationFrame, float currentFrame) {
      if (animationFrame == null) {
         return 0.0F;
      } else {
         float frameDifference = currentFrame - animationFrame.initialX;
         return frameDifference * (animationFrame.controlZ + (animationFrame.startY * frameDifference + animationFrame.controlW) * frameDifference) + animationFrame.controlY;
      }
   }

   public static int solvePolynomialEquation(float[] coefficients, int degree, float lowerBound, boolean isLowerInclusive, float upperBound, boolean isUpperInclusive, float[] roots) {
      float totalCoefficientSum = 0.0F;

      for(int i = 0; i < degree + 1; ++i) {
         totalCoefficientSum += Math.abs(coefficients[i]);
      }

      float tolerance = (Math.abs(lowerBound) + Math.abs(upperBound)) * (float)(degree + 1) * class121.field1479;
      if (totalCoefficientSum <= tolerance) {
         return -1;
      } else {
         float[] normalizedCoefficients = new float[degree + 1];

         int rootCount;
         for(rootCount = 0; rootCount < degree + 1; ++rootCount) {
            normalizedCoefficients[rootCount] = 1.0F / totalCoefficientSum * coefficients[rootCount];
         }

         while(Math.abs(normalizedCoefficients[degree]) < tolerance) {
            --degree;
         }

         rootCount = 0;
         if (degree == 0) {
            return rootCount;
         } else if (degree == 1) {
            roots[0] = -normalizedCoefficients[0] / normalizedCoefficients[1];
            boolean isRootBeyondLower = isLowerInclusive ? lowerBound < roots[0] + tolerance : lowerBound < roots[0] - tolerance;
            boolean isRootBeyondUpper = isUpperInclusive ? upperBound > roots[0] - tolerance : upperBound > tolerance + roots[0];
            rootCount = isRootBeyondLower && isRootBeyondUpper ? 1 : 0;
            if (rootCount > 0) {
               if (isLowerInclusive && roots[0] < lowerBound) {
                  roots[0] = lowerBound;
               } else if (isUpperInclusive && roots[0] > upperBound) {
                  roots[0] = upperBound;
               }
            }

            return rootCount;
         } else {
            PolynomialSolver polynomialSolver = new PolynomialSolver(normalizedCoefficients, degree);
            float[] derivativeRoots = new float[degree + 1];

            for(int var13 = 1; var13 <= degree; ++var13) {
               derivativeRoots[var13 - 1] = (float)var13 * normalizedCoefficients[var13];
            }

            float[] tempRoots = new float[degree + 1];
            int derivativeRootCount = solvePolynomialEquation(derivativeRoots, degree - 1, lowerBound, false, upperBound, false, tempRoots);
            if (derivativeRootCount == -1) {
               return 0;
            } else {
               boolean hasConverged = false;
               float lastEvaluatedValue = 0.0F;
               float nextEvaluatedValue = 0.0F;
               float nextRoot = 0.0F;

               for(int i = 0; i <= derivativeRootCount; ++i) {
                  if (rootCount > degree) {
                     return rootCount;
                  }

                  float root;
                  if (i == 0) {
                     root = lowerBound;
                     nextEvaluatedValue = evaluatePolynomial(normalizedCoefficients, degree, lowerBound);
                     if (Math.abs(nextEvaluatedValue) <= tolerance && isLowerInclusive) {
                        roots[rootCount++] = lowerBound;
                     }
                  } else {
                     root = nextRoot;
                     nextEvaluatedValue = lastEvaluatedValue;
                  }

                  if (derivativeRootCount == i) {
                     nextRoot = upperBound;
                     hasConverged = false;
                  } else {
                     nextRoot = tempRoots[i];
                  }

                  lastEvaluatedValue = evaluatePolynomial(normalizedCoefficients, degree, nextRoot);
                  if (hasConverged) {
                     hasConverged = false;
                  } else if (Math.abs(lastEvaluatedValue) < tolerance) {
                     if (derivativeRootCount != i || isUpperInclusive) {
                        roots[rootCount++] = nextRoot;
                        hasConverged = true;
                     }
                  } else if (nextEvaluatedValue < 0.0F && lastEvaluatedValue > 0.0F || nextEvaluatedValue > 0.0F && lastEvaluatedValue < 0.0F) {
                     int index = rootCount++;
                     float startX = root;
                     float endX = nextRoot;
                     float startValue = evaluatePolynomial(polynomialSolver.coefficients, polynomialSolver.degree, root);
                     float refinedRoot;
                     if (Math.abs(startValue) < class121.field1479) {
                        refinedRoot = root;
                     } else {
                        float endValue = evaluatePolynomial(polynomialSolver.coefficients, polynomialSolver.degree, nextRoot);
                        if (Math.abs(endValue) < class121.field1479) {
                           refinedRoot = nextRoot;
                        } else {
                           float var28 = 0.0F;
                           float var29 = 0.0F;
                           float var30 = 0.0F;
                           float var35 = 0.0F;
                           boolean var36 = true;
                           boolean var37 = false;

                           do {
                              var37 = false;
                              if (var36) {
                                 var28 = startX;
                                 var35 = startValue;
                                 var29 = endX - startX;
                                 var30 = var29;
                                 var36 = false;
                              }

                              if (Math.abs(var35) < Math.abs(endValue)) {
                                 startX = endX;
                                 endX = var28;
                                 var28 = startX;
                                 startValue = endValue;
                                 endValue = var35;
                                 var35 = startValue;
                              }

                              float var38 = class121.field1480 * Math.abs(endX) + 0.0F;
                              float var39 = (var28 - endX) * 0.5F;
                              boolean var40 = Math.abs(var39) > var38 && 0.0F != endValue;
                              if (var40) {
                                 if (!(Math.abs(var30) < var38) && !(Math.abs(startValue) <= Math.abs(endValue))) {
                                    float var34 = endValue / startValue;
                                    float var31;
                                    float var32;
                                    if (var28 == startX) {
                                       var31 = var39 * 2.0F * var34;
                                       var32 = 1.0F - var34;
                                    } else {
                                       var32 = startValue / var35;
                                       float var33 = endValue / var35;
                                       var31 = ((var32 - var33) * var39 * 2.0F * var32 - (endX - startX) * (var33 - 1.0F)) * var34;
                                       var32 = (var34 - 1.0F) * (var33 - 1.0F) * (var32 - 1.0F);
                                    }

                                    if ((double)var31 > 0.0) {
                                       var32 = -var32;
                                    } else {
                                       var31 = -var31;
                                    }

                                    var34 = var30;
                                    var30 = var29;
                                    if (2.0F * var31 < var32 * 3.0F * var39 - Math.abs(var38 * var32) && var31 < Math.abs(var32 * 0.5F * var34)) {
                                       var29 = var31 / var32;
                                    } else {
                                       var29 = var39;
                                       var30 = var39;
                                    }
                                 } else {
                                    var29 = var39;
                                    var30 = var39;
                                 }

                                 startX = endX;
                                 startValue = endValue;
                                 if (Math.abs(var29) > var38) {
                                    endX += var29;
                                 } else if ((double)var39 > 0.0) {
                                    endX += var38;
                                 } else {
                                    endX -= var38;
                                 }

                                 endValue = evaluatePolynomial(polynomialSolver.coefficients, polynomialSolver.degree, endX);
                                 if ((double)(endValue * (var35 / Math.abs(var35))) > 0.0) {
                                    var36 = true;
                                    var37 = true;
                                 } else {
                                    var37 = true;
                                 }
                              }
                           } while(var37);

                           refinedRoot = endX;
                        }
                     }

                     roots[index] = refinedRoot;
                     if (rootCount > 1 && roots[rootCount - 2] >= roots[rootCount - 1] - tolerance) {
                        roots[rootCount - 2] = 0.5F * (roots[rootCount - 1] + roots[rootCount - 2]);
                        --rootCount;
                     }
                  }
               }

               return rootCount;
            }
         }
      }
   }

   /**
    * Evaluates a polynomial at a given point using Horner's method.
    * @param coefficients An array of coefficients of the polynomial.
    * @param degree The degree of the polynomial.
    * @param x The point at which the polynomial is to be evaluated.
    * @return The value of the polynomial at the given point.
    */
   static float evaluatePolynomial(float[] coefficients, int degree, float x) {
      float result = coefficients[degree];

      for(int i = degree - 1; i >= 0; --i) {
         result = result * x + coefficients[i];
      }

      return result;
   }

   int read(Buffer buffer, int version) {
      int numberOfFrames = buffer.readUnsignedShort();
      validate(buffer.readUnsignedByte());
      int animationStateCode = buffer.readUnsignedByte();
      AnimationState animationState = (AnimationState)class4.findEnumerated(AnimationState.method2852(), animationStateCode);
      if (animationState == null) {
         animationState = AnimationState.DEFAULT;
      }

      this.initialAnimationState = animationState;
      this.finalAnimationState = AnimationState.method2292(buffer.readUnsignedByte());
      this.isInitialised = buffer.readUnsignedByte() != 0;
      this.keyFrames = new KeyFrame[numberOfFrames];
      KeyFrame previousFrame = null;

      for(int i = 0; i < numberOfFrames; ++i) {
         KeyFrame frame = new KeyFrame();
         frame.read(buffer, version);
         this.keyFrames[i] = frame;
         if (previousFrame != null) {
            previousFrame.next = frame;
         }

         previousFrame = frame;
      }

      return numberOfFrames;
   }

   void initialiseKeyFrames() {
      this.startFrame = this.keyFrames[0].frameNumber;
      this.endFrame = this.keyFrames[this.getLastFrameIndex() - 1].frameNumber;
      this.frameValues = new float[this.getFrameCount() + 1];

      for(int frame = this.getFirstFrame(); frame <= this.getLastFrame(); ++frame) {
         this.frameValues[frame - this.getFirstFrame()] = calculateFrameValue(this, (float)frame);
      }

      this.keyFrames = null;
      this.lowerBoundFrameValue = calculateFrameValue(this, (float)(this.getFirstFrame() - 1));
      this.upperBoundFrameValue = calculateFrameValue(this, (float)(this.getLastFrame() + 1));
   }

   public float evaluate(int frame) {
      if (frame < this.getFirstFrame()) {
         return this.lowerBoundFrameValue;
      } else {
         return frame > this.getLastFrame() ? this.upperBoundFrameValue : this.frameValues[frame - this.getFirstFrame()];
      }
   }

   int getFirstFrame() {
      return this.startFrame;
   }

   int getLastFrame() {
      return this.endFrame;
   }

   int getFrameCount() {
      return this.getLastFrame() - this.getFirstFrame();
   }

   int findFrameIndex(float frame) {

      if (this.currentKeyframeIndex < 0 || !((float)this.keyFrames[this.currentKeyframeIndex].frameNumber <= frame) || this.keyFrames[this.currentKeyframeIndex].next != null && !((float)this.keyFrames[this.currentKeyframeIndex].next.frameNumber > frame)) {
         if (!(frame < (float)this.getFirstFrame()) && !(frame > (float)this.getLastFrame())) {
            int count = this.getLastFrameIndex();
            int current = this.currentKeyframeIndex;
            if (count > 0) {
               int low = 0;
               int high = count - 1;

               do {
                  int var6 = high + low >> 1;
                  if (frame < (float)this.keyFrames[var6].frameNumber) {
                     if (frame > (float)this.keyFrames[var6 - 1].frameNumber) {
                        current = var6 - 1;
                        break;
                     }

                     high = var6 - 1;
                  } else {
                     if (!(frame > (float)this.keyFrames[var6].frameNumber)) {
                        current = var6;
                        break;
                     }

                     if (frame < (float)this.keyFrames[var6 + 1].frameNumber) {
                        current = var6;
                        break;
                     }

                     low = var6 + 1;
                  }
               } while(low <= high);
            }

            if (current != this.currentKeyframeIndex) {
               this.currentKeyframeIndex = current;
               this.needsUpdate = true;
            }

            return this.currentKeyframeIndex;
         } else {
            return -1;
         }
      } else {
         return this.currentKeyframeIndex;
      }
   }

   KeyFrame getKeyFrame(float frame) {
      int index = this.findFrameIndex(frame);
      return index >= 0 && index < this.keyFrames.length ? this.keyFrames[index] : null;
   }

   int getLastFrameIndex() {
      return this.keyFrames == null ? 0 : this.keyFrames.length;
   }
}
