package com.seastar.comm;

/**
 * Created by wjl on 2016/5/12.
 */
public class ReturnCode {


    public final static String CODE_OK = "0";
    public final static String CODE_OK_DESC = "操作成功";

    public final static String CODE_DB_ERR = "1";
    public final static String CODE_DB_ERR_DESC = "数据库操作失败";

    public final static String CODE_EMAIL_NULL = "2";
    public final static String CODE_EMAIL_NULL_DESC = "email is empty";

    public final static String CODE_MSG_ERR = "10";
    public final static String CODE_MSG_ERR_DESC = "请求消息格式错误";

    public final static String CODE_APP_NO_FIND = "20";
    public final static String CODE_APP_NO_FIND_DESC = "APP不存在";

    public final static String CODE_SIGN_ERR = "30";
    public final static String CODE_SIGN_ERR_DESC = "签名错误";

    public final static String CODE_APPLE_IAP_VERIFY_NET_ERR = "40";
    public final static String CODE_APPLE_IAP_VERIFY_NET_ERR_DESC = "验证失败，访问apple验证服务器失败";
    public final static String CODE_APPLE_IAP_VERIFY_RESULT_FORMAT_ERR = "41";
    public final static String CODE_APPLE_IAP_VERIFY_RESULT_FORMAT_ERR_DESC = "验证失败，apple验证服务器返回的结果格式不对";
    public final static String CODE_APPLE_IAP_VERIFY_ERR = "42";
    public final static String CODE_APPLE_IAP_VERIFY_ERR_DESC = "验证失败，未通过apple验证";
    public final static String CODE_APPLE_IAP_VERIFY_USED = "43";
    public final static String CODE_APPLE_IAP_VERIFY_USED_DESC = "验证失败，未通过apple验证";
    public final static String CODE_APPLE_IAP_DATA_ERR = "44";
    public final static String CODE_APPLE_IAP_DATA_ERR_DESC = "数据不正确";

    public final static String CODE_GOOGLE_IAP_VERIFY_NO_KEY = "50";
    public final static String CODE_GOOGLE_IAP_VERIFY_NO_KEY_DESC = "没有配置GOOGLE验证key";
    public final static String CODE_GOOGLE_IAP_VERIFY_ERR = "51";
    public final static String CODE_GOOGLE_IAP_VERIFY_ERR_DESC = "验证失败，未通过GOOGLE验证";
    public final static String CODE_GOOGLE_IAP_VERIFY_USED = "52";
    public final static String CODE_GOOGLE_IAP_VERIFY_USED_DESC = "已经验证过";

    public final static String CODE_LOGIN_NO_BASE = "60";
    public final static String CODE_LOGIN_NO_BASE_DESC = "无基础账号数据";
    public final static String CODE_LOGIN_PASSWORD_ERR = "61";
    public final static String CODE_LOGIN_PASSWORD_ERR_DESC = "密码错误";
    public final static String CODE_LOGIN_EXIST_USERNAME = "62";
    public final static String CODE_LOGIN_EXIST_USERNAME_DESC = "账号已经存在";
    public final static String CODE_LOGIN_NO_USER = "63";
    public final static String CODE_LOGIN_NO_USER_DESC = "账号不存在";
    public final static String CODE_LOGIN_DENY = "64";
    public final static String CODE_LOGIN_DENY_DESC = "你已经被封号";


    public final static String CODE_SESSION_NO = "70";
    public final static String CODE_SESSION_NO_DESC = "session不存在";

    public final static String CODE_BIND_EXIST = "80";
    public final static String CODE_BIND_EXIST_DESC = "已经绑定";
    public final static String CODE_BIND_THIRD_EXIST = "81";
    public final static String CODE_BIND_THIRD_EXIST_DESC = "第三方账号已经绑定其他的GUEST账号";
    public final static String CODE_BIND_EXIST_NO = "82";
    public final static String CODE_BIND_EXIST_NO_DESC = "不存在绑定关系";

    public final static String CODE_IAP_ORDER_EXIST = "90";
    public final static String CODE_IAP_ORDER_EXIST_DESC = "订单已经处理过";

    public final static String CODE_MYCARD_NO_CONFIG = "100";
    public final static String CODE_MYCARD_NO_CONFIG_DESC = "没有mycard配置";
    public final static String CODE_MYCARD_NO_ITEM = "101";
    public final static String CODE_MYCARD_NO_ITEM_DESC = "没有mycard商品";
    public final static String CODE_MYCARD_GET_AUTHCODE_FAIL = "102";
    public final static String CODE_MYCARD_GET_AUTHCODE_FAIL_DESC = "获取authcode失败";
    public final static String CODE_MYCARD_VERIFY_AUTHCODE_FAIL = "103";
    public final static String CODE_MYCARD_VERIFY_AUTHCODE_FAIL_DESC = "验证authcode失败";
    public final static String CODE_MYCARD_GET_MONEY_FAIL = "104";
    public final static String CODE_MYCARD_GET_MONEY_FAIL_DESC = "验证authcode失败";
    public final static String CODE_MYCARD_NO_SEQ = "105";
    public final static String CODE_MYCARD_NO_SEQ_DESC = "没有找到mycard订单";

    public final static String CODE_AD_NO_CONFIG = "-1000";
    public final static String CODE_AD_NO_CONFIG_DESC = "没有找到广告配置";
}
