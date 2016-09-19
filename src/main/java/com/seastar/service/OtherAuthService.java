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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

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

    @Value("${spring.mail.host}")
    private String mailhost;

    @Value("${spring.mail.username}")
    private String mailusername;

    @Value("${spring.mail.password}")
    private String mailpassword;


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
        mailSend("密码找回", "您的账号为:" + account.userName + "   密码为:" + account.password, account.email);
        return rsp;
    }


    @Async
    public void mailSend(String title,String content,String toemail)  {

        try {
            Properties prop = new Properties();
            prop.setProperty("mail.host", "smtp.sohu.com");
            prop.setProperty("mail.transport.protocol", "smtp");
            prop.setProperty("mail.smtp.auth", "true");
            //使用JavaMail发送邮件的5个步骤
            //1、创建session
            Session session = Session.getInstance(prop);
            //开启Session的debug模式，这样就可以查看到程序发送Email的运行状态
            session.setDebug(true);
            //2、通过session得到transport对象
            Transport ts = session.getTransport();
            //3、使用邮箱的用户名和密码连上邮件服务器，发送邮件时，发件人需要提交邮箱的用户名和密码给smtp服务器，用户名和密码都通过验证之后才能够正常发送邮件给收件人。
            ts.connect(mailhost, mailusername, mailpassword);
            //4、创建邮件
            Message message = createSimpleMail(session,title,content,toemail);
            //5、发送邮件
            ts.sendMessage(message, message.getAllRecipients());
            ts.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * @Method: createSimpleMail
     * @Description: 创建一封只包含文本的邮件
     * @param session
     * @return
     * @throws Exception
     */
    public MimeMessage createSimpleMail(Session session, String title , String content, String toemail)
            throws Exception {
        //创建邮件对象
        MimeMessage message = new MimeMessage(session);
        //指明邮件的发件人
        message.setFrom(new InternetAddress(mailusername));
        //指明邮件的收件人，现在发件人和收件人是一样的，那就是自己给自己发
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toemail));
        //邮件的标题
        message.setSubject(title);
        //邮件的文本内容
        message.setContent(content, "text/html;charset=UTF-8");
        //返回创建好的邮件对象
        return message;
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
