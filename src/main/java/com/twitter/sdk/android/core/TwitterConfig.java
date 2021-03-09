/*
 * Copyright (C) 2015 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.twitter.sdk.android.core;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;

/**
 * Configurable Twitter options
 */
public class TwitterConfig {
    @NonNull
    final Application appContext;
    @Nullable
    final Logger logger;
    @Nullable
    final TwitterAuthConfig twitterAuthConfig;
    @Nullable
    final ExecutorService executorService;
    @Nullable
    final Boolean debug;

    private TwitterConfig(@NonNull Application appContext, @Nullable Logger logger, @Nullable TwitterAuthConfig twitterAuthConfig,
            @Nullable ExecutorService executorService, @Nullable Boolean debug) {
        this.appContext = appContext;
        this.logger = logger;
        this.twitterAuthConfig = twitterAuthConfig;
        this.executorService = executorService;
        this.debug = debug;
    }

    /**
     * Builder for creating {@link TwitterConfig} instances.
     * */
    public static class Builder {
        @NonNull
        private final Application appContext;
        @Nullable
        private Logger logger;
        @Nullable
        private TwitterAuthConfig twitterAuthConfig;
        @Nullable
        private ExecutorService executorService;
        @Nullable
        private Boolean debug;

        /**
         * Start building a new {@link TwitterConfig} instance.
         */
        public Builder(@NonNull Application appContext) {
            //noinspection ConstantConditions
            if (appContext == null) {
                throw new IllegalArgumentException("Context must not be null.");
            }

            this.appContext = appContext;
        }

        /**
         * Sets the {@link Logger} to build with.
         */
        @NonNull
        public Builder logger(@NonNull Logger logger) {
            //noinspection ConstantConditions
            if (logger == null) {
                throw new IllegalArgumentException("Logger must not be null.");
            }

            this.logger = logger;

            return this;
        }

        /**
         * Sets the {@link TwitterAuthConfig} to build with.
         */
        @NonNull
        public Builder twitterAuthConfig(@NonNull TwitterAuthConfig authConfig) {
            //noinspection ConstantConditions
            if (authConfig == null) {
                throw new IllegalArgumentException("TwitterAuthConfig must not be null.");
            }

            this.twitterAuthConfig = authConfig;

            return this;
        }

        /**
         * Sets the {@link ExecutorService} to build with.
         */
        @NonNull
        public Builder executorService(@NonNull ExecutorService executorService) {
            //noinspection ConstantConditions
            if (executorService == null) {
                throw new IllegalArgumentException("ExecutorService must not be null.");
            }

            this.executorService = executorService;

            return this;
        }

        /**
         * Enable debug mode
         */
        @NonNull
        public Builder debug(boolean debug) {
            this.debug = debug;

            return this;
        }

        /**
         * Build the {@link TwitterConfig} instance
         */
        @NonNull
        public TwitterConfig build() {
            return new TwitterConfig(appContext, logger, twitterAuthConfig, executorService, debug);
        }
    }
}
