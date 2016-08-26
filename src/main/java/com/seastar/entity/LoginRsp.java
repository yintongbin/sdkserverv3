package com.seastar.entity;

/**
 * Created by wjl on 2016/5/16.
 */
public class LoginRsp extends BaseRsp {
    public long userId = 0;
    public String userName = "";
    public String password = "";
    public int status = 0;
    public int newUser = 0;
    public int loginType = 0;
    public String session = "";
    public int payType = 0;
}
