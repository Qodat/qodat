package jagex;

public interface RSEnum {

   static RSEnum findEnumerated(RSEnum[] var0, int var1) {
       for (RSEnum var4 : var0) {
           if (var1 == var4.rsOrdinal()) {
               return var4;
           }
       }
       return null;
   }

   int rsOrdinal();
}
