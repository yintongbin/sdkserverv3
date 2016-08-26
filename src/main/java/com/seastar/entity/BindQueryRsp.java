package com.seastar.entity;

/**
 * Created by wjl on 2016/5/17.
 */
public class BindQueryRsp extends BaseRsp {
    public long userId = 0;
    public int bindGoogle = 0;
    public int bindFacebook = 0;
    public int bindGameCenter = 0;

    public String googleUserId = "";
    public String facebookUserId = "";
    public String gameCenterUserId = "";
}
