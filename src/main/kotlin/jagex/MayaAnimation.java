package jagex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.ArchiveFiles;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite;

public class MayaAnimation  {
   int id;
   public MayaAnimationFrame[][] primaryFrames = null;
   public MayaAnimationFrame[][] secondaryFrames = null;
   public Skeleton skeleton;
   int totalDuration = 0;
   boolean hasTransformations;
   Future<Object> animationLoadTask;
   List<Object> frameLoadTask;

   public static MayaAnimation loadMayaAnimation(Index seqs, Index frames, int mayaAnimationId, boolean fileIndexOrGroupIndex) throws IOException {
      boolean var4 = true;
      final Archive seqArchive = seqs.getArchive(mayaAnimationId >> 16 & '\uffff');
      final byte[] seqData = OldschoolCacheRuneLite.INSTANCE.getStore().getStorage().loadArchive(seqArchive);
      final ArchiveFiles seqFiles = seqArchive.getFiles(seqData);
      final FSFile seqFile = seqFiles.findFile(mayaAnimationId & '\uffff');

      byte[] mayaAnimationData = seqFile.getContents();
      int mayaAnimationFrameGroupId = (mayaAnimationData[1] & 255) << 8 | mayaAnimationData[2] & 255;
      byte[] mayaAnimationFrameGroupData;
      if (fileIndexOrGroupIndex) {

         final Archive frameArchive = frames.getArchive(0);
         final byte[] frameData = OldschoolCacheRuneLite.INSTANCE.getStore().getStorage().loadArchive(frameArchive);
         final ArchiveFiles frameFiles = frameArchive.getFiles(frameData);
         mayaAnimationFrameGroupData = frameFiles.findFile(mayaAnimationFrameGroupId).getContents();
      } else {
         final Archive frameArchive = frames.getArchive(mayaAnimationFrameGroupId);
         final byte[] frameData = OldschoolCacheRuneLite.INSTANCE.getStore().getStorage().loadArchive(frameArchive);
         final ArchiveFiles frameFiles = frameArchive.getFiles(frameData);
         mayaAnimationFrameGroupData = frameFiles.findFile(0).getContents();
      }

      if (mayaAnimationFrameGroupData == null) {
         var4 = false;
      }

      if (!var4) {
         return null;
      } else {
         if (class277.threadPoolExecutor == null) {
            class461.threadPoolExecutorThreadCount = Runtime.getRuntime().availableProcessors();
            class277.threadPoolExecutor = new ThreadPoolExecutor(0, class461.threadPoolExecutorThreadCount, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue(class461.threadPoolExecutorThreadCount * 100 + 100), new class130());
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

      final Archive seqArchive = seqs.getArchive(this.id >> 16 & '\uffff');
      final byte[] seqData = OldschoolCacheRuneLite.INSTANCE.getStore().getStorage().loadArchive(seqArchive);
      final ArchiveFiles seqFiles = seqArchive.getFiles(seqData);
      final FSFile seqFile = seqFiles.findFile(this.id & '\uffff');

      byte[] data = seqFile.getContents();
      Buffer buffer = new Buffer(data);
      int version = buffer.readUnsignedByte();
      int frameGroupId = buffer.readUnsignedShort();
      byte[] frameGroupData;
      if (fileIndexOrGroupIndex) {
         final Archive frameArchive = frames.getArchive(0);
         final byte[] frameData = OldschoolCacheRuneLite.INSTANCE.getStore().getStorage().loadArchive(frameArchive);
         final ArchiveFiles frameFiles = frameArchive.getFiles(frameData);
         frameGroupData = frameFiles.findFile(frameGroupId).getContents();
      } else {
         final Archive frameArchive = frames.getArchive(frameGroupId);
         final byte[] frameData = OldschoolCacheRuneLite.INSTANCE.getStore().getStorage().loadArchive(frameArchive);
         final ArchiveFiles frameFiles = frameArchive.getFiles(frameData);
         frameGroupData = frameFiles.findFile(0).getContents();
      }

      this.skeleton = new Skeleton(frameGroupId, frameGroupData);
      this.frameLoadTask = new ArrayList<>();
      this.animationLoadTask = class277.threadPoolExecutor.submit(new MayaAnimationLoadTask(this, buffer, version));
   }

   void read(Buffer buffer, int version) {
      buffer.readUnsignedShort();
      buffer.readUnsignedShort();
      this.totalDuration = buffer.readUnsignedByte();
      int frameCount = buffer.readUnsignedShort();
      this.secondaryFrames = new MayaAnimationFrame[this.skeleton.getMayaAnimationSkeleton().frameCount()][];
      this.primaryFrames = new MayaAnimationFrame[this.skeleton.getCount()][];
      MayaAnimationFrameData[] frameData = new MayaAnimationFrameData[frameCount];

      int i;
      int frameType;
      int index;
      for(i = 0; i < frameCount; ++i) {
         frameType = buffer.readUnsignedByte();
         MayaAnimationFrameType[] animationTypes = new MayaAnimationFrameType[]{MayaAnimationFrameType.DEFAULT, MayaAnimationFrameType.field1548, MayaAnimationFrameType.field1555, MayaAnimationFrameType.field1550, MayaAnimationFrameType.TRANSFORMATION, MayaAnimationFrameType.field1551};
         MayaAnimationFrameType animationType = (MayaAnimationFrameType)class4.findEnumerated(animationTypes, frameType);
         if (animationType == null) {
            animationType = MayaAnimationFrameType.DEFAULT;
         }

         index = buffer.readShortSmart();
         int frameFlag = buffer.readUnsignedByte();
         MayaAnimationFrameFlag flag = (MayaAnimationFrameFlag)class4.findEnumerated(MayaAnimationFrameFlag.values(), frameFlag);
         if (flag == null) {
            flag = MayaAnimationFrameFlag.NORMAL;
         }

         MayaAnimationFrame animationFrame = new MayaAnimationFrame();
         animationFrame.read(buffer, version);
         frameData[i] = new MayaAnimationFrameData(this, animationFrame, animationType, flag, index);
         int maxIndex = animationType.getMaxIndex();
         MayaAnimationFrame[][] frames;
         if (animationType == MayaAnimationFrameType.field1548) {
            frames = this.secondaryFrames;
         } else {
            frames = this.primaryFrames;
         }

         if (frames[index] == null) {
            frames[index] = new MayaAnimationFrame[maxIndex];
         }

         if (animationType == MayaAnimationFrameType.TRANSFORMATION) {
            this.hasTransformations = true;
         }
      }

      i = frameCount / class461.threadPoolExecutorThreadCount;
      int remainder = frameCount % class461.threadPoolExecutorThreadCount;
      int start = 0;

      for(index = 0; index < class461.threadPoolExecutorThreadCount; ++index) {
         frameType = start;
         start += i;
         if (remainder > 0) {
            ++start;
            --remainder;
         }

         if (start == frameType) {
            break;
         }

         this.frameLoadTask.add(class277.threadPoolExecutor.submit(new MayaAnimationLoadFrameTask(this, frameType, start, frameData)));
      }
   }

   public boolean isAnimationLoaded() {
      if (this.animationLoadTask == null && this.frameLoadTask == null) {
         return true;
      } else {
         if (this.animationLoadTask != null) {
            if (!this.animationLoadTask.isDone()) {
               return false;
            }

            this.animationLoadTask = null;
         }

         boolean allTasksCompleted = true;

         for(int var2 = 0; var2 < this.frameLoadTask.size(); ++var2) {
            if (!((Future)this.frameLoadTask.get(var2)).isDone()) {
               allTasksCompleted = false;
            } else {
               this.frameLoadTask.remove(var2);
               --var2;
            }
         }

         if (!allTasksCompleted) {
            return false;
         } else {
            this.frameLoadTask = null;
            return true;
         }
      }
   }

   public int getDuration() {
      return this.totalDuration;
   }

   public boolean hasTransformations() {
      return this.hasTransformations;
   }

   public void apply(int animationStep, Bone bone, int frameIndex, int var4) {
      BoneTransform boneTransform;
      synchronized(BoneTransform.classPool) {
         if (BoneTransform.poolSize == 0) {
            boneTransform = new BoneTransform();
         } else {
            BoneTransform.classPool[--BoneTransform.poolSize].identityMatrix();
            boneTransform = BoneTransform.classPool[BoneTransform.poolSize];
         }
      }

      this.calculateBoneTransform(boneTransform, frameIndex, bone, animationStep);
      this.adjustBonePosition(boneTransform, frameIndex, bone, animationStep);
      this.adjustBoneScale(boneTransform, frameIndex, bone, animationStep);
      bone.setBaseTransform(boneTransform);
      boneTransform.release();
   }

   void calculateBoneTransform(BoneTransform boneTransform, int frameIndex, Bone bone, int animationStep) {
      float[] rotationAnglex = bone.getInverseBindMatrix(this.totalDuration);
      float xRotation = rotationAnglex[0];
      float yRotation = rotationAnglex[1];
      float zRotation = rotationAnglex[2];
      if (this.secondaryFrames[frameIndex] != null) {
         MayaAnimationFrame xFrame = this.secondaryFrames[frameIndex][0];
         MayaAnimationFrame yFrame = this.secondaryFrames[frameIndex][1];
         MayaAnimationFrame zFrame = this.secondaryFrames[frameIndex][2];
         if (xFrame != null) {
            xRotation = xFrame.evaluate(animationStep);
         }

         if (yFrame != null) {
            yRotation = yFrame.evaluate(animationStep);
         }

         if (zFrame != null) {
            zRotation = zFrame.evaluate(animationStep);
         }
      }

      Quaternion quaternionX;
      synchronized(Quaternion.pool) {
         if (Quaternion.poolSize == 0) {
            quaternionX = new Quaternion();
         } else {
            Quaternion.pool[--Quaternion.poolSize].reset();
            quaternionX = Quaternion.pool[Quaternion.poolSize];
         }
      }

      quaternionX.setFromAxisAngle(1.0F, 0.0F, 0.0F, xRotation);
      Quaternion quaternionY;
      synchronized(Quaternion.pool) {
         if (Quaternion.poolSize == 0) {
            quaternionY = new Quaternion();
         } else {
            Quaternion.pool[--Quaternion.poolSize].reset();
            quaternionY = Quaternion.pool[Quaternion.poolSize];
         }
      }

      quaternionY.setFromAxisAngle(0.0F, 1.0F, 0.0F, yRotation);
      Quaternion quaternionZ;
      synchronized(Quaternion.pool) {
         if (Quaternion.poolSize == 0) {
            quaternionZ = new Quaternion();
         } else {
            Quaternion.pool[--Quaternion.poolSize].reset();
            quaternionZ = Quaternion.pool[Quaternion.poolSize];
         }
      }

      quaternionZ.setFromAxisAngle(0.0F, 0.0F, 1.0F, zRotation);
      Quaternion combinedQuaternion;
      synchronized(Quaternion.pool) {
         if (Quaternion.poolSize == 0) {
            combinedQuaternion = new Quaternion();
         } else {
            Quaternion.pool[--Quaternion.poolSize].reset();
            combinedQuaternion = Quaternion.pool[Quaternion.poolSize];
         }
      }

      combinedQuaternion.multiply(quaternionZ);
      combinedQuaternion.multiply(quaternionX);
      combinedQuaternion.multiply(quaternionY);
      BoneTransform temporaryTransform;
      synchronized(BoneTransform.classPool) {
         if (BoneTransform.poolSize == 0) {
            temporaryTransform = new BoneTransform();
         } else {
            BoneTransform.classPool[--BoneTransform.poolSize].identityMatrix();
            temporaryTransform = BoneTransform.classPool[BoneTransform.poolSize];
         }
      }

      temporaryTransform.applyQuaternion(combinedQuaternion);
      boneTransform.combine(temporaryTransform);
      quaternionX.release();
      quaternionY.release();
      quaternionZ.release();
      combinedQuaternion.release();
      temporaryTransform.release();
   }

   void adjustBoneScale(BoneTransform transform, int frameIndex, Bone bone, int animationStep) {
      float[] scaleFactors = bone.getBindMatrix(this.totalDuration);
      float scaleX = scaleFactors[0];
      float scaleY = scaleFactors[1];
      float scaleZ = scaleFactors[2];
      if (this.secondaryFrames[frameIndex] != null) {
         MayaAnimationFrame scaleFrameX = this.secondaryFrames[frameIndex][3];
         MayaAnimationFrame scaleFrameY = this.secondaryFrames[frameIndex][4];
         MayaAnimationFrame scaleFrameZ = this.secondaryFrames[frameIndex][5];
         if (scaleFrameX != null) {
            scaleX = scaleFrameX.evaluate(animationStep);
         }

         if (scaleFrameY != null) {
            scaleY = scaleFrameY.evaluate(animationStep);
         }

         if (scaleFrameZ != null) {
            scaleZ = scaleFrameZ.evaluate(animationStep);
         }
      }

      transform.matrix[12] = scaleX;
      transform.matrix[13] = scaleY;
      transform.matrix[14] = scaleZ;
   }

   void adjustBonePosition(BoneTransform transform, int frameIndex, Bone bone, int animationStep) {
      float[] position = bone.getPosition(this.totalDuration);
      float positionX = position[0];
      float positionY = position[1];
      float positionZ = position[2];
      if (this.secondaryFrames[frameIndex] != null) {
         MayaAnimationFrame posXFrame = this.secondaryFrames[frameIndex][6];
         MayaAnimationFrame posYFrame = this.secondaryFrames[frameIndex][7];
         MayaAnimationFrame posZFrame = this.secondaryFrames[frameIndex][8];
         if (posXFrame != null) {
            positionX = posXFrame.evaluate(animationStep);
         }

         if (posYFrame != null) {
            positionY = posYFrame.evaluate(animationStep);
         }

         if (posZFrame != null) {
            positionZ = posZFrame.evaluate(animationStep);
         }
      }

      BoneTransform positionTransform;
      synchronized(BoneTransform.classPool) {
         if (BoneTransform.poolSize == 0) {
            positionTransform = new BoneTransform();
         } else {
            BoneTransform.classPool[--BoneTransform.poolSize].identityMatrix();
            positionTransform = BoneTransform.classPool[BoneTransform.poolSize];
         }
      }

      positionTransform.setScale(positionX, positionY, positionZ);
      transform.combine(positionTransform);
      positionTransform.release();
   }
}
