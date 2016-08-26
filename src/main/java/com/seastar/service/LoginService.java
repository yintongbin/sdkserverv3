package com.seastar.service;

import com.seastar.comm.AppUtils;
import com.seastar.comm.Const;
import com.seastar.comm.IDGenerate;
import com.seastar.comm.ReturnCode;
import com.seastar.domain.Account;
import com.seastar.domain.AccountDao;
import com.seastar.domain.App;
import com.seastar.domain.AppDao;
import com.seastar.entity.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created by wjl on 2016/8/23.
 */
@Service
public class LoginService {

    @Autowired
    private AppDao appDao;

    @Autowired
    private AccountDao accountDao;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Logger logger = LogManager.getLogger(LoginService.class);

    public LoginRsp doGuestLogin(GuestLoginReq req) {
        if (req.sign.isEmpty() ||
                req.deviceInfo.isEmpty() ||
                !AppUtils.validateDeviceId(req.deviceId) ||
                !AppUtils.validateLocale(req.locale)) {

            logger.info("Guest {} MsgErr", AppUtils.serialize(req));

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        App app = appDao.getAppById(req.appId);
        if (app == null) {
            logger.info("Guest {} NoAppConfig", AppUtils.serialize(req));

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_APP_NO_FIND;
            rsp.codeDesc = ReturnCode.CODE_APP_NO_FIND_DESC;
            return rsp;
        }

        // 验证应用签名
        String myMd5 = AppUtils.md5encode(req.appId + req.deviceId + req.locale + app.appKey);
        if (!myMd5.equals(req.sign)) {
            logger.info("Guest {} {} MD5Err", AppUtils.serialize(req), myMd5);

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_SIGN_ERR;
            rsp.codeDesc = ReturnCode.CODE_SIGN_ERR_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountByThirdId(req.deviceId, Const.TYPE_GUEST);
        if (account == null) {
            account = new Account();
            account.userId = IDGenerate.getId();
            account.userName = String.format("ST%06d", account.userId);
            account.password = IDGenerate.getRandomString(8);
            account.appId = req.appId;
            account.deviceId = req.deviceId;
            account.locale = req.locale;
            account.guestUserId = req.deviceId;
            account.status = Const.STATUS_ALLOW;
            accountDao.insertAccount(account);

            String session = AppUtils.createUUID();
            redisTemplate.opsForValue().set("session" + session, account.userId + "", 60, TimeUnit.DAYS);

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_OK;
            rsp.codeDesc = ReturnCode.CODE_OK_DESC;
            rsp.newUser = Const.NEWUSER;
            rsp.status = account.status;
            rsp.userId = account.userId;
            rsp.userName = account.userName;
            rsp.password = account.password;
            rsp.loginType = Const.TYPE_GUEST;
            rsp.session = session;
            rsp.payType = app.payType;

            logger.info("Guest {} {} Regist", AppUtils.serialize(req), AppUtils.serialize(rsp));
            return rsp;
        } else {
            if (account.status == Const.STATUS_DENY) {
                logger.info("Guest {} LoginDeny", AppUtils.serialize(req));

                LoginRsp rsp = new LoginRsp();
                rsp.codeDesc = ReturnCode.CODE_LOGIN_DENY_DESC;
                rsp.code = ReturnCode.CODE_LOGIN_DENY;
                return rsp;
            }

            String session = AppUtils.createUUID();
            redisTemplate.opsForValue().set("session" + session, account.userId + "", 60, TimeUnit.DAYS);
            // 直接登录
            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_OK;
            rsp.codeDesc = ReturnCode.CODE_OK_DESC;
            rsp.newUser = Const.OLDUSER;
            rsp.password = "";
            rsp.userId = account.userId;
            rsp.userName = account.userName;
            rsp.loginType = Const.TYPE_GUEST;
            rsp.status = account.status;
            rsp.session = session;
            rsp.payType = app.payType;

            logger.info("Guest {} {} Login", AppUtils.serialize(req), AppUtils.serialize(rsp));
            return rsp;
        }
    }

    public LoginRsp doUsernameLogin(UserNameLoginReq req) {
        if (req.sign.isEmpty() ||
                req.deviceInfo.isEmpty() ||
                !AppUtils.validateDeviceId(req.deviceId) ||
                !AppUtils.validateUserName(req.userName) ||
                !AppUtils.validatePassword(req.password) ||
                !AppUtils.validateLocale(req.locale)) {
            logger.info("Username {} MsgErr", AppUtils.serialize(req));

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        App app = appDao.getAppById(req.appId);
        if (app == null) {
            logger.info("Username {} NoAppConfig", AppUtils.serialize(req));

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_APP_NO_FIND;
            rsp.codeDesc = ReturnCode.CODE_APP_NO_FIND_DESC;
            return rsp;
        }

        // 验证应用签名
        String tmp = req.appId + req.deviceId + req.locale +
                req.userName + req.password + req.regist + app.appKey;
        String myMd5 = AppUtils.md5encode(tmp);
        if (!myMd5.equals(req.sign)) {
            logger.info("Username {} {} MD5Err", AppUtils.serialize(req), myMd5);

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_SIGN_ERR;
            rsp.codeDesc = ReturnCode.CODE_SIGN_ERR_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountByName(req.userName);
        if (account == null) {
            if (req.regist == Const.NOREGIST) {
                // 无账号下，进行登录，必须失败
                logger.info("Username {} LoginNoUser", AppUtils.serialize(req));

                LoginRsp rsp = new LoginRsp();
                rsp.code = ReturnCode.CODE_LOGIN_NO_USER;
                rsp.codeDesc = ReturnCode.CODE_LOGIN_NO_USER_DESC;
                return rsp;
            } else {
                // 无账号下，进行注册
                account = new Account();
                account.appId = req.appId;
                account.userId = IDGenerate.getId();
                account.userName = req.userName;
                account.password = req.password;
                account.deviceId = req.deviceId;
                account.locale = req.locale;
                account.email = req.email;
                account.status = Const.STATUS_ALLOW;

                accountDao.insertAccount(account);

                String session = AppUtils.createUUID();
                redisTemplate.opsForValue().set("session" + session, account.userId + "", 60, TimeUnit.DAYS);

                LoginRsp rsp = new LoginRsp();
                rsp.code = ReturnCode.CODE_OK;
                rsp.codeDesc = ReturnCode.CODE_OK_DESC;
                rsp.newUser = Const.NEWUSER;
                rsp.password = "";
                rsp.userId = account.userId;
                rsp.userName = account.userName;
                rsp.loginType = Const.TYPE_GUEST;
                rsp.status = account.status;
                rsp.session = session;
                rsp.payType = app.payType;

                logger.info("Username {} {} Regist", AppUtils.serialize(req), AppUtils.serialize(rsp));
                return rsp;
            }
        } else {
            if (req.regist == Const.REGIST) {
                // 有账号下，进行注册，必须失败
                logger.info("Username {} ExistUser", AppUtils.serialize(req));

                LoginRsp rsp = new LoginRsp();
                rsp.code = ReturnCode.CODE_LOGIN_EXIST_USERNAME;
                rsp.codeDesc = ReturnCode.CODE_LOGIN_EXIST_USERNAME_DESC;
                return rsp;
            } else {
                // 有账号下，进行登录
                String passwordmd5 = AppUtils.md5encode(account.password);
                if (!passwordmd5.equals(req.password)) {
                    logger.info("Username {} {} PwdErr", AppUtils.serialize(req), account.password);

                    LoginRsp rsp = new LoginRsp();
                    rsp.code = ReturnCode.CODE_LOGIN_PASSWORD_ERR;
                    rsp.codeDesc = ReturnCode.CODE_LOGIN_PASSWORD_ERR_DESC;
                    return rsp;
                } else if (account.status == Const.STATUS_DENY) {
                    logger.info("Username {} LoginDeny", AppUtils.serialize(req));

                    LoginRsp rsp = new LoginRsp();
                    rsp.code = ReturnCode.CODE_LOGIN_DENY;
                    rsp.codeDesc = ReturnCode.CODE_LOGIN_DENY_DESC;
                    return rsp;
                } else {
                    // 存储session数据
                    String session = AppUtils.createUUID();
                    redisTemplate.opsForValue().set("session" + session, account.userId + "", 60, TimeUnit.DAYS);

                    // 返回回应
                    LoginRsp rsp = new LoginRsp();
                    rsp.code = ReturnCode.CODE_OK;
                    rsp.codeDesc = ReturnCode.CODE_OK_DESC;
                    rsp.newUser = Const.OLDUSER;
                    rsp.password = "";
                    rsp.status = account.status;
                    rsp.session = session;
                    rsp.loginType = Const.TYPE_ACCOUNT;
                    rsp.userName = account.userName;
                    rsp.userId = account.userId;
                    rsp.payType = app.payType;

                    logger.info("Username {} {} Login", AppUtils.serialize(req), AppUtils.serialize(rsp));
                    return rsp;
                }
            }
        }
    }

    public LoginRsp doThirdLogin(ThirdPartyLoginReq req) {
        if (req.sign.isEmpty() ||
                req.loginType < Const.TYPE_GOOGLE ||
                req.loginType > Const.TYPE_FACEBOOK ||
                !AppUtils.validateDeviceId(req.deviceId) ||
                !AppUtils.validateLocale(req.locale) ||
                !AppUtils.validateThirdPartyUserId(req.thirdUserId)) {
            logger.info("Third {} MsgErr", AppUtils.serialize(req));

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        App app = appDao.getAppById(req.appId);
        if (app == null) {
            logger.info("Third {} NoAppConfig", AppUtils.serialize(req));

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_APP_NO_FIND;
            rsp.codeDesc = ReturnCode.CODE_APP_NO_FIND_DESC;
            return rsp;
        }

        // 验证应用签名
        String myMd5 = AppUtils.md5encode(req.appId +
                req.deviceId +
                req.locale +
                req.thirdUserId +
                req.thirdAccessToken +
                req.loginType +
                app.appKey);
        if (!myMd5.equals(req.sign)) {
            logger.info("Third {} {} MD5Err", AppUtils.serialize(req), myMd5);

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_SIGN_ERR;
            rsp.codeDesc = ReturnCode.CODE_SIGN_ERR_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountByThirdId(req.thirdUserId, req.loginType);
        if (account == null) {
            // 无第三方账号时创建账号
            account = new Account();
            account.userId = IDGenerate.getId();
            account.userName = String.format("ST%06d", account.userId);
            account.password = IDGenerate.getRandomString(8);
            account.appId = req.appId;
            account.status = Const.STATUS_ALLOW;
            account.deviceId = req.deviceId;
            account.locale = req.locale;
            if (req.loginType == Const.TYPE_FACEBOOK)
                account.facebookUserId = req.thirdUserId;
            else if (req.loginType == Const.TYPE_GOOGLE)
                account.googleUserId = req.thirdUserId;
            else if (req.loginType == Const.TYPE_GAMECENTER)
                account.gamecenterUserId = req.thirdUserId;

            accountDao.insertAccount(account);

            String session = AppUtils.createUUID();
            redisTemplate.opsForValue().set("session" + session, account.userId + "", 60, TimeUnit.DAYS);
            // 返回结果
            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_OK;
            rsp.codeDesc = ReturnCode.CODE_OK_DESC;
            rsp.newUser = Const.NEWUSER;
            rsp.password = account.password;
            rsp.userId = account.userId;
            rsp.userName = account.userName;
            rsp.loginType = req.loginType;
            rsp.status = Const.STATUS_ALLOW;
            rsp.session = session;
            rsp.payType = app.payType;

            logger.info("Third {} {} Regist", AppUtils.serialize(req), AppUtils.serialize(rsp));

            return rsp;
        } else {
            if (account.status == Const.STATUS_DENY) {
                logger.info("Third {} LoginDeny", AppUtils.serialize(req));

                LoginRsp rsp = new LoginRsp();
                rsp.code = ReturnCode.CODE_LOGIN_DENY;
                rsp.codeDesc = ReturnCode.CODE_LOGIN_DENY_DESC;
                return rsp;
            }

            // 存储session数据
            String session = AppUtils.createUUID();
            redisTemplate.opsForValue().set("session" + session, account.userId + "", 60, TimeUnit.DAYS);

            // 直接登录
            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_OK;
            rsp.codeDesc = ReturnCode.CODE_OK_DESC;
            rsp.newUser = Const.OLDUSER;
            rsp.password = "";
            rsp.userId = account.userId;
            rsp.userName = account.userName;
            rsp.loginType = req.loginType;
            rsp.status = account.status;
            rsp.session = session;
            rsp.payType = app.payType;

            logger.info("Third {} {} Login", AppUtils.serialize(req), AppUtils.serialize(rsp));

            return rsp;
        }
    }

    public LoginRsp doSessionLogin(SessionLoginReq req) {
        if (req.deviceInfo.isEmpty() || req.session.isEmpty() || !AppUtils.validateLocale(req.locale)) {
            logger.info("Session {} MsgErr", AppUtils.serialize(req));

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        App app = appDao.getAppById(req.appId);
        if (app == null) {
            logger.info("Session {} NoAppConfig", AppUtils.serialize(req));

            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_APP_NO_FIND;
            rsp.codeDesc = ReturnCode.CODE_APP_NO_FIND_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountBySession(req.session);
        if (account != null && account.userId == req.userId) {
            LoginRsp rsp = new LoginRsp();
            rsp.code = ReturnCode.CODE_OK;
            rsp.codeDesc = ReturnCode.CODE_OK_DESC;
            rsp.newUser = Const.OLDUSER;
            rsp.status = Const.STATUS_ALLOW;
            rsp.userId = req.userId;
            rsp.userName = account.userName;
            rsp.password = "";
            rsp.loginType = Const.TYPE_ACCOUNT;
            rsp.session = req.session;
            rsp.payType = app.payType;

            logger.info("Session {} {} Login", AppUtils.serialize(req), AppUtils.serialize(rsp));

            return rsp;
        }

        LoginRsp rsp = new LoginRsp();
        rsp.codeDesc = ReturnCode.CODE_SESSION_NO_DESC;
        rsp.code = ReturnCode.CODE_SESSION_NO;

        logger.info("Session {} NoSession", AppUtils.serialize(req));

        return rsp;
    }

    public LoginVerifyRsp doLoginVerify(LoginVerifyReq req) {
        if (req.token.isEmpty()) {
            logger.info("Verify {} MsgErr", AppUtils.serialize(req));

            LoginVerifyRsp rsp = new LoginVerifyRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountBySession(req.token);
        if (account != null && account.userId == req.userId) {
            LoginVerifyRsp rsp = new LoginVerifyRsp();
            rsp.code = ReturnCode.CODE_OK;
            rsp.codeDesc = ReturnCode.CODE_OK_DESC;
            rsp.cparam = req.cparam;
            rsp.userId = req.userId;

            logger.info("Verify {} OK", AppUtils.serialize(req));
            return rsp;
        }

        LoginVerifyRsp rsp = new LoginVerifyRsp();
        rsp.code = ReturnCode.CODE_LOGIN_NO_USER;
        rsp.codeDesc = ReturnCode.CODE_LOGIN_NO_USER_DESC;

        logger.info("Verify {} NoUser", AppUtils.serialize(req));
        return rsp;
    }

    public BaseRsp doLogout(LogoutReq req) {
        if (req.session.isEmpty()) {
            logger.info("Logout {} NoSession", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        redisTemplate.delete("session" + req.session);
        BaseRsp baseRsp = new BaseRsp();
        baseRsp.code = ReturnCode.CODE_OK;
        baseRsp.codeDesc = ReturnCode.CODE_OK_DESC;

        logger.info("Logout {} OK", AppUtils.serialize(req));
        return baseRsp;
    }
}
