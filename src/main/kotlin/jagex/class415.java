package jagex;

import net.runelite.mapping.ObfuscatedName;
import net.runelite.mapping.ObfuscatedSignature;

public class class415 {
   float field4632;
   float field4631;
   float field4630;

   static {
      new class415(0.0F, 0.0F, 0.0F);
      new class415(1.0F, 1.0F, 1.0F);
      new class415(1.0F, 0.0F, 0.0F);
      new class415(0.0F, 1.0F, 0.0F);
      new class415(0.0F, 0.0F, 1.0F);
   }

   class415(float var1, float var2, float var3) {
      this.field4632 = var1;
      this.field4631 = var2;
      this.field4630 = var3;
   }

   final float method7872() {
      return (float)Math.sqrt((double)(this.field4630 * this.field4630 + this.field4631 * this.field4631 + this.field4632 * this.field4632));
   }

   public String toString() {
      return this.field4632 + ", " + this.field4631 + ", " + this.field4630;
   }
}
