package jagex;

public class MayaAnimationSkeleton {

    Bone[] bones;
    int transformCount;

    MayaAnimationSkeleton(Buffer buffer, int boneCount) {
        bones = new Bone[boneCount];
        transformCount = buffer.readUnsignedByte();
        for (int i = 0; i < bones.length; ++i) {
            final Bone frame = new Bone(transformCount, buffer, false);
            bones[i] = frame;
        }
        initializeBones();
    }

    private void initializeBones() {
        for (Bone frame : bones)
            if (frame.index >= 0)
                frame.parentBone = bones[frame.index];
    }


    public void applyAnimation(MayaAnimation animation, int frameIndex) {
        applyAnimation(animation, frameIndex, null, false);
    }

    public void applyAnimation(MayaAnimation animation, int frameIndex, boolean[] affectedBones, boolean inverse) {
        for (int i = 0, bonesLength = bones.length; i < bonesLength; i++) {
            final Bone frame = bones[i];
            if (affectedBones == null || inverse == affectedBones[i])
                animation.apply(frameIndex, frame, i);
        }
    }

    public Bone getBone(int index) {
        return index >= getBoneCount() ? null : bones[index];
    }

    public int getBoneCount() {
        return bones.length;
    }
}
