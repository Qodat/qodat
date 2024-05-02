package jagex;

public class KeyFrame {
   int frameNumber;
   float value;
   float field1469 = Float.MAX_VALUE;
   float field1470 = Float.MAX_VALUE;
   float controlPoint1 = Float.MAX_VALUE;
   float controlPoint2 = Float.MAX_VALUE;
   KeyFrame next;

   void read(Buffer var1, @SuppressWarnings("unused") int version) {
      this.frameNumber = var1.readShort();
      this.value = var1.readIntAsFloat();
      this.field1469 = var1.readIntAsFloat();
      this.field1470 = var1.readIntAsFloat();
      this.controlPoint1 = var1.readIntAsFloat();
      this.controlPoint2 = var1.readIntAsFloat();
   }
}
