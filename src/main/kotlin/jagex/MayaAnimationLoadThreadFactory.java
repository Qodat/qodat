package jagex;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Represents a thread factory for loading Maya animations.
 */
final class MayaAnimationLoadThreadFactory implements ThreadFactory {

   static int threadPoolExecutorThreadCount;
   static ThreadPoolExecutor threadPoolExecutor;

   public Thread newThread(@NotNull Runnable task) {
      return new Thread(task, "OSRS Maya Anim Load");
   }
}
