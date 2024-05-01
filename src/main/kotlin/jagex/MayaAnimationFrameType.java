package jagex;

import net.runelite.mapping.Export;
import net.runelite.mapping.ObfuscatedGetter;
import net.runelite.mapping.ObfuscatedName;
import net.runelite.mapping.ObfuscatedSignature;

@ObfuscatedName("dk")
public class MayaAnimationFrameType implements MouseWheel {
   @ObfuscatedName("f")
   @ObfuscatedSignature(
      descriptor = "Ldk;"
   )
   static final MayaAnimationFrameType DEFAULT = new MayaAnimationFrameType(0, 0, (String)null, 0);
   @ObfuscatedName("w")
   @ObfuscatedSignature(
      descriptor = "Ldk;"
   )
   static final MayaAnimationFrameType field1548 = new MayaAnimationFrameType(1, 1, (String)null, 9);
   @ObfuscatedName("v")
   @ObfuscatedSignature(
      descriptor = "Ldk;"
   )
   static final MayaAnimationFrameType field1555 = new MayaAnimationFrameType(2, 2, (String)null, 3);
   @ObfuscatedName("s")
   @ObfuscatedSignature(
      descriptor = "Ldk;"
   )
   static final MayaAnimationFrameType field1550 = new MayaAnimationFrameType(3, 3, (String)null, 6);
   @ObfuscatedName("z")
   @ObfuscatedSignature(
      descriptor = "Ldk;"
   )
   static final MayaAnimationFrameType TRANSFORMATION = new MayaAnimationFrameType(4, 4, (String)null, 1);
   @ObfuscatedName("j")
   @ObfuscatedSignature(
      descriptor = "Ldk;"
   )
   static final MayaAnimationFrameType field1551 = new MayaAnimationFrameType(5, 5, (String)null, 3);
   @ObfuscatedName("i")
   @ObfuscatedGetter(
      intValue = -162414941
   )
   final int field1547;
   @ObfuscatedName("n")
   @ObfuscatedGetter(
      intValue = -1294570757
   )
   final int field1553;
   @ObfuscatedName("l")
   @ObfuscatedGetter(
      intValue = -1207176119
   )
   final int field1554;

   MayaAnimationFrameType(int var1, int var2, String var3, int var4) {
      this.field1547 = var1;
      this.field1553 = var2;
      this.field1554 = var4;
   }

   @ObfuscatedName("f")
   @ObfuscatedSignature(
      descriptor = "(B)I",
      garbageValue = "3"
   )
   @Export("rsOrdinal")
   public int rsOrdinal() {
      return this.field1553;
   }

   @ObfuscatedName("v")
   @ObfuscatedSignature(
      descriptor = "(B)I",
      garbageValue = "20"
   )
   int getMaxIndex() {
      return this.field1554;
   }

}
