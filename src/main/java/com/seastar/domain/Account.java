package com.seastar.domain;

/**
 * Created by os on 2016/8/20.
 */
public class Account {
    // 基础数据
    public long userId = 0;
    public String userName = "";
    public String password = "";
    public String email = "";
    public int status = 0;

    // 登录渠道数据
    public String facebookUserId = "";
    public String guestUserId = "";
    public String googleUserId = "";
    public String gamecenterUserId = "";

    public String deviceId = "";
    public String locale = "";

    public int appId = 0; // 当前登录的应用
}
