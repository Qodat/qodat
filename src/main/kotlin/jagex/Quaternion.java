package jagex;

public final class Quaternion {

   public static final int POOL_CAPACITY = 100;
   public static final Quaternion[] pool = new Quaternion[POOL_CAPACITY];
   public static int poolIndex;

   float qW;
   float qX;
   float qY;
   float qZ;

   public Quaternion() {
      this.reset();
   }

   static Quaternion pool() {
      Quaternion quaternionX;
      synchronized(pool) {
         if (poolIndex == 0) {
            quaternionX = new Quaternion();
         } else {
            pool[--poolIndex].reset();
            quaternionX = pool[poolIndex];
         }
      }
      return quaternionX;
   }

   public void release() {
      synchronized(pool) {
         if (poolIndex < POOL_CAPACITY - 1) {
            pool[++poolIndex - 1] = this;
         }
      }
   }

   void setValues(float x, float y, float z, float w) {
      this.qX = x;
      this.qY = y;
      this.qZ = z;
      this.qW = w;
   }

   public void setFromAxisAngle(float axisX, float axisY, float axisZ, float angle) {
      float sinHalfAngle = (float)Math.sin(angle * 0.5F);
      float cosHalfAngle = (float)Math.cos(0.5F * angle);
      this.qX = axisX * sinHalfAngle;
      this.qY = axisY * sinHalfAngle;
      this.qZ = sinHalfAngle * axisZ;
      this.qW = cosHalfAngle;
   }

   public void reset() {
      this.qZ = 0.0F;
      this.qY = 0.0F;
      this.qX = 0.0F;
      this.qW = 1.0F;
   }

   public void multiply(Quaternion other) {
      this.setValues(other.qY * this.qZ + this.qW * other.qX + other.qW * this.qX - this.qY * other.qZ, this.qX * other.qZ + this.qY * other.qW - other.qX * this.qZ + other.qY * this.qW, other.qW * this.qZ + other.qX * this.qY - this.qX * other.qY + other.qZ * this.qW, other.qW * this.qW - this.qX * other.qX - other.qY * this.qY - other.qZ * this.qZ);
   }

   public boolean equals(Object other) {
      if (!(other instanceof Quaternion otherQuaternion)) {
         return false;
      } else {
          return this.qX == otherQuaternion.qX && otherQuaternion.qY == this.qY && otherQuaternion.qZ == this.qZ && otherQuaternion.qW == this.qW;
      }
   }

   public int hashCode() {
       float result = 1.0F;
      result = result * 31.0F + this.qX;
      result = this.qY + 31.0F * result;
      result = this.qZ + result * 31.0F;
      result = 31.0F * result + this.qW;
      return (int)result;
   }

   public String toString() {
      return this.qX + "," + this.qY + "," + this.qZ + "," + this.qW;
   }
}
