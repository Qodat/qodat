package jagex;

public final class Quaternion {
   public static Quaternion[] pool = new Quaternion[0];
   public static int poolSize;
   static int maxPoolSize;
   float qW;
   float qX;
   float qY;
   float qZ;

   static {
      initializePool(100);
      new Quaternion();
   }

   public Quaternion() {
      this.reset();
   }

   static void initializePool(int size) {
      maxPoolSize = size;
      pool = new Quaternion[size];
      poolSize = 0;
   }

   public void release() {
      synchronized(pool) {
         if (poolSize < maxPoolSize - 1) {
            pool[++poolSize - 1] = this;
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
      float sinHalfAngle = (float)Math.sin((double)(angle * 0.5F));
      float cosHalfAngle = (float)Math.cos((double)(0.5F * angle));
      this.qX = axisX * sinHalfAngle;
      this.qY = axisY * sinHalfAngle;
      this.qZ = sinHalfAngle * axisZ;
      this.qW = cosHalfAngle;
   }

   public final void reset() {
      this.qZ = 0.0F;
      this.qY = 0.0F;
      this.qX = 0.0F;
      this.qW = 1.0F;
   }

   public final void multiply(Quaternion other) {
      this.setValues(other.qY * this.qZ + this.qW * other.qX + other.qW * this.qX - this.qY * other.qZ, this.qX * other.qZ + this.qY * other.qW - other.qX * this.qZ + other.qY * this.qW, other.qW * this.qZ + other.qX * this.qY - this.qX * other.qY + other.qZ * this.qW, other.qW * this.qW - this.qX * other.qX - other.qY * this.qY - other.qZ * this.qZ);
   }

   public boolean equals(Object other) {
      if (!(other instanceof Quaternion)) {
         return false;
      } else {
         Quaternion otherQuaternion = (Quaternion)other;
         return this.qX == otherQuaternion.qX && otherQuaternion.qY == this.qY && otherQuaternion.qZ == this.qZ && otherQuaternion.qW == this.qW;
      }
   }

   public int hashCode() {
      boolean var1 = true;
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
