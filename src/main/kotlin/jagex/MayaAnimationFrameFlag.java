package jagex;

public class MayaAnimationFrameFlag implements MouseWheel {

   static final MayaAnimationFrameFlag NORMAL = new MayaAnimationFrameFlag(0, 0, -1);
   static final MayaAnimationFrameFlag field1559 = new MayaAnimationFrameFlag(1, 1, 0);
   static final MayaAnimationFrameFlag field1568 = new MayaAnimationFrameFlag(2, 2, 1);
   static final MayaAnimationFrameFlag field1560 = new MayaAnimationFrameFlag(3, 3, 2);
   static final MayaAnimationFrameFlag field1562 = new MayaAnimationFrameFlag(4, 4, 3);
   static final MayaAnimationFrameFlag field1578 = new MayaAnimationFrameFlag(5, 5, 4);
   static final MayaAnimationFrameFlag field1564 = new MayaAnimationFrameFlag(6, 6, 5);
   static final MayaAnimationFrameFlag field1565 = new MayaAnimationFrameFlag(7, 7, 6);
   static final MayaAnimationFrameFlag field1561 = new MayaAnimationFrameFlag(8, 8, 7);
   static final MayaAnimationFrameFlag field1567 = new MayaAnimationFrameFlag(9, 9, 8);
   static final MayaAnimationFrameFlag field1563 = new MayaAnimationFrameFlag(10, 10, 0);
   static final MayaAnimationFrameFlag field1569 = new MayaAnimationFrameFlag(11, 11, 1);
   static final MayaAnimationFrameFlag field1570 = new MayaAnimationFrameFlag(12, 12, 2);
   static final MayaAnimationFrameFlag field1571 = new MayaAnimationFrameFlag(13, 13, 3);
   static final MayaAnimationFrameFlag field1572 = new MayaAnimationFrameFlag(14, 14, 4);
   static final MayaAnimationFrameFlag field1573 = new MayaAnimationFrameFlag(15, 15, 5);
   static final MayaAnimationFrameFlag field1574 = new MayaAnimationFrameFlag(16, 16, 0);

   final int field1575;
   final int field1576;
   final int field1577;

   MayaAnimationFrameFlag(int var1, int var2, int var4) {
      this.field1575 = var1;
      this.field1576 = var2;
      this.field1577 = var4;
   }

    static MayaAnimationFrameFlag[] values() {
       return new MayaAnimationFrameFlag[]{NORMAL, field1559, field1568, field1560, field1562, field1578, field1564, field1565, field1561, field1567, field1563, field1569, field1570, field1571, field1572, field1573, field1574};
    }

   public int rsOrdinal() {
      return this.field1576;
   }

   int method3076() {
      return this.field1577;
   }
}
