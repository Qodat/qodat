package jagex;

import java.util.concurrent.Callable;

class MayaAnimationLoadFrameTask implements Callable<Object> {

   final MayaAnimation animation;
   final int val$workStart;
   final int val$workEnd;
   final MayaAnimationFrameData[] val$curveLoadJobs;

   MayaAnimationLoadFrameTask(MayaAnimation animation, int var2, int var3, MayaAnimationFrameData[] var4) {
      this.animation = animation;
      this.val$workStart = var2;
      this.val$workEnd = var3;
      this.val$curveLoadJobs = var4;
   }

   public Object call() {
      for(int var1 = this.val$workStart; var1 < this.val$workEnd; ++var1) {
         this.val$curveLoadJobs[var1].call();
      }

      return null;
   }
}
