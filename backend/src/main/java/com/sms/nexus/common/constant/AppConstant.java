package com.sms.nexus.common.constant;

public class AppConstant {

    public static final String APP_NAME = "SMS Nexus";
    public static final String APP_VERSION = "1.0.0";

    // ID prefixes
    public static final String ID_PREFIX_ADMIN = "adm-";
    public static final String ID_PREFIX_STUDENT = "stu-";
    public static final String ID_PREFIX_TEACHER = "tch-";
    public static final String ID_PREFIX_LOG = "log-";
    public static final String ID_PREFIX_NOTIFICATION = "ntf-";

    // Redis key prefixes
    public static final String REDIS_ID_COUNTER_PREFIX = "id:counter:";
    public static final String REDIS_LOGIN_ATTEMPTS_PREFIX = "login:attempts:";
    public static final String REDIS_TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    public static final String REDIS_UNREAD_PREFIX = "notification:unread:";

    // Login rate limit
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final int LOGIN_LOCK_MINUTES = 15;

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private AppConstant() {}
}
