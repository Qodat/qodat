package jagex;

import java.util.Arrays;

public final class BoneTransform {
    public static int poolIndex = 0;
    public static final int POOL_CAPACITY = 100;
    public static final BoneTransform[] POOL = new BoneTransform[POOL_CAPACITY];
    public static final BoneTransform IDENTITY = new BoneTransform();

    public float[] matrix = new float[16];

    public BoneTransform() {
        this.identityMatrix();
    }

    public BoneTransform(BoneTransform other) {
        this.copy(other);
    }

    public BoneTransform(Buffer buffer, boolean readOrientation) {
        this.read(buffer, readOrientation);
    }

    static BoneTransform pool() {
        BoneTransform temporaryTransform;
        synchronized (POOL) {
            if (poolIndex == 0) {
                temporaryTransform = new BoneTransform();
            } else {
                POOL[--poolIndex].identityMatrix();
                temporaryTransform = POOL[poolIndex];
            }
        }
        return temporaryTransform;
    }

    public void release() {
        synchronized (POOL) {
            if (poolIndex < POOL_CAPACITY - 1) {
                POOL[++poolIndex - 1] = this;
            }
        }
    }

    void read(Buffer buffer, boolean readOrientation) {
        if (readOrientation) {
            final TransformationMatrix tempMatrix = new TransformationMatrix();

            int xRotation = buffer.readShort();
            xRotation &= 16383;
            float xRadians = (float) (6.283185307179586 * (double) ((float) xRotation / 16384.0F));
            tempMatrix.rotateX(xRadians);

            int yRotation = buffer.readShort();
            yRotation &= 16383;
            float yRadians = (float) (6.283185307179586 * (double) ((float) yRotation / 16384.0F));
            tempMatrix.rotateY(yRadians);

            int zRotation = buffer.readShort();
            zRotation &= 16383;
            float zRadians = (float) ((double) ((float) zRotation / 16384.0F) * 6.283185307179586);
            tempMatrix.rotateZ(zRadians);

            float tx = buffer.readShort();
            float ty = buffer.readShort();
            float tz = buffer.readShort();
            tempMatrix.translate(tx, ty, tz);

            applyTransformation(tempMatrix);
        } else {
            for (int i = 0; i < 16; ++i)
                matrix[i] = buffer.readIntAsFloat();
        }
    }

    float[] extractEulerAngles() {
        float[] eulerAngles = new float[3];
        if ((double) matrix[2] < 0.999 && (double) matrix[2] > -0.999) {
            eulerAngles[1] = (float) (-Math.asin(matrix[2]));
            double cosPitch = Math.cos(eulerAngles[1]);
            eulerAngles[0] = (float) Math.atan2((double) matrix[6] / cosPitch, (double) matrix[10] / cosPitch);
            eulerAngles[2] = (float) Math.atan2((double) matrix[1] / cosPitch, (double) matrix[0] / cosPitch);
        } else {
            eulerAngles[0] = 0.0F;
            eulerAngles[1] = (float) Math.atan2(matrix[2], 0.0);
            eulerAngles[2] = (float) Math.atan2(-matrix[9], matrix[5]);
        }

        return eulerAngles;
    }

    public float[] extractRotation() {
        float[] var1 = new float[]{(float) (-Math.asin(matrix[6])), 0.0F, 0.0F};
        double var2 = Math.cos(var1[0]);
        double var4;
        double var6;
        if (Math.abs(var2) > 0.005) {
            var4 = matrix[2];
            var6 = matrix[10];
            double var8 = matrix[4];
            double var10 = matrix[5];
            var1[1] = (float) Math.atan2(var4, var6);
            var1[2] = (float) Math.atan2(var8, var10);
        } else {
            var4 = matrix[1];
            var6 = matrix[0];
            if (matrix[6] < 0.0F)
                var1[1] = (float) Math.atan2(var4, var6);
            else
                var1[1] = (float) (-Math.atan2(var4, var6));
            var1[2] = 0.0F;
        }
        return var1;
    }

    public void identityMatrix() {
        matrix[0] = 1.0F;
        matrix[1] = 0.0F;
        matrix[2] = 0.0F;
        matrix[3] = 0.0F;
        matrix[4] = 0.0F;
        matrix[5] = 1.0F;
        matrix[6] = 0.0F;
        matrix[7] = 0.0F;
        matrix[8] = 0.0F;
        matrix[9] = 0.0F;
        matrix[10] = 1.0F;
        matrix[11] = 0.0F;
        matrix[12] = 0.0F;
        matrix[13] = 0.0F;
        matrix[14] = 0.0F;
        matrix[15] = 1.0F;
    }

    public void zeroMatrix() {
        matrix[0] = 0.0F;
        matrix[1] = 0.0F;
        matrix[2] = 0.0F;
        matrix[3] = 0.0F;
        matrix[4] = 0.0F;
        matrix[5] = 0.0F;
        matrix[6] = 0.0F;
        matrix[7] = 0.0F;
        matrix[8] = 0.0F;
        matrix[9] = 0.0F;
        matrix[10] = 0.0F;
        matrix[11] = 0.0F;
        matrix[12] = 0.0F;
        matrix[13] = 0.0F;
        matrix[14] = 0.0F;
        matrix[15] = 0.0F;
    }

    public void copy(BoneTransform var1) {
        System.arraycopy(var1.matrix, 0, matrix, 0, 16);
    }

    public void setUniformScale(float scale) {
        this.setScale(scale, scale, scale);
    }

    public void setScale(float scaleX, float scaleY, float scaleZ) {
        identityMatrix();
        matrix[0] = scaleX;
        matrix[5] = scaleY;
        matrix[10] = scaleZ;
    }

    public void addTransform(BoneTransform transform) {
        for (int i = 0; i < matrix.length; ++i)
            matrix[i] += transform.matrix[i];
    }

    public void combine(BoneTransform transform) {
        float var2 = transform.matrix[0] * matrix[0] + transform.matrix[4] * matrix[1] + transform.matrix[8] * matrix[2] + transform.matrix[12] * matrix[3];
        float var3 = matrix[3] * transform.matrix[13] + matrix[1] * transform.matrix[5] + matrix[0] * transform.matrix[1] + transform.matrix[9] * matrix[2];
        float var4 = matrix[3] * transform.matrix[14] + transform.matrix[2] * matrix[0] + transform.matrix[6] * matrix[1] + matrix[2] * transform.matrix[10];
        float var5 = matrix[3] * transform.matrix[15] + matrix[2] * transform.matrix[11] + matrix[0] * transform.matrix[3] + transform.matrix[7] * matrix[1];
        float var6 = matrix[7] * transform.matrix[12] + matrix[6] * transform.matrix[8] + transform.matrix[0] * matrix[4] + matrix[5] * transform.matrix[4];
        float var7 = matrix[5] * transform.matrix[5] + matrix[4] * transform.matrix[1] + transform.matrix[9] * matrix[6] + matrix[7] * transform.matrix[13];
        float var8 = transform.matrix[14] * matrix[7] + transform.matrix[10] * matrix[6] + transform.matrix[6] * matrix[5] + transform.matrix[2] * matrix[4];
        float var9 = transform.matrix[11] * matrix[6] + matrix[5] * transform.matrix[7] + matrix[4] * transform.matrix[3] + matrix[7] * transform.matrix[15];
        float var10 = matrix[9] * transform.matrix[4] + transform.matrix[0] * matrix[8] + matrix[10] * transform.matrix[8] + matrix[11] * transform.matrix[12];
        float var11 = transform.matrix[13] * matrix[11] + matrix[10] * transform.matrix[9] + transform.matrix[5] * matrix[9] + transform.matrix[1] * matrix[8];
        float var12 = matrix[11] * transform.matrix[14] + transform.matrix[6] * matrix[9] + transform.matrix[2] * matrix[8] + matrix[10] * transform.matrix[10];
        float var13 = transform.matrix[15] * matrix[11] + transform.matrix[3] * matrix[8] + matrix[9] * transform.matrix[7] + matrix[10] * transform.matrix[11];
        float var14 = matrix[14] * transform.matrix[8] + matrix[12] * transform.matrix[0] + matrix[13] * transform.matrix[4] + transform.matrix[12] * matrix[15];
        float var15 = transform.matrix[13] * matrix[15] + matrix[12] * transform.matrix[1] + matrix[13] * transform.matrix[5] + transform.matrix[9] * matrix[14];
        float var16 = matrix[15] * transform.matrix[14] + transform.matrix[2] * matrix[12] + transform.matrix[6] * matrix[13] + matrix[14] * transform.matrix[10];
        float var17 = matrix[15] * transform.matrix[15] + matrix[14] * transform.matrix[11] + matrix[13] * transform.matrix[7] + matrix[12] * transform.matrix[3];
        matrix[0] = var2;
        matrix[1] = var3;
        matrix[2] = var4;
        matrix[3] = var5;
        matrix[4] = var6;
        matrix[5] = var7;
        matrix[6] = var8;
        matrix[7] = var9;
        matrix[8] = var10;
        matrix[9] = var11;
        matrix[10] = var12;
        matrix[11] = var13;
        matrix[12] = var14;
        matrix[13] = var15;
        matrix[14] = var16;
        matrix[15] = var17;
    }

    public void applyQuaternion(Quaternion quaternion) {
        float var2 = quaternion.qW * quaternion.qW;
        float var3 = quaternion.qX * quaternion.qW;
        float var4 = quaternion.qW * quaternion.qY;
        float var5 = quaternion.qZ * quaternion.qW;
        float var6 = quaternion.qX * quaternion.qX;
        float var7 = quaternion.qY * quaternion.qX;
        float var8 = quaternion.qX * quaternion.qZ;
        float var9 = quaternion.qY * quaternion.qY;
        float var10 = quaternion.qZ * quaternion.qY;
        float var11 = quaternion.qZ * quaternion.qZ;
        matrix[0] = var2 + var6 - var11 - var9;
        matrix[1] = var5 + var5 + var7 + var7;
        matrix[2] = var8 - var4 - var4 + var8;
        matrix[4] = var7 + (var7 - var5 - var5);
        matrix[5] = var2 + var9 - var6 - var11;
        matrix[6] = var10 + var3 + var10 + var3;
        matrix[8] = var4 + var8 + var8 + var4;
        matrix[9] = var10 - var3 - var3 + var10;
        matrix[10] = var2 + var11 - var9 - var6;
    }

    void applyTransformation(TransformationMatrix other) {
        matrix[0] = other.scaleX;
        matrix[1] = other.skewYX;
        matrix[2] = other.skewZX;
        matrix[3] = 0.0F;
        matrix[4] = other.skewXY;
        matrix[5] = other.scaleY;
        matrix[6] = other.skewZY;
        matrix[7] = 0.0F;
        matrix[8] = other.skewXZ;
        matrix[9] = other.skewYZ;
        matrix[10] = other.scaleZ;
        matrix[11] = 0.0F;
        matrix[12] = other.translateX;
        matrix[13] = other.translateY;
        matrix[14] = other.translateZ;
        matrix[15] = 1.0F;
    }

    float calculateDeterminant() {
        return matrix[8] * matrix[5] * matrix[3] * matrix[14] + matrix[13] * matrix[10] * matrix[3] * matrix[4] + (matrix[8] * matrix[1] * matrix[6] * matrix[15] + matrix[14] * matrix[1] * matrix[4] * matrix[11] + (matrix[14] * matrix[9] * matrix[0] * matrix[7] + matrix[11] * matrix[6] * matrix[0] * matrix[13] + (matrix[15] * matrix[10] * matrix[5] * matrix[0] - matrix[11] * matrix[5] * matrix[0] * matrix[14] - matrix[15] * matrix[9] * matrix[0] * matrix[6]) - matrix[0] * matrix[7] * matrix[10] * matrix[13] - matrix[1] * matrix[4] * matrix[10] * matrix[15]) - matrix[12] * matrix[6] * matrix[1] * matrix[11] - matrix[8] * matrix[1] * matrix[7] * matrix[14] + matrix[12] * matrix[10] * matrix[7] * matrix[1] + matrix[2] * matrix[4] * matrix[9] * matrix[15] - matrix[13] * matrix[11] * matrix[4] * matrix[2] - matrix[2] * matrix[5] * matrix[8] * matrix[15] + matrix[12] * matrix[11] * matrix[5] * matrix[2] + matrix[13] * matrix[2] * matrix[7] * matrix[8] - matrix[7] * matrix[2] * matrix[9] * matrix[12] - matrix[14] * matrix[4] * matrix[3] * matrix[9]) - matrix[3] * matrix[5] * matrix[10] * matrix[12] - matrix[13] * matrix[3] * matrix[6] * matrix[8] + matrix[9] * matrix[6] * matrix[3] * matrix[12];
    }

    public void normalize() {
        float var1 = 1.0F / this.calculateDeterminant();
        float var2 = var1 * (matrix[13] * matrix[6] * matrix[11] + (matrix[10] * matrix[5] * matrix[15] - matrix[14] * matrix[11] * matrix[5] - matrix[6] * matrix[9] * matrix[15]) + matrix[9] * matrix[7] * matrix[14] - matrix[13] * matrix[10] * matrix[7]);
        float var3 = var1 * (matrix[13] * matrix[10] * matrix[3] + (matrix[10] * -matrix[1] * matrix[15] + matrix[14] * matrix[11] * matrix[1] + matrix[9] * matrix[2] * matrix[15] - matrix[13] * matrix[11] * matrix[2] - matrix[3] * matrix[9] * matrix[14]));
        float var4 = (matrix[15] * matrix[1] * matrix[6] - matrix[14] * matrix[7] * matrix[1] - matrix[2] * matrix[5] * matrix[15] + matrix[2] * matrix[7] * matrix[13] + matrix[5] * matrix[3] * matrix[14] - matrix[13] * matrix[6] * matrix[3]) * var1;
        float var5 = (matrix[9] * matrix[6] * matrix[3] + (matrix[2] * matrix[5] * matrix[11] + matrix[1] * matrix[7] * matrix[10] + matrix[11] * matrix[6] * -matrix[1] - matrix[9] * matrix[7] * matrix[2] - matrix[3] * matrix[5] * matrix[10])) * var1;
        float var6 = (matrix[6] * matrix[8] * matrix[15] + matrix[15] * -matrix[4] * matrix[10] + matrix[14] * matrix[4] * matrix[11] - matrix[12] * matrix[11] * matrix[6] - matrix[7] * matrix[8] * matrix[14] + matrix[12] * matrix[10] * matrix[7]) * var1;
        float var7 = var1 * (matrix[8] * matrix[3] * matrix[14] + matrix[2] * matrix[11] * matrix[12] + (matrix[10] * matrix[0] * matrix[15] - matrix[0] * matrix[11] * matrix[14] - matrix[15] * matrix[8] * matrix[2]) - matrix[3] * matrix[10] * matrix[12]);
        float var8 = var1 * (matrix[2] * matrix[4] * matrix[15] + matrix[15] * matrix[6] * -matrix[0] + matrix[7] * matrix[0] * matrix[14] - matrix[12] * matrix[7] * matrix[2] - matrix[14] * matrix[3] * matrix[4] + matrix[3] * matrix[6] * matrix[12]);
        float var9 = (matrix[4] * matrix[3] * matrix[10] + matrix[11] * matrix[0] * matrix[6] - matrix[10] * matrix[7] * matrix[0] - matrix[11] * matrix[4] * matrix[2] + matrix[8] * matrix[2] * matrix[7] - matrix[8] * matrix[3] * matrix[6]) * var1;
        float var10 = (matrix[13] * matrix[7] * matrix[8] + matrix[11] * matrix[5] * matrix[12] + (matrix[9] * matrix[4] * matrix[15] - matrix[4] * matrix[11] * matrix[13] - matrix[15] * matrix[5] * matrix[8]) - matrix[12] * matrix[7] * matrix[9]) * var1;
        float var11 = (matrix[3] * matrix[9] * matrix[12] + (matrix[8] * matrix[1] * matrix[15] + matrix[9] * -matrix[0] * matrix[15] + matrix[11] * matrix[0] * matrix[13] - matrix[1] * matrix[11] * matrix[12] - matrix[13] * matrix[3] * matrix[8])) * var1;
        float var12 = (matrix[15] * matrix[5] * matrix[0] - matrix[0] * matrix[7] * matrix[13] - matrix[1] * matrix[4] * matrix[15] + matrix[1] * matrix[7] * matrix[12] + matrix[13] * matrix[4] * matrix[3] - matrix[12] * matrix[5] * matrix[3]) * var1;
        float var13 = var1 * (matrix[3] * matrix[5] * matrix[8] + (matrix[11] * matrix[4] * matrix[1] + -matrix[0] * matrix[5] * matrix[11] + matrix[7] * matrix[0] * matrix[9] - matrix[1] * matrix[7] * matrix[8] - matrix[3] * matrix[4] * matrix[9]));
        float var14 = var1 * (matrix[14] * matrix[9] * -matrix[4] + matrix[13] * matrix[4] * matrix[10] + matrix[8] * matrix[5] * matrix[14] - matrix[12] * matrix[10] * matrix[5] - matrix[8] * matrix[6] * matrix[13] + matrix[12] * matrix[9] * matrix[6]);
        float var15 = (matrix[14] * matrix[0] * matrix[9] - matrix[13] * matrix[0] * matrix[10] - matrix[14] * matrix[8] * matrix[1] + matrix[12] * matrix[1] * matrix[10] + matrix[2] * matrix[8] * matrix[13] - matrix[12] * matrix[2] * matrix[9]) * var1;
        float var16 = var1 * (matrix[14] * matrix[1] * matrix[4] + matrix[5] * -matrix[0] * matrix[14] + matrix[6] * matrix[0] * matrix[13] - matrix[12] * matrix[6] * matrix[1] - matrix[4] * matrix[2] * matrix[13] + matrix[12] * matrix[2] * matrix[5]);
        float var17 = (matrix[9] * matrix[2] * matrix[4] + matrix[1] * matrix[6] * matrix[8] + (matrix[5] * matrix[0] * matrix[10] - matrix[0] * matrix[6] * matrix[9] - matrix[4] * matrix[1] * matrix[10]) - matrix[2] * matrix[5] * matrix[8]) * var1;
        matrix[0] = var2;
        matrix[1] = var3;
        matrix[2] = var4;
        matrix[3] = var5;
        matrix[4] = var6;
        matrix[5] = var7;
        matrix[6] = var8;
        matrix[7] = var9;
        matrix[8] = var10;
        matrix[9] = var11;
        matrix[10] = var12;
        matrix[11] = var13;
        matrix[12] = var14;
        matrix[13] = var15;
        matrix[14] = var16;
        matrix[15] = var17;
    }

    public float[] extractScale() {
        final float[] scales = new float[3];
        final Vector3D scaleX = new Vector3D(matrix[0], matrix[1], matrix[2]);
        final Vector3D scaleY = new Vector3D(matrix[4], matrix[5], matrix[6]);
        final Vector3D scaleZ = new Vector3D(matrix[8], matrix[9], matrix[10]);
        scales[0] = scaleX.magnitude();
        scales[1] = scaleY.magnitude();
        scales[2] = scaleZ.magnitude();
        return scales;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        extractRotation();
        extractEulerAngles();
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                if (j > 0)
                    stringBuilder.append("\t");
                float var4 = matrix[j + i * 4];
                if (Math.sqrt(var4 * var4) < 9.999999747378752E-5)
                    var4 = 0.0F;
                stringBuilder.append(var4);
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    public int hashCode() {
        int var2 = 1;
        var2 = var2 * 31 + Arrays.hashCode(matrix);
        return var2;
    }

    public boolean equals(Object other) {
        if (!(other instanceof BoneTransform otherTransform))
            return false;
        else {
            for (int i = 0; i < 16; ++i)
                if (otherTransform.matrix[i] != matrix[i])
                    return false;
            return true;
        }
    }
}
