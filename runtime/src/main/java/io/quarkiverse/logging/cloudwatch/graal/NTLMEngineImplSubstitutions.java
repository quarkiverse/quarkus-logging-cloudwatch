package io.quarkiverse.logging.cloudwatch.graal;

import java.security.SecureRandom;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.apache.http.impl.auth.NTLMEngineImpl")
final class NTLMEngineImplSubstitutions {

    @Alias
    @InjectAccessors(RandomAccessors.class)
    private static SecureRandom RND_GEN;

    public static final class RandomAccessors {

        private static volatile SecureRandom volatileRandom;

        public static SecureRandom get() {
            SecureRandom localVolatileRandom = volatileRandom;
            if (localVolatileRandom == null) {
                synchronized (RandomAccessors.class) {
                    localVolatileRandom = volatileRandom;
                    if (localVolatileRandom == null) {
                        volatileRandom = localVolatileRandom = new SecureRandom();
                    }
                }
            }
            return localVolatileRandom;
        }

        public static void set(SecureRandom secureRandom) {
            throw new IllegalStateException(
                    "The setter for org.apache.http.impl.auth.NTLMEngineImpl#random shouldn't be called.");
        }
    }
}
