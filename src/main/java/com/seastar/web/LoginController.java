package com.seastar.web;

import com.seastar.entity.*;
import com.seastar.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by wjl on 2016/8/23.
 */
@RestController
public class LoginController {
    @Autowired
    private LoginService loginService;

    @RequestMapping(value = "/auth/guest", method = RequestMethod.POST)
    public LoginRsp onGuestLogin(@RequestBody GuestLoginReq req) {
        return loginService.doGuestLogin(req);
    }

    @RequestMapping(value = "/auth/username", method = RequestMethod.POST)
    public LoginRsp onUsernameLogin(@RequestBody UserNameLoginReq req) {
        return loginService.doUsernameLogin(req);
    }

    @RequestMapping(value = "/auth/thirdparty", method = RequestMethod.POST)
    public LoginRsp onThirdLogin(@RequestBody ThirdPartyLoginReq req) {
        return loginService.doThirdLogin(req);
    }

    @RequestMapping(value = "/auth/session", method = RequestMethod.POST)
    public LoginRsp onSessionLogin(@RequestBody SessionLoginReq req) {
        return loginService.doSessionLogin(req);
    }

    @RequestMapping(value = "/auth/loginverify", method = RequestMethod.POST)
    public LoginVerifyRsp onLoginVerify(LoginVerifyReq req) {
        return loginService.doLoginVerify(req);
    }

    @RequestMapping(value = "/auth/logout", method = RequestMethod.POST)
    public BaseRsp onLoginout(LogoutReq req) {
        return loginService.doLogout(req);
    }

}
