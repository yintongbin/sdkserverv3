package com.seastar.domain;

/**
 * Created by wjl on 2016/8/19.
 */
public class App {
    // 基本配置
    public int appId = 0; // 应用id
    public int status = 0; // 0创建,1审核中,2审核通过,3下架
    public String appKey = ""; // 服务器验证key
    public String appSecret = ""; // 服务器间加密key
    public int sandbox = 0; // 0正式模式，1沙箱模式
    public String notifyUrl = ""; // 支付回调url
    public int payType = 0; // 支付方式，仅安卓版有效，0无支付方式 1google 2mycard 3apple 4google&mycard

    // google配置
    public String googleKey = ""; // 存放签名用key

    // mycard配置
    public String facServiceId = "seastar"; // 厂商服务代码
    public String tradeType = "1"; // 交易模式 1:android 2:web
    public String hashKey = "1adc3f0bdc96b0d3212ccc16053fdf2f"; // 厂商key
    public String sandBoxMode = "false"; // 是否为测试环境, true, false
}
