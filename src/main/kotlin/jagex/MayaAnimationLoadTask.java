package jagex;

import java.util.concurrent.Callable;

record MayaAnimationLoadTask(
        MayaAnimation mayaAnimation,
        Buffer mayaAnimationBuffer,
        int mayaAnimationVersion
) implements Callable<Object> {

   public Object call() {
      this.mayaAnimation.read(this.mayaAnimationBuffer, this.mayaAnimationVersion);
      return null;
   }
}
