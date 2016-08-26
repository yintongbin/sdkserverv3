package com.seastar.web;

import com.seastar.entity.*;
import com.seastar.service.MycardPayService;
import com.seastar.service.OfficalPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by wjl on 2016/8/23.
 */
@RestController
public class PayController {

    @Autowired
    private OfficalPayService officalPayService;

    @Autowired
    private MycardPayService mycardPayService;

    @RequestMapping(value = "/iap/apple", method = RequestMethod.POST)
    public AppleIapRsp onApplePay(@RequestBody AppleIapReq req) {
        return officalPayService.doApplePay(req);
    }

    @RequestMapping(value = "/iab/google", method = RequestMethod.POST)
    public GoogleIabRsp onGooglePay(@RequestBody GoogleIabReq req) {
        return officalPayService.doGooglePay(req);
    }

    @RequestMapping(value = "/mycard/authcode", method = RequestMethod.POST)
    public MycardReqAuthCodeRsp onMycardReqAuthCode(@RequestBody MycardReqAuthCodeReq req) {
        return mycardPayService.doReqAuthCode(req);
    }

    @RequestMapping(value = "/mycard/money", method = RequestMethod.POST)
    public MycardCompleteRsp onMycardComplete(@RequestBody MycardCompleteReq req) {
        return mycardPayService.doComplete(req);
    }
}
