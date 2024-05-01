package jagex;

public class MayaAnimationSkeleton {

    Bone[] bones;
    int boneCount;

    MayaAnimationSkeleton(Buffer buffer, int var2) {
        this.bones = new Bone[var2];
        this.boneCount = buffer.readUnsignedByte();

        for (int var3 = 0; var3 < this.bones.length; ++var3) {
            Bone frame = new Bone(this.boneCount, buffer, false);
            this.bones[var3] = frame;
        }

        this.initializeBones();
    }

    void initializeBones() {
        for (Bone frame : bones) {
            if (frame.index >= 0) {
                frame.parentBone = this.bones[frame.index];
            }
        }
    }

    public int frameCount() {
        return this.bones.length;
    }

    public Bone getBone(int index) {
        return index >= this.frameCount() ? null : this.bones[index];
    }

    public Bone[] getAllBones() {
        return this.bones;
    }

    public void applyAnimation(MayaAnimation animation, int frameIndex) {
        this.applyAnimation(animation, frameIndex, (boolean[]) null, false);
    }

    public void applyAnimation(MayaAnimation animation, int frameIndex, boolean[] affectedBones, boolean inverse) {
        int animLength = animation.getDuration();
        int boneIndex = 0;
        Bone[] frames = this.getAllBones();
        for (Bone frame : frames) {
            if (affectedBones == null || inverse == affectedBones[boneIndex]) {
                animation.apply(frameIndex, frame, boneIndex, animLength);
            }
            ++boneIndex;
        }
    }
}
