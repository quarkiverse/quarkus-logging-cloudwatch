package io.quarkiverse.logging.cloudwatch.graal;

import java.util.Random;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.amazonaws.retry.PredefinedBackoffStrategies$EqualJitterBackoffStrategy")
final class AwsSubstitutions {

    @Alias
    @InjectAccessors(RandomAccessors.class)
    private Random random;

    public static final class RandomAccessors {

        private static volatile Random volatileRandom;

        public static Random get(Object object) {
            Random localVolatileRandom = volatileRandom;
            if (localVolatileRandom == null) {
                synchronized (RandomAccessors.class) {
                    localVolatileRandom = volatileRandom;
                    if (localVolatileRandom == null) {
                        volatileRandom = localVolatileRandom = new Random();
                    }
                }
            }
            return localVolatileRandom;
        }

        public static void set(Object object) {
            throw new IllegalStateException(
                    "The setter for com.amazonaws.retry.PredefinedBackoffStrategies$EqualJitterBackoffStrategy#random shouldn't be called.");
        }
    }
}
