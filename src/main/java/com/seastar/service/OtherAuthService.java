package com.seastar.service;

import com.seastar.comm.AppUtils;
import com.seastar.comm.Const;
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

/**
 * Created by wjl on 2016/8/23.
 */
@Service
public class OtherAuthService {
    @Autowired
    private AccountDao accountDao;

    @Autowired
    private AppDao appDao;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Logger logger = LogManager.getLogger(OtherAuthService.class);

    public BindRsp doBind(BindReq req) {
        if (req.session.isEmpty() ||
                !AppUtils.validateThirdPartyUserId(req.thirdUserId) ||
                (req.loginType != Const.TYPE_FACEBOOK &&
                        req.loginType != Const.TYPE_GOOGLE &&
                        req.loginType != Const.TYPE_GAMECENTER)) {

            logger.info("Bind {} MsgErr", AppUtils.serialize(req));

            BindRsp rsp = new BindRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountBySession(req.session);
        if (account == null) {
            logger.info("Bind {} NoSession", AppUtils.serialize(req));

            BindRsp rsp = new BindRsp();
            rsp.code = ReturnCode.CODE_SESSION_NO;
            rsp.codeDesc = ReturnCode.CODE_SESSION_NO_DESC;
            return rsp;
        }

        if (req.loginType == Const.TYPE_FACEBOOK && !account.facebookUserId.isEmpty()) {
            logger.info("Bind {} BindedFB", AppUtils.serialize(req));

            BindRsp rsp = new BindRsp();
            rsp.code = ReturnCode.CODE_BIND_EXIST;
            rsp.codeDesc = ReturnCode.CODE_BIND_EXIST_DESC;
            return rsp;
        }

        if (req.loginType == Const.TYPE_GOOGLE && !account.googleUserId.isEmpty()) {
            logger.info("Bind {} BindedGoogle", AppUtils.serialize(req));

            BindRsp rsp = new BindRsp();
            rsp.code = ReturnCode.CODE_BIND_EXIST;
            rsp.codeDesc = ReturnCode.CODE_BIND_EXIST_DESC;
            return rsp;
        }

        if (req.loginType == Const.TYPE_GAMECENTER && !account.gamecenterUserId.isEmpty()) {
            logger.info("Bind {} BindedGameCenter", AppUtils.serialize(req));

            BindRsp rsp = new BindRsp();
            rsp.code = ReturnCode.CODE_BIND_EXIST;
            rsp.codeDesc = ReturnCode.CODE_BIND_EXIST_DESC;
            return rsp;
        }

        // 查找thirdUserId是否绑定了其他userid
        Account account1 = accountDao.getAccountByThirdId(req.thirdUserId, req.loginType);
        if (account1 != null) {
            logger.info("Bind {} BindOtherUser", AppUtils.serialize(req));

            BindRsp rsp = new BindRsp();
            rsp.code = ReturnCode.CODE_BIND_THIRD_EXIST;
            rsp.codeDesc = ReturnCode.CODE_BIND_THIRD_EXIST_DESC;
            return rsp;
        }

        // 建立绑定关系
        if (req.loginType == Const.TYPE_FACEBOOK)
            account.facebookUserId = req.thirdUserId;
        else if (req.loginType == Const.TYPE_GOOGLE)
            account.googleUserId = req.thirdUserId;
        else if (req.loginType == Const.TYPE_GAMECENTER)
            account.gamecenterUserId = req.thirdUserId;
        account.appId = req.appId;

        if (accountDao.bindUser(account)) {
            logger.info("Bind {} OK", AppUtils.serialize(req));

            BindRsp rsp = new BindRsp();
            rsp.code = ReturnCode.CODE_OK;
            rsp.codeDesc = ReturnCode.CODE_OK_DESC;
            rsp.loginType = req.loginType;
            rsp.thirdUserId = req.thirdUserId;
            rsp.userId = req.userId;
            return rsp;
        } else {
            logger.info("Bind {} DBErr", AppUtils.serialize(req));

            BindRsp rsp = new BindRsp();
            rsp.code = ReturnCode.CODE_DB_ERR;
            rsp.codeDesc = ReturnCode.CODE_DB_ERR_DESC;
            return rsp;
        }
    }

    public BindQueryRsp doBindQuery(BindQueryReq req) {
        if (req.session.isEmpty()) {
            logger.info("BindQuery {} MsgErr", AppUtils.serialize(req));

            BindQueryRsp rsp = new BindQueryRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountBySession(req.session);
        if (account == null || account.userId != req.userId) {
            logger.info("BindQuery {} NoSession", AppUtils.serialize(req));

            BindQueryRsp rsp = new BindQueryRsp();
            rsp.code = ReturnCode.CODE_SESSION_NO;
            rsp.codeDesc = ReturnCode.CODE_SESSION_NO_DESC;
            return rsp;
        }

        BindQueryRsp bindQRsp = new BindQueryRsp();
        bindQRsp.code = ReturnCode.CODE_OK;
        bindQRsp.codeDesc = ReturnCode.CODE_OK_DESC;
        bindQRsp.userId = req.userId;
        if (!account.googleUserId.isEmpty()) {
            bindQRsp.bindGoogle = Const.BIND;
            bindQRsp.googleUserId = account.googleUserId;
        } else {
            bindQRsp.bindGoogle = Const.UNBIND;
        }

        if (!account.facebookUserId.isEmpty()) {
            bindQRsp.bindFacebook = Const.BIND;
            bindQRsp.facebookUserId = account.facebookUserId;
        } else {
            bindQRsp.bindFacebook = Const.UNBIND;
        }

        if (!account.gamecenterUserId.isEmpty()) {
            bindQRsp.bindGameCenter = Const.BIND;
            bindQRsp.gameCenterUserId = account.gamecenterUserId;
        } else {
            bindQRsp.bindGameCenter = Const.UNBIND;
        }

        logger.info("BindQuery {} OK", AppUtils.serialize(req));

        return bindQRsp;
    }

    public BaseRsp doChangePwd(ChangePwdReq req) {
        if (req.session.isEmpty() || req.oldPwd.equals(req.newPwd) || !AppUtils.validatePassword(req.newPwd)) {
            logger.info("ChangePwd {} MsgErr", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountBySession(req.session);
        if (account == null || !AppUtils.md5encode(account.password).equals(req.oldPwd)) {
            logger.info("ChangePwd {} OldPwdErr", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_SESSION_NO;
            rsp.codeDesc = ReturnCode.CODE_SESSION_NO_DESC;
            return rsp;
        }

        account.password = req.newPwd;
        if (accountDao.updateUserPwd(account)) {
            logger.info("ChangePwd {} OK", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_OK;
            rsp.codeDesc = ReturnCode.CODE_OK_DESC;
            return rsp;
        } else {
            logger.info("ChangePwd {} DBErr", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_DB_ERR;
            rsp.codeDesc = ReturnCode.CODE_DB_ERR_DESC;
            return rsp;
        }
    }

    public BaseRsp doFindPwd(FindPwdReq req) {
        if (!AppUtils.validateUserName(req.userName)) {
            logger.info("FindPwd {} MsgErr", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        App app = appDao.getAppById(req.appId);
        if (app == null) {
            logger.info("FindPwd {} NoAppConfig", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_APP_NO_FIND;
            rsp.codeDesc = ReturnCode.CODE_APP_NO_FIND_DESC;
            return rsp;
        }

        String sign = AppUtils.md5encode(req.appId + req.userName + app.appKey);
        if (!sign.equals(req.sign)) {
            logger.info("FindPwd {} MD5Err", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_SIGN_ERR;
            rsp.codeDesc = ReturnCode.CODE_SIGN_ERR_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountByName(req.userName);
        if (account == null) {
            logger.info("FindPwd {} NoUser", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_LOGIN_NO_USER;
            rsp.codeDesc = ReturnCode.CODE_LOGIN_NO_USER_DESC;
            return rsp;
        }

        if (account.email.isEmpty()) {
            logger.info("FindPwd {} NoEmail", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_EMAIL_NULL;
            rsp.codeDesc = ReturnCode.CODE_EMAIL_NULL_DESC;
            return rsp;
        }

        BaseRsp rsp = new BaseRsp();
        rsp.code = ReturnCode.CODE_OK;
        rsp.codeDesc = ReturnCode.CODE_OK_DESC;

        logger.info("FindPwd {} Ok", AppUtils.serialize(req));

        // 发送邮件

        return rsp;
    }

    public BaseRsp doUnBind(UnBindReq req) {
        if (req.session.isEmpty() ||
                !AppUtils.validateThirdPartyUserId(req.thirdUserId) ||
                (req.loginType != Const.TYPE_FACEBOOK &&
                        req.loginType != Const.TYPE_GOOGLE &&
                        req.loginType != Const.TYPE_GAMECENTER)) {
            logger.info("Unbind {} MsgErr", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountBySession(req.session);
        if (account == null) {
            logger.info("Unbind {} NoSession", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_SESSION_NO;
            rsp.codeDesc = ReturnCode.CODE_SESSION_NO_DESC;
            return rsp;
        }

        if ((req.loginType == Const.TYPE_FACEBOOK && account.facebookUserId.isEmpty()) ||
                (req.loginType == Const.TYPE_GOOGLE && account.googleUserId.isEmpty()) ||
                (req.loginType == Const.TYPE_GAMECENTER && account.gamecenterUserId.isEmpty())) {

            logger.info("Unbind {} NoBind", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_BIND_EXIST_NO;
            rsp.codeDesc = ReturnCode.CODE_BIND_EXIST_NO_DESC;
            return rsp;
        }

        if (accountDao.deleteUserChannel(account, req.thirdUserId, req.loginType)) {
            logger.info("Unbind {} OK", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_OK;
            rsp.codeDesc = ReturnCode.CODE_OK_DESC;
            return rsp;
        } else {
            logger.info("Unbind {} DBErr", AppUtils.serialize(req));

            BaseRsp rsp = new BaseRsp();
            rsp.code = ReturnCode.CODE_DB_ERR;
            rsp.codeDesc = ReturnCode.CODE_DB_ERR_DESC;
            return rsp;
        }
    }

}
