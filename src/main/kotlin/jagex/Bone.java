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
         boneTransforms[i] = new BoneTransform(buffer, readTransforms);
         rotations[i][0] = buffer.readIntAsFloat();
         rotations[i][1] = buffer.readIntAsFloat();
         rotations[i][2] = buffer.readIntAsFloat();
      }

      updateTransforms();
   }

   void updateTransforms() {
      inverseBindMatrices = new float[boneTransforms.length][3];
      bindMatrices = new float[boneTransforms.length][3];
      positions = new float[boneTransforms.length][3];
      final BoneTransform tempTransform = BoneTransform.pool();
      for(int i = 0; i < boneTransforms.length; ++i) {
         final BoneTransform transform = getBoneTransform(i);
         tempTransform.copy(transform);
         tempTransform.normalize();
         inverseBindMatrices[i] = tempTransform.extractRotation();
         bindMatrices[i][0] = transform.matrix[12];
         bindMatrices[i][1] = transform.matrix[13];
         bindMatrices[i][2] = transform.matrix[14];
         positions[i] = transform.extractScale();
      }
      tempTransform.release();
   }

   BoneTransform getBoneTransform(int index) {
      return boneTransforms[index];
   }

   BoneTransform getComputedBoneTransform(int index) {
      if (computedBoneTransforms[index] == null) {
         computedBoneTransforms[index] = new BoneTransform(getBoneTransform(index));
         if (parentBone != null)
            computedBoneTransforms[index].combine(parentBone.getComputedBoneTransform(index));
         else
            computedBoneTransforms[index].combine(BoneTransform.IDENTITY);
      }
      return computedBoneTransforms[index];
   }

   BoneTransform getFinalBoneTransform(int index) {
      if (finalBoneTransforms[index] == null) {
         finalBoneTransforms[index] = new BoneTransform(getComputedBoneTransform(index));
         finalBoneTransforms[index].normalize();
      }
      return finalBoneTransforms[index];
   }

   void setBaseTransform(BoneTransform var1) {
      baseTransform.copy(var1);
      baseTransformUpdate = true;
      globalTransformUpdated = true;
   }

   BoneTransform getBaseTransform() {
      return baseTransform;
   }

   BoneTransform getGlobalTransform() {
      if (baseTransformUpdate) {
         globalTransform.copy(getBaseTransform());
         if (parentBone != null)
            globalTransform.combine(parentBone.getGlobalTransform());
         baseTransformUpdate = false;
      }
      return globalTransform;
   }

   public BoneTransform getTransform(int index) {
      if (globalTransformUpdated) {
         localTransform.copy(getFinalBoneTransform(index));
         localTransform.combine(getGlobalTransform());
         globalTransformUpdated = false;
      }
      return localTransform;
   }

   float[] getInverseBindMatrix(int index) {
      return inverseBindMatrices[index];
   }

   float[] getBindMatrix(int index) {
      return bindMatrices[index];
   }

   float[] getPosition(int index) {
      return positions[index];
   }
}
