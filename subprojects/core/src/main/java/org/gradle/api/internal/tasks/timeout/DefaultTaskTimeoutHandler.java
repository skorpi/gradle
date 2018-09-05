/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.timeout;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultTaskTimeoutHandler implements TaskTimeoutHandler {
    private final ScheduledExecutorService executor;

    public DefaultTaskTimeoutHandler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public TaskTimeout start(Thread taskExecutionThread, long timeoutInMillis) {
        CancelThreadOnTimeout timeout = new CancelThreadOnTimeout(taskExecutionThread, timeoutInMillis);
        ScheduledFuture<?> periodicCheck = executor.scheduleAtFixedRate(timeout, 0, 50, TimeUnit.MILLISECONDS);
        return new DefaultTaskTimeout(timeout, periodicCheck);
    }

    private static final class DefaultTaskTimeout implements TaskTimeout {

        private final CancelThreadOnTimeout timeout;
        private final ScheduledFuture<?> periodicCheck;

        private DefaultTaskTimeout(CancelThreadOnTimeout timeout, ScheduledFuture<?> periodicCheck) {
            this.timeout = timeout;
            this.periodicCheck = periodicCheck;
        }

        @Override
        public void stop() {
            periodicCheck.cancel(true);
        }

        @Override
        public boolean didTimeout() {
            return timeout.timedOut;
        }
    }

    private static class CancelThreadOnTimeout implements Runnable {
        private final long start;
        private final long timeout;
        private final Thread thread;

        private volatile boolean timedOut;

        private CancelThreadOnTimeout(Thread thread, long timeout) {
            this.thread = thread;
            this.start = System.currentTimeMillis();
            this.timeout = timeout;
        }

        @Override
        public void run() {
            if (timedOut) {
                return;
            }
            if (runtime() > timeout) {
                timedOut = true;
                thread.interrupt();
            }
        }

        private long runtime() {
            return System.currentTimeMillis() - start;
        }
    }
}
