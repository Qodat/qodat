package jagex;

import java.util.Arrays;

public final class BoneTransform {
   public static final BoneTransform identity;
   public static BoneTransform[] classPool = new BoneTransform[0];
   static int maxPoolSize = 100;
   public static int poolSize;
   public float[] matrix = new float[16];

   static {
      classPool = new BoneTransform[100];
      poolSize = 0;
      identity = new BoneTransform();
   }

   public BoneTransform() {
      this.identityMatrix();
   }

   public BoneTransform(BoneTransform other) {
      this.copy(other);
   }

   public BoneTransform(Buffer buffer, boolean readOrientation) {
      this.read(buffer, readOrientation);
   }

   public void release() {
      synchronized(classPool) {
         if (poolSize < maxPoolSize - 1) {
            classPool[++poolSize - 1] = this;
         }

      }
   }

   void read(Buffer buffer, boolean readOrientation) {
      if (readOrientation) {
         TransformationMatrix tempMatrix = new TransformationMatrix();

         int xRotation = buffer.readShort();
         xRotation &= 16383;
         float xRadians = (float)(6.283185307179586 * (double)((float)xRotation / 16384.0F));
         tempMatrix.rotateX(xRadians);

         int yRotation = buffer.readShort();
         yRotation &= 16383;
         float yRadians = (float)(6.283185307179586 * (double)((float)yRotation / 16384.0F));
         tempMatrix.rotateY(yRadians);

         int zRotation = buffer.readShort();
         zRotation &= 16383;
         float zRadians = (float)((double)((float)zRotation / 16384.0F) * 6.283185307179586);
         tempMatrix.rotateZ(zRadians);

         float tx = buffer.readShort();
         float ty = buffer.readShort();
         float tz = buffer.readShort();
         tempMatrix.translate(tx, ty, tz);

         this.applyTransformation(tempMatrix);
      } else {
         for(int i = 0; i < 16; ++i) {
            this.matrix[i] = buffer.readIntAsFloat();
         }
      }

   }

   float[] extractEulerAngles() {
      float[] eulerAngles = new float[3];
      if ((double)this.matrix[2] < 0.999 && (double)this.matrix[2] > -0.999) {
         eulerAngles[1] = (float)(-Math.asin((double)this.matrix[2]));
         double cosPitch = Math.cos((double)eulerAngles[1]);
         eulerAngles[0] = (float)Math.atan2((double)this.matrix[6] / cosPitch, (double)this.matrix[10] / cosPitch);
         eulerAngles[2] = (float)Math.atan2((double)this.matrix[1] / cosPitch, (double)this.matrix[0] / cosPitch);
      } else {
         eulerAngles[0] = 0.0F;
         eulerAngles[1] = (float)Math.atan2((double)this.matrix[2], 0.0);
         eulerAngles[2] = (float)Math.atan2((double)(-this.matrix[9]), (double)this.matrix[5]);
      }

      return eulerAngles;
   }

   public float[] extractRotation() {
      float[] var1 = new float[]{(float)(-Math.asin((double)this.matrix[6])), 0.0F, 0.0F};
      double var2 = Math.cos((double)var1[0]);
      double var4;
      double var6;
      if (Math.abs(var2) > 0.005) {
         var4 = (double)this.matrix[2];
         var6 = (double)this.matrix[10];
         double var8 = (double)this.matrix[4];
         double var10 = (double)this.matrix[5];
         var1[1] = (float)Math.atan2(var4, var6);
         var1[2] = (float)Math.atan2(var8, var10);
      } else {
         var4 = (double)this.matrix[1];
         var6 = (double)this.matrix[0];
         if (this.matrix[6] < 0.0F) {
            var1[1] = (float)Math.atan2(var4, var6);
         } else {
            var1[1] = (float)(-Math.atan2(var4, var6));
         }

         var1[2] = 0.0F;
      }

      return var1;
   }

   public void identityMatrix() {
      this.matrix[0] = 1.0F;
      this.matrix[1] = 0.0F;
      this.matrix[2] = 0.0F;
      this.matrix[3] = 0.0F;
      this.matrix[4] = 0.0F;
      this.matrix[5] = 1.0F;
      this.matrix[6] = 0.0F;
      this.matrix[7] = 0.0F;
      this.matrix[8] = 0.0F;
      this.matrix[9] = 0.0F;
      this.matrix[10] = 1.0F;
      this.matrix[11] = 0.0F;
      this.matrix[12] = 0.0F;
      this.matrix[13] = 0.0F;
      this.matrix[14] = 0.0F;
      this.matrix[15] = 1.0F;
   }

   public void zeroMatrix() {
      this.matrix[0] = 0.0F;
      this.matrix[1] = 0.0F;
      this.matrix[2] = 0.0F;
      this.matrix[3] = 0.0F;
      this.matrix[4] = 0.0F;
      this.matrix[5] = 0.0F;
      this.matrix[6] = 0.0F;
      this.matrix[7] = 0.0F;
      this.matrix[8] = 0.0F;
      this.matrix[9] = 0.0F;
      this.matrix[10] = 0.0F;
      this.matrix[11] = 0.0F;
      this.matrix[12] = 0.0F;
      this.matrix[13] = 0.0F;
      this.matrix[14] = 0.0F;
      this.matrix[15] = 0.0F;
   }

   public void copy(BoneTransform var1) {
      System.arraycopy(var1.matrix, 0, this.matrix, 0, 16);
   }

   public void setUniformScale(float scale) {
      this.setScale(scale, scale, scale);
   }

   public void setScale(float scaleX, float scaleY, float scaleZ) {
      this.identityMatrix();
      this.matrix[0] = scaleX;
      this.matrix[5] = scaleY;
      this.matrix[10] = scaleZ;
   }

   public void addTransform(BoneTransform transform) {
      for(int i = 0; i < this.matrix.length; ++i) {
         float[] matrix = this.matrix;
         matrix[i] += transform.matrix[i];
      }
   }

   public void combine(BoneTransform transform) {
      float var2 = transform.matrix[0] * this.matrix[0] + transform.matrix[4] * this.matrix[1] + transform.matrix[8] * this.matrix[2] + transform.matrix[12] * this.matrix[3];
      float var3 = this.matrix[3] * transform.matrix[13] + this.matrix[1] * transform.matrix[5] + this.matrix[0] * transform.matrix[1] + transform.matrix[9] * this.matrix[2];
      float var4 = this.matrix[3] * transform.matrix[14] + transform.matrix[2] * this.matrix[0] + transform.matrix[6] * this.matrix[1] + this.matrix[2] * transform.matrix[10];
      float var5 = this.matrix[3] * transform.matrix[15] + this.matrix[2] * transform.matrix[11] + this.matrix[0] * transform.matrix[3] + transform.matrix[7] * this.matrix[1];
      float var6 = this.matrix[7] * transform.matrix[12] + this.matrix[6] * transform.matrix[8] + transform.matrix[0] * this.matrix[4] + this.matrix[5] * transform.matrix[4];
      float var7 = this.matrix[5] * transform.matrix[5] + this.matrix[4] * transform.matrix[1] + transform.matrix[9] * this.matrix[6] + this.matrix[7] * transform.matrix[13];
      float var8 = transform.matrix[14] * this.matrix[7] + transform.matrix[10] * this.matrix[6] + transform.matrix[6] * this.matrix[5] + transform.matrix[2] * this.matrix[4];
      float var9 = transform.matrix[11] * this.matrix[6] + this.matrix[5] * transform.matrix[7] + this.matrix[4] * transform.matrix[3] + this.matrix[7] * transform.matrix[15];
      float var10 = this.matrix[9] * transform.matrix[4] + transform.matrix[0] * this.matrix[8] + this.matrix[10] * transform.matrix[8] + this.matrix[11] * transform.matrix[12];
      float var11 = transform.matrix[13] * this.matrix[11] + this.matrix[10] * transform.matrix[9] + transform.matrix[5] * this.matrix[9] + transform.matrix[1] * this.matrix[8];
      float var12 = this.matrix[11] * transform.matrix[14] + transform.matrix[6] * this.matrix[9] + transform.matrix[2] * this.matrix[8] + this.matrix[10] * transform.matrix[10];
      float var13 = transform.matrix[15] * this.matrix[11] + transform.matrix[3] * this.matrix[8] + this.matrix[9] * transform.matrix[7] + this.matrix[10] * transform.matrix[11];
      float var14 = this.matrix[14] * transform.matrix[8] + this.matrix[12] * transform.matrix[0] + this.matrix[13] * transform.matrix[4] + transform.matrix[12] * this.matrix[15];
      float var15 = transform.matrix[13] * this.matrix[15] + this.matrix[12] * transform.matrix[1] + this.matrix[13] * transform.matrix[5] + transform.matrix[9] * this.matrix[14];
      float var16 = this.matrix[15] * transform.matrix[14] + transform.matrix[2] * this.matrix[12] + transform.matrix[6] * this.matrix[13] + this.matrix[14] * transform.matrix[10];
      float var17 = this.matrix[15] * transform.matrix[15] + this.matrix[14] * transform.matrix[11] + this.matrix[13] * transform.matrix[7] + this.matrix[12] * transform.matrix[3];
      this.matrix[0] = var2;
      this.matrix[1] = var3;
      this.matrix[2] = var4;
      this.matrix[3] = var5;
      this.matrix[4] = var6;
      this.matrix[5] = var7;
      this.matrix[6] = var8;
      this.matrix[7] = var9;
      this.matrix[8] = var10;
      this.matrix[9] = var11;
      this.matrix[10] = var12;
      this.matrix[11] = var13;
      this.matrix[12] = var14;
      this.matrix[13] = var15;
      this.matrix[14] = var16;
      this.matrix[15] = var17;
   }

   public void applyQuaternion(Quaternion quaternion) {
      float var2 = quaternion.qW * quaternion.qW;
      float var3 = quaternion.qX * quaternion.qW;
      float var4 = quaternion.qW * quaternion.qY;
      float var5 = quaternion.qZ * quaternion.qW;
      float var6 = quaternion.qX * quaternion.qX;
      float var7 = quaternion.qY * quaternion.qX;
      float var8 = quaternion.qX * quaternion.qZ;
      float var9 = quaternion.qY * quaternion.qY;
      float var10 = quaternion.qZ * quaternion.qY;
      float var11 = quaternion.qZ * quaternion.qZ;
      this.matrix[0] = var2 + var6 - var11 - var9;
      this.matrix[1] = var5 + var5 + var7 + var7;
      this.matrix[2] = var8 - var4 - var4 + var8;
      this.matrix[4] = var7 + (var7 - var5 - var5);
      this.matrix[5] = var2 + var9 - var6 - var11;
      this.matrix[6] = var10 + var3 + var10 + var3;
      this.matrix[8] = var4 + var8 + var8 + var4;
      this.matrix[9] = var10 - var3 - var3 + var10;
      this.matrix[10] = var2 + var11 - var9 - var6;
   }

   void applyTransformation(TransformationMatrix other) {
      this.matrix[0] = other.scaleX;
      this.matrix[1] = other.skewYX;
      this.matrix[2] = other.skewZX;
      this.matrix[3] = 0.0F;
      this.matrix[4] = other.skewXY;
      this.matrix[5] = other.scaleY;
      this.matrix[6] = other.skewZY;
      this.matrix[7] = 0.0F;
      this.matrix[8] = other.skewXZ;
      this.matrix[9] = other.skewYZ;
      this.matrix[10] = other.scaleZ;
      this.matrix[11] = 0.0F;
      this.matrix[12] = other.translateX;
      this.matrix[13] = other.translateY;
      this.matrix[14] = other.translateZ;
      this.matrix[15] = 1.0F;
   }

   float calculateDeterminant() {
      return this.matrix[8] * this.matrix[5] * this.matrix[3] * this.matrix[14] + this.matrix[13] * this.matrix[10] * this.matrix[3] * this.matrix[4] + (this.matrix[8] * this.matrix[1] * this.matrix[6] * this.matrix[15] + this.matrix[14] * this.matrix[1] * this.matrix[4] * this.matrix[11] + (this.matrix[14] * this.matrix[9] * this.matrix[0] * this.matrix[7] + this.matrix[11] * this.matrix[6] * this.matrix[0] * this.matrix[13] + (this.matrix[15] * this.matrix[10] * this.matrix[5] * this.matrix[0] - this.matrix[11] * this.matrix[5] * this.matrix[0] * this.matrix[14] - this.matrix[15] * this.matrix[9] * this.matrix[0] * this.matrix[6]) - this.matrix[0] * this.matrix[7] * this.matrix[10] * this.matrix[13] - this.matrix[1] * this.matrix[4] * this.matrix[10] * this.matrix[15]) - this.matrix[12] * this.matrix[6] * this.matrix[1] * this.matrix[11] - this.matrix[8] * this.matrix[1] * this.matrix[7] * this.matrix[14] + this.matrix[12] * this.matrix[10] * this.matrix[7] * this.matrix[1] + this.matrix[2] * this.matrix[4] * this.matrix[9] * this.matrix[15] - this.matrix[13] * this.matrix[11] * this.matrix[4] * this.matrix[2] - this.matrix[2] * this.matrix[5] * this.matrix[8] * this.matrix[15] + this.matrix[12] * this.matrix[11] * this.matrix[5] * this.matrix[2] + this.matrix[13] * this.matrix[2] * this.matrix[7] * this.matrix[8] - this.matrix[7] * this.matrix[2] * this.matrix[9] * this.matrix[12] - this.matrix[14] * this.matrix[4] * this.matrix[3] * this.matrix[9]) - this.matrix[3] * this.matrix[5] * this.matrix[10] * this.matrix[12] - this.matrix[13] * this.matrix[3] * this.matrix[6] * this.matrix[8] + this.matrix[9] * this.matrix[6] * this.matrix[3] * this.matrix[12];
   }

   public void normalize() {
      float var1 = 1.0F / this.calculateDeterminant();
      float var2 = var1 * (this.matrix[13] * this.matrix[6] * this.matrix[11] + (this.matrix[10] * this.matrix[5] * this.matrix[15] - this.matrix[14] * this.matrix[11] * this.matrix[5] - this.matrix[6] * this.matrix[9] * this.matrix[15]) + this.matrix[9] * this.matrix[7] * this.matrix[14] - this.matrix[13] * this.matrix[10] * this.matrix[7]);
      float var3 = var1 * (this.matrix[13] * this.matrix[10] * this.matrix[3] + (this.matrix[10] * -this.matrix[1] * this.matrix[15] + this.matrix[14] * this.matrix[11] * this.matrix[1] + this.matrix[9] * this.matrix[2] * this.matrix[15] - this.matrix[13] * this.matrix[11] * this.matrix[2] - this.matrix[3] * this.matrix[9] * this.matrix[14]));
      float var4 = (this.matrix[15] * this.matrix[1] * this.matrix[6] - this.matrix[14] * this.matrix[7] * this.matrix[1] - this.matrix[2] * this.matrix[5] * this.matrix[15] + this.matrix[2] * this.matrix[7] * this.matrix[13] + this.matrix[5] * this.matrix[3] * this.matrix[14] - this.matrix[13] * this.matrix[6] * this.matrix[3]) * var1;
      float var5 = (this.matrix[9] * this.matrix[6] * this.matrix[3] + (this.matrix[2] * this.matrix[5] * this.matrix[11] + this.matrix[1] * this.matrix[7] * this.matrix[10] + this.matrix[11] * this.matrix[6] * -this.matrix[1] - this.matrix[9] * this.matrix[7] * this.matrix[2] - this.matrix[3] * this.matrix[5] * this.matrix[10])) * var1;
      float var6 = (this.matrix[6] * this.matrix[8] * this.matrix[15] + this.matrix[15] * -this.matrix[4] * this.matrix[10] + this.matrix[14] * this.matrix[4] * this.matrix[11] - this.matrix[12] * this.matrix[11] * this.matrix[6] - this.matrix[7] * this.matrix[8] * this.matrix[14] + this.matrix[12] * this.matrix[10] * this.matrix[7]) * var1;
      float var7 = var1 * (this.matrix[8] * this.matrix[3] * this.matrix[14] + this.matrix[2] * this.matrix[11] * this.matrix[12] + (this.matrix[10] * this.matrix[0] * this.matrix[15] - this.matrix[0] * this.matrix[11] * this.matrix[14] - this.matrix[15] * this.matrix[8] * this.matrix[2]) - this.matrix[3] * this.matrix[10] * this.matrix[12]);
      float var8 = var1 * (this.matrix[2] * this.matrix[4] * this.matrix[15] + this.matrix[15] * this.matrix[6] * -this.matrix[0] + this.matrix[7] * this.matrix[0] * this.matrix[14] - this.matrix[12] * this.matrix[7] * this.matrix[2] - this.matrix[14] * this.matrix[3] * this.matrix[4] + this.matrix[3] * this.matrix[6] * this.matrix[12]);
      float var9 = (this.matrix[4] * this.matrix[3] * this.matrix[10] + this.matrix[11] * this.matrix[0] * this.matrix[6] - this.matrix[10] * this.matrix[7] * this.matrix[0] - this.matrix[11] * this.matrix[4] * this.matrix[2] + this.matrix[8] * this.matrix[2] * this.matrix[7] - this.matrix[8] * this.matrix[3] * this.matrix[6]) * var1;
      float var10 = (this.matrix[13] * this.matrix[7] * this.matrix[8] + this.matrix[11] * this.matrix[5] * this.matrix[12] + (this.matrix[9] * this.matrix[4] * this.matrix[15] - this.matrix[4] * this.matrix[11] * this.matrix[13] - this.matrix[15] * this.matrix[5] * this.matrix[8]) - this.matrix[12] * this.matrix[7] * this.matrix[9]) * var1;
      float var11 = (this.matrix[3] * this.matrix[9] * this.matrix[12] + (this.matrix[8] * this.matrix[1] * this.matrix[15] + this.matrix[9] * -this.matrix[0] * this.matrix[15] + this.matrix[11] * this.matrix[0] * this.matrix[13] - this.matrix[1] * this.matrix[11] * this.matrix[12] - this.matrix[13] * this.matrix[3] * this.matrix[8])) * var1;
      float var12 = (this.matrix[15] * this.matrix[5] * this.matrix[0] - this.matrix[0] * this.matrix[7] * this.matrix[13] - this.matrix[1] * this.matrix[4] * this.matrix[15] + this.matrix[1] * this.matrix[7] * this.matrix[12] + this.matrix[13] * this.matrix[4] * this.matrix[3] - this.matrix[12] * this.matrix[5] * this.matrix[3]) * var1;
      float var13 = var1 * (this.matrix[3] * this.matrix[5] * this.matrix[8] + (this.matrix[11] * this.matrix[4] * this.matrix[1] + -this.matrix[0] * this.matrix[5] * this.matrix[11] + this.matrix[7] * this.matrix[0] * this.matrix[9] - this.matrix[1] * this.matrix[7] * this.matrix[8] - this.matrix[3] * this.matrix[4] * this.matrix[9]));
      float var14 = var1 * (this.matrix[14] * this.matrix[9] * -this.matrix[4] + this.matrix[13] * this.matrix[4] * this.matrix[10] + this.matrix[8] * this.matrix[5] * this.matrix[14] - this.matrix[12] * this.matrix[10] * this.matrix[5] - this.matrix[8] * this.matrix[6] * this.matrix[13] + this.matrix[12] * this.matrix[9] * this.matrix[6]);
      float var15 = (this.matrix[14] * this.matrix[0] * this.matrix[9] - this.matrix[13] * this.matrix[0] * this.matrix[10] - this.matrix[14] * this.matrix[8] * this.matrix[1] + this.matrix[12] * this.matrix[1] * this.matrix[10] + this.matrix[2] * this.matrix[8] * this.matrix[13] - this.matrix[12] * this.matrix[2] * this.matrix[9]) * var1;
      float var16 = var1 * (this.matrix[14] * this.matrix[1] * this.matrix[4] + this.matrix[5] * -this.matrix[0] * this.matrix[14] + this.matrix[6] * this.matrix[0] * this.matrix[13] - this.matrix[12] * this.matrix[6] * this.matrix[1] - this.matrix[4] * this.matrix[2] * this.matrix[13] + this.matrix[12] * this.matrix[2] * this.matrix[5]);
      float var17 = (this.matrix[9] * this.matrix[2] * this.matrix[4] + this.matrix[1] * this.matrix[6] * this.matrix[8] + (this.matrix[5] * this.matrix[0] * this.matrix[10] - this.matrix[0] * this.matrix[6] * this.matrix[9] - this.matrix[4] * this.matrix[1] * this.matrix[10]) - this.matrix[2] * this.matrix[5] * this.matrix[8]) * var1;
      this.matrix[0] = var2;
      this.matrix[1] = var3;
      this.matrix[2] = var4;
      this.matrix[3] = var5;
      this.matrix[4] = var6;
      this.matrix[5] = var7;
      this.matrix[6] = var8;
      this.matrix[7] = var9;
      this.matrix[8] = var10;
      this.matrix[9] = var11;
      this.matrix[10] = var12;
      this.matrix[11] = var13;
      this.matrix[12] = var14;
      this.matrix[13] = var15;
      this.matrix[14] = var16;
      this.matrix[15] = var17;
   }

   public float[] extractScale() {
      float[] var1 = new float[3];
      class415 var2 = new class415(this.matrix[0], this.matrix[1], this.matrix[2]);
      class415 var3 = new class415(this.matrix[4], this.matrix[5], this.matrix[6]);
      class415 var4 = new class415(this.matrix[8], this.matrix[9], this.matrix[10]);
      var1[0] = var2.method7872();
      var1[1] = var3.method7872();
      var1[2] = var4.method7872();
      return var1;
   }

   public String toString() {
      StringBuilder var1 = new StringBuilder();
      this.extractRotation();
      this.extractEulerAngles();

      for(int var2 = 0; var2 < 4; ++var2) {
         for(int var3 = 0; var3 < 4; ++var3) {
            if (var3 > 0) {
               var1.append("\t");
            }

            float var4 = this.matrix[var3 + var2 * 4];
            if (Math.sqrt((double)(var4 * var4)) < 9.999999747378752E-5) {
               var4 = 0.0F;
            }

            var1.append(var4);
         }

         var1.append("\n");
      }

      return var1.toString();
   }

   public int hashCode() {
      boolean var1 = true;
      int var2 = 1;
      var2 = var2 * 31 + Arrays.hashCode(this.matrix);
      return var2;
   }

   public boolean equals(Object other) {
      if (!(other instanceof BoneTransform)) {
         return false;
      } else {
         BoneTransform otherTransform = (BoneTransform)other;

         for(int var3 = 0; var3 < 16; ++var3) {
            if (otherTransform.matrix[var3] != this.matrix[var3]) {
               return false;
            }
         }

         return true;
      }
   }
}
