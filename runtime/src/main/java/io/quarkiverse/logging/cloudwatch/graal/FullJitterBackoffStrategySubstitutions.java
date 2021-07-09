/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkiverse.logging.cloudwatch.graal;

import java.util.Random;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.amazonaws.retry.PredefinedBackoffStrategies$FullJitterBackoffStrategy")
final class FullJitterBackoffStrategySubstitutions {

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
                    "The setter for com.amazonaws.retry.PredefinedBackoffStrategies$FullJitterBackoffStrategy#random shouldn't be called.");
        }
    }
}
