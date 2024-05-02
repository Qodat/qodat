package jagex;

public class Skeleton {

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

        for (i = 0; i < count; ++i)
            labels[i] = new int[buffer.readUnsignedByte()];

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

}
