package jagex;

/**
 * Represents a 3D vector with basic operations.
 */
public class Vector3D {

   float x;
   float y;
   float z;

   static {
      new Vector3D(0.0F, 0.0F, 0.0F);
      new Vector3D(1.0F, 1.0F, 1.0F);
      new Vector3D(1.0F, 0.0F, 0.0F);
      new Vector3D(0.0F, 1.0F, 0.0F);
      new Vector3D(0.0F, 0.0F, 1.0F);
   }
   /**
    * Constructor for the Vector3D.
    *
    * @param x The x-coordinate of the vector.
    * @param y The y-coordinate of the vector.
    * @param z The z-coordinate of the vector.
    */
   Vector3D(float x, float y, float z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   /**
    * Computes the magnitude of this vector.
    *
    * @return The magnitude of the vector.
    */
   final float magnitude() {
      return (float)Math.sqrt(this.z * this.z + this.y * this.y + this.x * this.x);
   }
   /**
    * Provides a string representation of the vector.
    *
    * @return A string in the format "x, y, z".
    */
   @Override
   public String toString() {
      return this.x + ", " + this.y + ", " + this.z;
   }
}
