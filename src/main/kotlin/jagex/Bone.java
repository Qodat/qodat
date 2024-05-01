package jagex;

public class Bone {

   public final int index;
   public Bone parentBone;
   float[][] rotations;
   final BoneTransform[] boneTransforms;
   BoneTransform[] computedBoneTransforms;
   BoneTransform[] finalBoneTransforms;
   BoneTransform baseTransform = new BoneTransform();
   boolean baseTransformUpdate = true;
   BoneTransform globalTransform = new BoneTransform();
   boolean globalTransformUpdated = true;
   BoneTransform localTransform = new BoneTransform();
   float[][] inverseBindMatrices;
   float[][] bindMatrices;
   float[][] positions;

   public Bone(int transformCount, Buffer buffer, boolean readTransforms) {
      this.index = buffer.readShort();
      this.boneTransforms = new BoneTransform[transformCount];
      this.computedBoneTransforms = new BoneTransform[this.boneTransforms.length];
      this.finalBoneTransforms = new BoneTransform[this.boneTransforms.length];
      this.rotations = new float[this.boneTransforms.length][3];

      for(int i = 0; i < this.boneTransforms.length; ++i) {
         this.boneTransforms[i] = new BoneTransform(buffer, readTransforms);
         this.rotations[i][0] = buffer.readIntAsFloat();
         this.rotations[i][1] = buffer.readIntAsFloat();
         this.rotations[i][2] = buffer.readIntAsFloat();
      }

      this.updateTransforms();
   }

   void updateTransforms() {
      this.inverseBindMatrices = new float[this.boneTransforms.length][3];
      this.bindMatrices = new float[this.boneTransforms.length][3];
      this.positions = new float[this.boneTransforms.length][3];
      BoneTransform tempTransform;
      synchronized(BoneTransform.classPool) {
         if (BoneTransform.poolSize == 0) {
            tempTransform = new BoneTransform();
         } else {
            BoneTransform.classPool[--BoneTransform.poolSize].identityMatrix();
            tempTransform = BoneTransform.classPool[BoneTransform.poolSize];
         }
      }

      BoneTransform transform = tempTransform;

      for(int i = 0; i < this.boneTransforms.length; ++i) {
         BoneTransform bone = this.getBoneTransform(i);
         transform.copy(bone);
         transform.normalize();
         this.inverseBindMatrices[i] = transform.extractRotation();
         this.bindMatrices[i][0] = bone.matrix[12];
         this.bindMatrices[i][1] = bone.matrix[13];
         this.bindMatrices[i][2] = bone.matrix[14];
         this.positions[i] = bone.extractScale();
      }

      transform.release();
   }

   BoneTransform getBoneTransform(int index) {
      return this.boneTransforms[index];
   }

   BoneTransform getComputedBoneTransform(int index) {
      if (this.computedBoneTransforms[index] == null) {
         this.computedBoneTransforms[index] = new BoneTransform(this.getBoneTransform(index));
         if (this.parentBone != null) {
            this.computedBoneTransforms[index].combine(this.parentBone.getComputedBoneTransform(index));
         } else {
            this.computedBoneTransforms[index].combine(BoneTransform.identity);
         }
      }

      return this.computedBoneTransforms[index];
   }

   BoneTransform getFinalBoneTransform(int index) {
      if (this.finalBoneTransforms[index] == null) {
         this.finalBoneTransforms[index] = new BoneTransform(this.getComputedBoneTransform(index));
         this.finalBoneTransforms[index].normalize();
      }

      return this.finalBoneTransforms[index];
   }

   void setBaseTransform(BoneTransform var1) {
      this.baseTransform.copy(var1);
      this.baseTransformUpdate = true;
      this.globalTransformUpdated = true;
   }

   BoneTransform getBaseTransform() {
      return this.baseTransform;
   }

   BoneTransform getGlobalTransform() {
      if (this.baseTransformUpdate) {
         this.globalTransform.copy(this.getBaseTransform());
         if (this.parentBone != null) {
            this.globalTransform.combine(this.parentBone.getGlobalTransform());
         }

         this.baseTransformUpdate = false;
      }

      return this.globalTransform;
   }

   public BoneTransform getTransform(int index) {
      if (this.globalTransformUpdated) {
         this.localTransform.copy(this.getFinalBoneTransform(index));
         this.localTransform.combine(this.getGlobalTransform());
         this.globalTransformUpdated = false;
      }

      return this.localTransform;
   }

   float[] getInverseBindMatrix(int index) {
      return this.inverseBindMatrices[index];
   }

   float[] getBindMatrix(int index) {
      return this.bindMatrices[index];
   }

   float[] getPosition(int index) {
      return this.positions[index];
   }
}
