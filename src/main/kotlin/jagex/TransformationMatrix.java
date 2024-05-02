package jagex;

public class TransformationMatrix {

   float scaleX;
   float skewYX;
   float skewZX;
   float skewXY;
   float scaleY;
   float skewZY;
   float skewXZ;
   float skewYZ;
   float scaleZ;
   float translateX;
   float translateY;
   float translateZ;

   static {
      new TransformationMatrix();
   }

   TransformationMatrix() {
      this.reset();
   }

   void reset() {
      this.translateZ = 0.0F;
      this.translateY = 0.0F;
      this.translateX = 0.0F;
      this.skewYZ = 0.0F;
      this.skewXZ = 0.0F;
      this.skewZY = 0.0F;
      this.skewXY = 0.0F;
      this.skewZX = 0.0F;
      this.skewYX = 0.0F;
      this.scaleZ = 1.0F;
      this.scaleY = 1.0F;
      this.scaleX = 1.0F;
   }

   void rotateX(float angle) {
      float cosAngle = (float)Math.cos(angle);
      float sinAngle = (float)Math.sin(angle);
      float tempSkewYX = this.skewYX;
      float tempScaleY = this.scaleY;
      float tempSkewYZ = this.skewYZ;
      float tempTranslateY = this.translateY;
      this.skewYX = tempSkewYX * cosAngle - this.skewZX * sinAngle;
      this.skewZX = tempSkewYX * sinAngle + cosAngle * this.skewZX;
      this.scaleY = cosAngle * tempScaleY - sinAngle * this.skewZY;
      this.skewZY = cosAngle * this.skewZY + sinAngle * tempScaleY;
      this.skewYZ = cosAngle * tempSkewYZ - sinAngle * this.scaleZ;
      this.scaleZ = this.scaleZ * cosAngle + tempSkewYZ * sinAngle;
      this.translateY = tempTranslateY * cosAngle - sinAngle * this.translateZ;
      this.translateZ = cosAngle * this.translateZ + sinAngle * tempTranslateY;
   }

   void rotateY(float angle) {
      float cosAngle = (float)Math.cos(angle);
      float sinAngle = (float)Math.sin(angle);
      float tempScaleX = this.scaleX;
      float tempSkewXY = this.skewXY;
      float tempSkewXZ = this.skewXZ;
      float tempTranslateX = this.translateX;
      this.scaleX = this.skewZX * sinAngle + tempScaleX * cosAngle;
      this.skewZX = cosAngle * this.skewZX - sinAngle * tempScaleX;
      this.skewXY = sinAngle * this.skewZY + tempSkewXY * cosAngle;
      this.skewZY = this.skewZY * cosAngle - tempSkewXY * sinAngle;
      this.skewXZ = sinAngle * this.scaleZ + tempSkewXZ * cosAngle;
      this.scaleZ = this.scaleZ * cosAngle - tempSkewXZ * sinAngle;
      this.translateX = cosAngle * tempTranslateX + this.translateZ * sinAngle;
      this.translateZ = this.translateZ * cosAngle - sinAngle * tempTranslateX;
   }

   void rotateZ(float angle) {
      float cosAngle = (float)Math.cos(angle);
      float sinAngle = (float)Math.sin(angle);
      float tempScaleX = this.scaleX;
      float tempSkewXY = this.skewXY;
      float tempSkewXZ = this.skewXZ;
      float tempTranslateX = this.translateX;
      this.scaleX = tempScaleX * cosAngle - sinAngle * this.skewYX;
      this.skewYX = cosAngle * this.skewYX + tempScaleX * sinAngle;
      this.skewXY = tempSkewXY * cosAngle - sinAngle * this.scaleY;
      this.scaleY = this.scaleY * cosAngle + sinAngle * tempSkewXY;
      this.skewXZ = tempSkewXZ * cosAngle - sinAngle * this.skewYZ;
      this.skewYZ = cosAngle * this.skewYZ + sinAngle * tempSkewXZ;
      this.translateX = cosAngle * tempTranslateX - this.translateY * sinAngle;
      this.translateY = sinAngle * tempTranslateX + this.translateY * cosAngle;
   }

   void translate(float x, float y, float z) {
      this.translateX += x;
      this.translateY += y;
      this.translateZ += z;
   }

   public String toString() {
      return this.scaleX + "," + this.skewXY + "," + this.skewXZ + "," + this.translateX + "\n" + this.skewYX + "," + this.scaleY + "," + this.skewYZ + "," + this.translateY + "\n" + this.skewZX + "," + this.skewZY + "," + this.scaleZ + "," + this.translateZ;
   }
}
