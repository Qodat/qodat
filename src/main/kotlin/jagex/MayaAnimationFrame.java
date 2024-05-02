package jagex;

import qodat.cache.definition.AnimationFrameDefinition;

public class MayaAnimationFrame implements AnimationFrameDefinition {

   public static final float ULP = Math.ulp(1.0F);
   public static final float DOUBLE_ULP = 2.0F * ULP;
   public static float[] globalCoefficients = new float[4];
   public static float[] globalRoots = new float[5];

   boolean isInitialised;
   boolean isProcessed;
   MayaAnimationState initialAnimationState;
   MayaAnimationState finalAnimationState;
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

   static float calculateFrameValue(MayaAnimationFrame frame, float currentFrame) {
      if (frame != null && frame.getLastFrameIndex() != 0) {
         if (currentFrame < (float)frame.keyFrames[0].frameNumber) {
            return frame.initialAnimationState == MayaAnimationState.DEFAULT ? frame.keyFrames[0].value : interpolate(frame, currentFrame, true);
         } else if (currentFrame > (float)frame.keyFrames[frame.getLastFrameIndex() - 1].frameNumber) {
            return frame.finalAnimationState == MayaAnimationState.DEFAULT ? frame.keyFrames[frame.getLastFrameIndex() - 1].value : interpolate(frame, currentFrame, false);
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
                                    if (var23 + ULP > 0.0F) {
                                       if (ULP + normalizedValues[0] < 1.3333334F) {
                                          float var24 = normalizedValues[0] - 2.0F;
                                          float var25 = normalizedValues[0] - 1.0F;
                                          float var26 = (float)Math.sqrt((double)(var24 * var24 - 4.0F * var25 * var25));
                                          float var27 = 0.5F * (-var24 + var26);
                                          if (normalizedValues[1] + ULP > var27) {
                                             normalizedValues[1] = var27 - ULP;
                                          } else {
                                             var27 = (-var24 - var26) * 0.5F;
                                             if (normalizedValues[1] < var27 + ULP) {
                                                normalizedValues[1] = ULP + var27;
                                             }
                                          }
                                       } else {
                                          normalizedValues[0] = 1.3333334F - ULP;
                                          normalizedValues[1] = 0.33333334F - ULP;
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
               if (animationFrame.initialAnimationState == MayaAnimationState.MIRROR) {
                  if ((double)fractionalPart != 0.0) {
                     adjustedFrame += firstFrameNumber;
                  } else {
                     adjustedFrame = lastFrameNumber - adjustedFrame;
                  }
               } else if (animationFrame.initialAnimationState != MayaAnimationState.CUSTOM2 && animationFrame.initialAnimationState != MayaAnimationState.REPEAT) {
                  if (animationFrame.initialAnimationState == MayaAnimationState.CUSTOM1) {
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
            } else if (animationFrame.finalAnimationState == MayaAnimationState.MIRROR) {
               if (0.0 != (double)fractionalPart) {
                  adjustedFrame = lastFrameNumber - adjustedFrame;
               } else {
                  adjustedFrame += firstFrameNumber;
               }
            } else if (animationFrame.finalAnimationState != MayaAnimationState.CUSTOM2 && animationFrame.finalAnimationState != MayaAnimationState.REPEAT) {
               if (animationFrame.finalAnimationState == MayaAnimationState.CUSTOM1) {
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
            if (useInitial && animationFrame.initialAnimationState == MayaAnimationState.REPEAT) {
               rangeDifference = animationFrame.keyFrames[animationFrame.getLastFrameIndex() - 1].value - animationFrame.keyFrames[0].value;
               interpolateValue = (float)((double)interpolateValue - integerPart * (double)rangeDifference);
            } else if (!useInitial && animationFrame.finalAnimationState == MayaAnimationState.REPEAT) {
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
            globalCoefficients[3] = animationFrame.controlY;
            globalCoefficients[2] = animationFrame.controlZ;
            globalCoefficients[1] = animationFrame.controlW;
            globalCoefficients[0] = animationFrame.startY - normalizedFrame;
            globalRoots[0] = 0.0F;
            globalRoots[1] = 0.0F;
            globalRoots[2] = 0.0F;
            globalRoots[3] = 0.0F;
            globalRoots[4] = 0.0F;
            int var4 = PolynomialSolver.solvePolynomialEquation(globalCoefficients, 3, 0.0F, true, 1.0F, true, globalRoots);
            if (var4 == 1) {
               calculatedValue = globalRoots[0];
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

   int read(Buffer buffer, int version) {
      int numberOfFrames = buffer.readUnsignedShort();
      buffer.readUnsignedByte();
      int animationStateCode = buffer.readUnsignedByte();
      MayaAnimationState animationState = (MayaAnimationState) RSEnum.findEnumerated(MayaAnimationState.method2852(), animationStateCode);
      if (animationState == null) {
         animationState = MayaAnimationState.DEFAULT;
      }

      this.initialAnimationState = animationState;
      this.finalAnimationState = MayaAnimationState.method2292(buffer.readUnsignedByte());
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
