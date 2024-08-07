package jagex;

public enum MayaAnimationState implements RSEnum {

   DEFAULT(0, 0),
   CUSTOM1(1, 1),
   CUSTOM2(2, 2),
   REPEAT(3, 3),
   MIRROR(4, 4);

   final int field1508;
   final int field1507;

   MayaAnimationState(int var3, int var4) {
      this.field1508 = var3;
      this.field1507 = var4;
   }

   static MayaAnimationState method2292(int var0) {
      MayaAnimationState var1 = (MayaAnimationState) RSEnum.findEnumerated(method2852(), var0);
      if (var1 == null) {
         var1 = DEFAULT;
      }

      return var1;
   }

   public int rsOrdinal() {
      return this.field1507;
   }

   static MayaAnimationState[] method2852() {
      return new MayaAnimationState[]{MayaAnimationState.DEFAULT, MayaAnimationState.CUSTOM1, MayaAnimationState.CUSTOM2, MayaAnimationState.REPEAT, MayaAnimationState.MIRROR};
   }
}
