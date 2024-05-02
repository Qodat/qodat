package jagex;

import java.util.concurrent.Callable;

record MayaAnimationLoadTask(
        MayaAnimation mayaAnimation,
        Buffer mayaAnimationBuffer,
        int mayaAnimationVersion
) implements Callable<Object> {

   public Object call() {
      mayaAnimation.read(mayaAnimationBuffer, mayaAnimationVersion);
      return null;
   }
}
