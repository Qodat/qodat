package jagex;

public enum AnimationState implements MouseWheel {

   DEFAULT(0, 0),
   CUSTOM1(1, 1),
   CUSTOM2(2, 2),
   REPEAT(3, 3),
   MIRROR(4, 4);

   final int field1508;
   final int field1507;

   AnimationState(int var3, int var4) {
      this.field1508 = var3;
      this.field1507 = var4;
   }

   static AnimationState method2292(int var0) {
      AnimationState var1 = (AnimationState)class4.findEnumerated(method2852(), var0);
      if (var1 == null) {
         var1 = DEFAULT;
      }

      return var1;
   }

   public int rsOrdinal() {
      return this.field1507;
   }

   static AnimationState[] method2852() {
      return new AnimationState[]{AnimationState.DEFAULT, AnimationState.CUSTOM1, AnimationState.CUSTOM2, AnimationState.REPEAT, AnimationState.MIRROR};
   }
}
