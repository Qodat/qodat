package jagex;

public class MayaAnimationFrameType implements RSEnum {

   static final MayaAnimationFrameType DEFAULT = new MayaAnimationFrameType(0, 0);
   static final MayaAnimationFrameType USE_SECONDARY_FRAMES = new MayaAnimationFrameType(1, 9);
   static final MayaAnimationFrameType field1555 = new MayaAnimationFrameType(2, 3);
   static final MayaAnimationFrameType field1550 = new MayaAnimationFrameType(3, 6);
   static final MayaAnimationFrameType TRANSFORMATION = new MayaAnimationFrameType(4, 1);
   static final MayaAnimationFrameType field1551 = new MayaAnimationFrameType(5, 3);

   final int rsOrdinal;
   final int maxIndex;

   MayaAnimationFrameType(int rsOrdinal, int maxIndex) {
      this.rsOrdinal = rsOrdinal;
      this.maxIndex = maxIndex;
   }

   @Override
   public int rsOrdinal() {
      return this.rsOrdinal;
   }

   int getMaxIndex() {
      return this.maxIndex;
   }
}
