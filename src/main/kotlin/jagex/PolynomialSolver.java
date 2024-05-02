package jagex;

public class PolynomialSolver {

   float[] coefficients;
   int degree;

   PolynomialSolver(float[] coefficients, int degree) {
      this.coefficients = coefficients;
      this.degree = degree;
   }

   /**
    * Attempts to solve a polynomial equation within specified bounds and stores the roots.
    *
    * @param coefficients     Array of coefficients for the polynomial equation.
    * @param degree           The degree of the polynomial.
    * @param lowerBound       The lower bound for root searching.
    * @param isLowerInclusive Indicates if the lower bound is inclusive.
    * @param upperBound       The upper bound for root searching.
    * @param isUpperInclusive Indicates if the upper bound is inclusive.
    * @param roots            Array to store the roots found within the bounds.
    * @return The number of roots found or -1 if the coefficients sum to a value within the tolerance.
    */
   public static int solvePolynomialEquation(
           float[] coefficients,
           int degree,
           float lowerBound,
           boolean isLowerInclusive,
           float upperBound,
           boolean isUpperInclusive,
           float[] roots
   ) {
       float totalCoefficientSum = 0.0F;

       for (int i = 0; i < degree + 1; ++i) {
           totalCoefficientSum += Math.abs(coefficients[i]);
       }

       float tolerance = (Math.abs(lowerBound) + Math.abs(upperBound)) * (float) (degree + 1) * MayaAnimationFrame.ULP;
       if (totalCoefficientSum <= tolerance) {
           return -1;
       } else {
           float[] normalizedCoefficients = new float[degree + 1];

           int rootCount;
           for (rootCount = 0; rootCount < degree + 1; ++rootCount) {
               normalizedCoefficients[rootCount] = 1.0F / totalCoefficientSum * coefficients[rootCount];
           }

           while (Math.abs(normalizedCoefficients[degree]) < tolerance) {
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

               for (int var13 = 1; var13 <= degree; ++var13) {
                   derivativeRoots[var13 - 1] = (float) var13 * normalizedCoefficients[var13];
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

                   for (int i = 0; i <= derivativeRootCount; ++i) {
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
                           if (Math.abs(startValue) < MayaAnimationFrame.ULP) {
                               refinedRoot = root;
                           } else {
                               float endValue = evaluatePolynomial(polynomialSolver.coefficients, polynomialSolver.degree, nextRoot);
                               if (Math.abs(endValue) < MayaAnimationFrame.ULP) {
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

                                       float var38 = MayaAnimationFrame.DOUBLE_ULP * Math.abs(endX) + 0.0F;
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

                                               if ((double) var31 > 0.0) {
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
                                           } else if ((double) var39 > 0.0) {
                                               endX += var38;
                                           } else {
                                               endX -= var38;
                                           }

                                           endValue = evaluatePolynomial(polynomialSolver.coefficients, polynomialSolver.degree, endX);
                                           if ((double) (endValue * (var35 / Math.abs(var35))) > 0.0) {
                                               var36 = true;
                                               var37 = true;
                                           } else {
                                               var37 = true;
                                           }
                                       }
                                   } while (var37);

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
    *
    * @param coefficients An array of coefficients of the polynomial.
    * @param degree       The degree of the polynomial.
    * @param x            The point at which the polynomial is to be evaluated.
    * @return The value of the polynomial at the given point.
    */
   static float evaluatePolynomial(float[] coefficients, int degree, float x) {
       float result = coefficients[degree];

       for (int i = degree - 1; i >= 0; --i) {
           result = result * x + coefficients[i];
       }

       return result;
   }
}
