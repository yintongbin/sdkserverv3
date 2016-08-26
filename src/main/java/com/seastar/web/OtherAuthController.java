package com.seastar.web;

import com.seastar.entity.*;
import com.seastar.service.OtherAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by wjl on 2016/8/23.
 */
@RestController
public class OtherAuthController {

    @Autowired
    private OtherAuthService otherAuthService;

    @RequestMapping(value = "/auth/bind", method = RequestMethod.POST)
    public BindRsp onBind(@RequestBody BindReq req) {
        return otherAuthService.doBind(req);
    }

    @RequestMapping(value = "/auth/bindquery", method = RequestMethod.POST)
    public BindQueryRsp onBindQuery(@RequestBody BindQueryReq req) {
        return otherAuthService.doBindQuery(req);
    }

    @RequestMapping(value = "/auth/changepwd", method = RequestMethod.POST)
    public BaseRsp onChangePwd(@RequestBody ChangePwdReq req) {
        return otherAuthService.doChangePwd(req);
    }

    @RequestMapping(value = "/auth/findpwd", method = RequestMethod.POST)
    public BaseRsp onFindPwd(@RequestBody FindPwdReq req) {
        return otherAuthService.doFindPwd(req);
    }

    @RequestMapping(value = "/auth/unbind", method = RequestMethod.POST)
    public BaseRsp onUnBind(@RequestBody UnBindReq req) {
        return otherAuthService.doUnBind(req);
    }

}
