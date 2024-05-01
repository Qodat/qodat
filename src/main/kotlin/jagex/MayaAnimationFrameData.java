package jagex;

import java.util.concurrent.Callable;

public class MayaAnimationFrameData implements Callable<Object> {

   final MayaAnimationFrame animationFrame;
   final MayaAnimationFrameType animationFrameType;
   final MayaAnimationFrameFlag animationFrameFlag;
   final int field1515;
   final MayaAnimation animation;

   MayaAnimationFrameData(MayaAnimation animation, MayaAnimationFrame animationFrame, MayaAnimationFrameType animationFrameType, MayaAnimationFrameFlag animationFrameFlag, int var5) {
      this.animation = animation;
      this.animationFrame = animationFrame;
      this.animationFrameType = animationFrameType;
      this.animationFrameFlag = animationFrameFlag;
      this.field1515 = var5;
   }

   public Object call() {
      this.animationFrame.initialiseKeyFrames();
      MayaAnimationFrame[][] var1;
      if (this.animationFrameType == MayaAnimationFrameType.field1548) {
         var1 = this.animation.secondaryFrames;
      } else {
         var1 = this.animation.primaryFrames;
      }

      var1[this.field1515][this.animationFrameFlag.method3076()] = this.animationFrame;
      return null;
   }

}
