package jagex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.displee.cache.index.Index;
import com.displee.cache.index.archive.Archive;
import com.displee.cache.index.archive.file.File;

public class MayaAnimation {

    int id;
    public MayaAnimationFrame[][] primaryFrames = null;
    public MayaAnimationFrame[][] secondaryFrames = null;
    public Skeleton skeleton;
    int totalDuration = 0;
    boolean hasTransformations;
    Future<Object> animationLoadTask;
    List<Future<Object>> frameLoadTask;

    public static MayaAnimation load(Index seqs, Index frames, int mayaAnimationId, boolean fileIndexOrGroupIndex) throws IOException {
        boolean var4 = true;
        final Archive seqArchive = seqs.archive(mayaAnimationId >> 16 & '\uffff');
        final File seqFile = seqArchive.file(mayaAnimationId & '\uffff');

        byte[] mayaAnimationData = seqFile.getData();
        int mayaAnimationFrameGroupId = (mayaAnimationData[1] & 255) << 8 | mayaAnimationData[2] & 255;
        byte[] mayaAnimationFrameGroupData;
        if (fileIndexOrGroupIndex) {

            final Archive frameArchive = frames.archive(0);
            mayaAnimationFrameGroupData = frameArchive.file(mayaAnimationFrameGroupId).getData();
        } else {
            final Archive frameArchive = frames.archive(mayaAnimationFrameGroupId);
            mayaAnimationFrameGroupData = frameArchive.file(0).getData();
        }

        if (mayaAnimationFrameGroupData == null)
            var4 = false;

        if (!var4) {
            return null;
        } else {
            if (MayaAnimationLoadThreadFactory.threadPoolExecutor == null) {
                MayaAnimationLoadThreadFactory.threadPoolExecutorThreadCount = Runtime.getRuntime().availableProcessors();
                MayaAnimationLoadThreadFactory.threadPoolExecutor = new ThreadPoolExecutor(0, MayaAnimationLoadThreadFactory.threadPoolExecutorThreadCount, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue(MayaAnimationLoadThreadFactory.threadPoolExecutorThreadCount * 100 + 100), new MayaAnimationLoadThreadFactory());
            }

            try {
                return new MayaAnimation(seqs, frames, mayaAnimationId, fileIndexOrGroupIndex);
            } catch (Exception var9) {
                var9.printStackTrace();
                return null;
            }
        }
    }

    MayaAnimation(Index seqs, Index frames, int mayaAnimationId, boolean fileIndexOrGroupIndex) throws IOException {
        this.id = mayaAnimationId;

        final Archive seqArchive = seqs.archive(id >> 16 & '\uffff');
        final File seqFile = seqArchive.file(id & '\uffff');

        byte[] data = seqFile.getData();
        Buffer buffer = new Buffer(data);
        int version = buffer.readUnsignedByte();
        int frameGroupId = buffer.readUnsignedShort();
        byte[] frameGroupData;
        if (fileIndexOrGroupIndex) {
            final Archive frameArchive = frames.archive(0);
            frameGroupData = frameArchive.file(frameGroupId).getData();
        } else {
            final Archive frameArchive = frames.archive(frameGroupId);
            frameGroupData = frameArchive.file(0).getData();
        }

        this.skeleton = new Skeleton(frameGroupId, frameGroupData);
        this.frameLoadTask = new ArrayList<>();
        this.animationLoadTask = MayaAnimationLoadThreadFactory.threadPoolExecutor.submit(new MayaAnimationLoadTask(this, buffer, version));
    }

    void read(Buffer buffer, int version) {
        buffer.readUnsignedShort();
        buffer.readUnsignedShort();
        this.totalDuration = buffer.readUnsignedByte();
        final int frameCount = buffer.readUnsignedShort();
        this.secondaryFrames = new MayaAnimationFrame[skeleton.getMayaAnimationSkeleton().getBoneCount()][];
        this.primaryFrames = new MayaAnimationFrame[skeleton.getCount()][];
        final MayaAnimationFrameData[] frameData = new MayaAnimationFrameData[frameCount];

        int i;
        int frameType;
        int index;
        for (i = 0; i < frameCount; ++i) {
            frameType = buffer.readUnsignedByte();
            final MayaAnimationFrameType[] animationTypes = new MayaAnimationFrameType[]{MayaAnimationFrameType.DEFAULT, MayaAnimationFrameType.USE_SECONDARY_FRAMES, MayaAnimationFrameType.field1555, MayaAnimationFrameType.field1550, MayaAnimationFrameType.TRANSFORMATION, MayaAnimationFrameType.field1551};
            MayaAnimationFrameType animationType = (MayaAnimationFrameType) RSEnum.findEnumerated(animationTypes, frameType);
            if (animationType == null) {
                animationType = MayaAnimationFrameType.DEFAULT;
            }

            index = buffer.readShortSmart();
            final int frameFlag = buffer.readUnsignedByte();
            MayaAnimationFrameFlag flag = (MayaAnimationFrameFlag) RSEnum.findEnumerated(MayaAnimationFrameFlag.values(), frameFlag);
            if (flag == null) {
                flag = MayaAnimationFrameFlag.NORMAL;
            }

            final MayaAnimationFrame animationFrame = new MayaAnimationFrame();
            animationFrame.read(buffer, version);
            frameData[i] = new MayaAnimationFrameData(this, animationFrame, animationType, flag, index);
            final int maxIndex = animationType.getMaxIndex();
            final MayaAnimationFrame[][] frames = animationType == MayaAnimationFrameType.USE_SECONDARY_FRAMES
                    ? secondaryFrames
                    : primaryFrames;
            if (frames[index] == null)
                frames[index] = new MayaAnimationFrame[maxIndex];
            if (animationType == MayaAnimationFrameType.TRANSFORMATION)
                hasTransformations = true;
        }

        i = frameCount / MayaAnimationLoadThreadFactory.threadPoolExecutorThreadCount;
        int remainder = frameCount % MayaAnimationLoadThreadFactory.threadPoolExecutorThreadCount;
        int start = 0;

        for (index = 0; index < MayaAnimationLoadThreadFactory.threadPoolExecutorThreadCount; ++index) {
            frameType = start;
            start += i;
            if (remainder > 0) {
                ++start;
                --remainder;
            }

            if (start == frameType)
                break;

            final MayaAnimationLoadFrameTask loadFrameTask = new MayaAnimationLoadFrameTask(this, frameType, start, frameData);
            final Future<Object> loadCompleteFuture = MayaAnimationLoadThreadFactory.threadPoolExecutor.submit(loadFrameTask);
            frameLoadTask.add(loadCompleteFuture);
        }
    }

    public boolean isAnimationLoaded() {
        if (animationLoadTask == null && frameLoadTask == null)
            return true;
        else {
            if (animationLoadTask == null || animationLoadTask.isDone())
                animationLoadTask = null;
            else
                return false;
            frameLoadTask.removeIf(Future::isDone);
            if (frameLoadTask.isEmpty()) {
                frameLoadTask = null;
                return true;
            } else
                return false;
        }
    }

    public int getDuration() {
        return totalDuration;
    }

    public boolean hasTransformations() {
        return hasTransformations;
    }

    public void apply(int animationStep, Bone bone, int frameIndex) {
        final BoneTransform boneTransform = BoneTransform.pool();
        calculateBoneTransform(boneTransform, frameIndex, bone, animationStep);
        adjustBonePosition(boneTransform, frameIndex, bone, animationStep);
        adjustBoneScale(boneTransform, frameIndex, bone, animationStep);

        bone.setBaseTransform(boneTransform);

        boneTransform.release();
    }

    void calculateBoneTransform(BoneTransform boneTransform, int frameIndex, Bone bone, int animationStep) {
        final float[] rotationAngles = bone.getInverseBindMatrix(this.totalDuration);
        float xRotation = rotationAngles[0];
        float yRotation = rotationAngles[1];
        float zRotation = rotationAngles[2];
        if (secondaryFrames[frameIndex] != null) {
            final MayaAnimationFrame xFrame = secondaryFrames[frameIndex][0];
            final MayaAnimationFrame yFrame = secondaryFrames[frameIndex][1];
            final MayaAnimationFrame zFrame = secondaryFrames[frameIndex][2];
            if (xFrame != null) xRotation = xFrame.evaluate(animationStep);
            if (yFrame != null) yRotation = yFrame.evaluate(animationStep);
            if (zFrame != null) zRotation = zFrame.evaluate(animationStep);
        }

        final Quaternion quaternionX = Quaternion.pool();
        quaternionX.setFromAxisAngle(1.0F, 0.0F, 0.0F, xRotation);

        final Quaternion quaternionY = Quaternion.pool();
        quaternionY.setFromAxisAngle(0.0F, 1.0F, 0.0F, yRotation);

        final Quaternion quaternionZ = Quaternion.pool();
        quaternionZ.setFromAxisAngle(0.0F, 0.0F, 1.0F, zRotation);

        final Quaternion combinedQuaternion = Quaternion.pool();
        combinedQuaternion.multiply(quaternionZ);
        combinedQuaternion.multiply(quaternionX);
        combinedQuaternion.multiply(quaternionY);

        final BoneTransform temporaryTransform = BoneTransform.pool();
        temporaryTransform.applyQuaternion(combinedQuaternion);

        boneTransform.combine(temporaryTransform);

        quaternionX.release();
        quaternionY.release();
        quaternionZ.release();
        combinedQuaternion.release();
        temporaryTransform.release();
    }

    void adjustBoneScale(BoneTransform transform, int frameIndex, Bone bone, int animationStep) {
        final float[] scaleFactors = bone.getBindMatrix(this.totalDuration);
        float scaleX = scaleFactors[0];
        float scaleY = scaleFactors[1];
        float scaleZ = scaleFactors[2];
        if (secondaryFrames[frameIndex] != null) {
            final MayaAnimationFrame scaleFrameX = secondaryFrames[frameIndex][3];
            final MayaAnimationFrame scaleFrameY = secondaryFrames[frameIndex][4];
            final MayaAnimationFrame scaleFrameZ = secondaryFrames[frameIndex][5];
            if (scaleFrameX != null) scaleX = scaleFrameX.evaluate(animationStep);
            if (scaleFrameY != null) scaleY = scaleFrameY.evaluate(animationStep);
            if (scaleFrameZ != null) scaleZ = scaleFrameZ.evaluate(animationStep);
        }
        transform.matrix[12] = scaleX;
        transform.matrix[13] = scaleY;
        transform.matrix[14] = scaleZ;
    }

    void adjustBonePosition(BoneTransform transform, int frameIndex, Bone bone, int animationStep) {
        final float[] position = bone.getPosition(this.totalDuration);
        float positionX = position[0];
        float positionY = position[1];
        float positionZ = position[2];
        if (secondaryFrames[frameIndex] != null) {
            final MayaAnimationFrame posXFrame = secondaryFrames[frameIndex][6];
            final MayaAnimationFrame posYFrame = secondaryFrames[frameIndex][7];
            final MayaAnimationFrame posZFrame = secondaryFrames[frameIndex][8];
            if (posXFrame != null) positionX = posXFrame.evaluate(animationStep);
            if (posYFrame != null) positionY = posYFrame.evaluate(animationStep);
            if (posZFrame != null) positionZ = posZFrame.evaluate(animationStep);
        }

        final BoneTransform positionTransform = BoneTransform.pool();
        positionTransform.setScale(positionX, positionY, positionZ);

        transform.combine(positionTransform);

        positionTransform.release();
    }
}
