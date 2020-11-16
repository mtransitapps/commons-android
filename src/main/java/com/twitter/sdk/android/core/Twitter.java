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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.app.Application;
import android.content.Context;

import com.twitter.sdk.android.core.internal.ActivityLifecycleManager;
import com.twitter.sdk.android.core.internal.CommonUtils;
import com.twitter.sdk.android.core.internal.ExecutorUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 *  The {@link Twitter} class stores common configuration and state for TwitterKit SDK.
 */
public class Twitter {

    public static final String TAG = Twitter.class.getSimpleName();

    private static final String CONSUMER_KEY = "com.twitter.sdk.android.CONSUMER_KEY";
    private static final String CONSUMER_SECRET = "com.twitter.sdk.android.CONSUMER_SECRET";
    private static final String NOT_INITIALIZED_MESSAGE = "Must initialize Twitter before using getInstance()";

    @NonNull
    static final Logger DEFAULT_LOGGER = new DefaultLogger();

    @Nullable
    static volatile Twitter INSTANCE;

    @NonNull
    private final Application appContext;
    @NonNull
    private final ExecutorService executorService;
    @NonNull
    private final TwitterAuthConfig twitterAuthConfig;
    @NonNull
    private final ActivityLifecycleManager lifecycleManager;
    @NonNull
    private final Logger logger;
    private final boolean debug;

    private Twitter(@NonNull TwitterConfig config) {
        appContext = config.appContext;
        lifecycleManager = new ActivityLifecycleManager(appContext);

        if (config.twitterAuthConfig == null) {
            final String key = CommonUtils.getStringResourceValue(appContext, CONSUMER_KEY, "");
            final String secret = CommonUtils.getStringResourceValue(appContext, CONSUMER_SECRET, "");
            twitterAuthConfig = new TwitterAuthConfig(key, secret);
        } else {
            twitterAuthConfig = config.twitterAuthConfig;
        }

        if (config.executorService == null) {
            executorService = ExecutorUtils.buildThreadPoolExecutorService("twitter-worker");
        } else {
            executorService = config.executorService;
        }

        if (config.logger == null) {
            logger = DEFAULT_LOGGER;
        } else {
            logger = config.logger;
        }

        if (config.debug == null) {
            debug = false;
        } else {
            debug = config.debug;
        }
    }

    /**
     * Entry point to initialize the TwitterKit SDK.
     * <p>
     * Only the Application context is retained.
     * See https://developer.android.com/resources/articles/avoiding-memory-leaks.html
     * <p>
     * Should be called from {@code OnCreate()} method of custom {@code Application} class.
     * <pre>
     * public class SampleApplication extends Application {
     *   &#64;Override
     *   public void onCreate() {
     *     Twitter.initialize(this);
     *   }
     * }
     * </pre>
     *
     * @param appContext Android context used for initialization
     */
    public static void initialize(@NonNull Application appContext) {
        final TwitterConfig config = new TwitterConfig
                .Builder(appContext)
                .build();
        createTwitter(config);
    }

    /**
     * Entry point to initialize the TwitterKit SDK.
     * <p>
     * Only the Application context is retained.
     * See https://developer.android.com/resources/articles/avoiding-memory-leaks.html
     * <p>
     * Should be called from {@code OnCreate()} method of custom {@code Application} class.
     * <pre>
     * public class SampleApplication extends Application {
     *   &#64;Override
     *   public void onCreate() {
     *     final TwitterConfig config = new TwitterConfig.Builder(this).build();
     *     Twitter.initialize(config);
     *   }
     * }
     * </pre>
     *
     * @param config {@link TwitterConfig} user for initialization
     */
    public static void initialize(@NonNull TwitterConfig config) {
        createTwitter(config);
    }

    @NonNull
    static synchronized Twitter createTwitter(@NonNull TwitterConfig config) {
        if (INSTANCE == null) {
            INSTANCE = new Twitter(config);
            //noinspection ConstantConditions
            return INSTANCE;
        }
        //noinspection ConstantConditions
        return INSTANCE;
    }

    static void checkInitialized() {
        if (INSTANCE == null) {
            throw new IllegalStateException(NOT_INITIALIZED_MESSAGE);
        }
    }

    /**
     * @return Single instance of the {@link Twitter}.
     */
    @NonNull
    public static Twitter getInstance() {
        checkInitialized();
        //noinspection ConstantConditions
        return INSTANCE;
    }

    /**
     * @param component the component name
     * @return A {@link TwitterContext} for specified component.
     */
    @NonNull
    public Context getContext(@NonNull String component) {
        return new TwitterContext(appContext, component, ".TwitterKit" + File.separator + component);
    }

    /**
     * @return the global {@link TwitterAuthConfig}.
     */
    @NonNull
    public TwitterAuthConfig getTwitterAuthConfig() {
        return twitterAuthConfig;
    }

    /**
     * @return the global {@link ExecutorService}.
     */
    @NonNull
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * @return the global {@link ActivityLifecycleManager}.
     */
    @NonNull
    public ActivityLifecycleManager getActivityLifecycleManager() {
        return lifecycleManager;
    }

    /**
     * @return the global value for debug mode.
     */
    public static boolean isDebug() {
        if (INSTANCE == null) {
            return false;
        }
        //noinspection ConstantConditions
        return INSTANCE.debug;
    }

    /**
     * @return the global {@link Logger}.
     */
    @NonNull
    public static Logger getLogger() {
        if (INSTANCE == null) {
            return DEFAULT_LOGGER;
        }
        //noinspection ConstantConditions
        return INSTANCE.logger;
    }
}
