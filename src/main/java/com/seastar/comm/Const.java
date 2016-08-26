package com.seastar.comm;


/**
 * Created by wjl on 2016/5/19.
 */
public class Const {
    public static final int REGIST = 1;
    public static final int NOREGIST = 0;

    public static final int STATUS_DENY = 0;
    public static final int STATUS_ALLOW = 2;

    public static final int TYPE_ACCOUNT = 0;
    public static final int TYPE_GUEST = 1;
    public static final int TYPE_GOOGLE = 2;
    public static final int TYPE_GAMECENTER = 3;
    public static final int TYPE_FACEBOOK = 4;

    public static final int NEWUSER = 1;
    public static final int OLDUSER = 0;


    public static final int BIND = 1;
    public static final int UNBIND = 0;

    public static final int PAY_CHANNEL_GOOGLE = 0;
    public static final int PAY_CHANNEL_APPLE = 1;
    public static final int PAY_CHANNEL_MYCARD = 2;

    public static final int PAY_STATUS_CREATE = 0;
    public static final int PAY_STATUS_WAITPUSH = 1;
    public static final int PAY_STATUS_SUCCESSPUSH = 2;
    public static final int PAY_STATUS_FAILPUSH = 3;



}
