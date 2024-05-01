package jagex;

public class Skeleton  {
   int id;
   int count;
   int[] transformTypes;
   int[][] labels;
   MayaAnimationSkeleton mayaAnimationSkeleton;

   public Skeleton(int var1, byte[] var2) {
      this.id = var1;
      Buffer buffer = new Buffer(var2);
      this.count = buffer.readUnsignedByte();
      this.transformTypes = new int[this.count];
      this.labels = new int[this.count][];

      int var4;
      for(var4 = 0; var4 < this.count; ++var4) {
         this.transformTypes[var4] = buffer.readUnsignedByte();
      }

      for(var4 = 0; var4 < this.count; ++var4) {
         this.labels[var4] = new int[buffer.readUnsignedByte()];
      }

      for(var4 = 0; var4 < this.count; ++var4) {
         for(int var5 = 0; var5 < this.labels[var4].length; ++var5) {
            this.labels[var4][var5] = buffer.readUnsignedByte();
         }
      }

      if (buffer.offset < buffer.array.length) {
         var4 = buffer.readUnsignedShort();
         if (var4 > 0) {
            this.mayaAnimationSkeleton = new MayaAnimationSkeleton(buffer, var4);
         }
      }
   }

   public int getId() {
      return this.id;
   }

   public int getCount() {
      return this.count;
   }

   public MayaAnimationSkeleton getMayaAnimationSkeleton() {
      return this.mayaAnimationSkeleton;
   }

}
