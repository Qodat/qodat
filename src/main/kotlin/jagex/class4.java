package jagex;

public class class4 {

    public static MouseWheel findEnumerated(MouseWheel[] var0, int var1) {
        for (MouseWheel var4 : var0) {
            if (var1 == var4.rsOrdinal()) {
                return var4;
            }
        }
        return null;
    }
}
