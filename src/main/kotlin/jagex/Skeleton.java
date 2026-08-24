package jagex;

public class Skeleton {

    private static final int[] EMPTY_LABELS = new int[0];

    int id;
    int count;
    int[] transformTypes;
    int[][] labels;
    MayaAnimationSkeleton mayaAnimationSkeleton;

    public Skeleton(int skeletonId, byte[] skeletonData) {
        this.id = skeletonId;
        Buffer buffer = new Buffer(skeletonData);
        this.count = buffer.readUnsignedByte();
        this.transformTypes = new int[this.count];
        this.labels = new int[this.count][];

        int i;
        for (i = 0; i < count; ++i)
            transformTypes[i] = buffer.readUnsignedByte();

        for (i = 0; i < count; ++i) {
            int length = buffer.readUnsignedByte();
            labels[i] = length == 0 ? EMPTY_LABELS : new int[length];
        }

        for (i = 0; i < count; ++i)
            for (int j = 0; j < labels[i].length; ++j)
                labels[i][j] = buffer.readUnsignedByte();

        if (buffer.offset < buffer.array.length) {
            final int boneCount = buffer.readUnsignedShort();
            if (boneCount > 0)
                mayaAnimationSkeleton = new MayaAnimationSkeleton(buffer, boneCount);
        }
    }

    public int getId() {
        return id;
    }

    public int getCount() {
        return count;
    }

    public MayaAnimationSkeleton getMayaAnimationSkeleton() {
        return mayaAnimationSkeleton;
    }

    public int[] getTransformTypes() {
        return transformTypes;
    }

    public int[][] getLabels() {
        return labels;
    }

}
