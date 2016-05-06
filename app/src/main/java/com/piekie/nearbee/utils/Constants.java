package com.piekie.nearbee.utils;

public class Constants {
    /**
     * Request code to use when launching the resolution activity.
     */
    public static final int REQUEST_RESOLVE_ERROR = 1001;

    // Keys to get and set the current subscription and publication tasks using SharedPreferences.
    public static final String KEY_SUBSCRIPTION_TASK = "subscription_task";
    public static final String KEY_PUBLICATION_TASK = "publication_task";

    // Tasks constants.
    public static final String TASK_SUBSCRIBE = "task_subscribe";
    public static final String TASK_UNSUBSCRIBE = "task_unsubscribe";
    public static final String TASK_PUBLISH = "task_publish";
    public static final String TASK_UNPUBLISH = "task_unpublish";
    public static final String TASK_NONE = "task_none";

    //Additional constants.
    public static final String LAST_IDENTIFIER = "last_identifier";
    public static final String NONE = "none";
    public static final int ID_MIN_SYMBOLS = 3;

    //Toast
    public static final String TOO_SMALL_ID = "More than 2 symbols please :0";
}
