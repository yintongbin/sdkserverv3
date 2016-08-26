package com.seastar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seastar.comm.*;
import com.seastar.domain.*;
import com.seastar.entity.*;
import com.seastar.task.push.PushTask;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by wjl on 2016/8/23.
 */
@Service
public class OfficalPayService {
    @Autowired
    private AppDao appDao;

    @Autowired
    private AccountDao accountDao;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PayInfoDao payInfoDao;

    @Autowired
    private PushTask pushTask;

    //@Value("${server.tomcat.max-threads}")
    //private int maxThread = 10;

    //private CloseableHttpClient httpClient;
    private ObjectMapper mapper = new ObjectMapper();
    private Logger logger = LogManager.getLogger(OfficalPayService.class);

    /*
    @PostConstruct
    public void init() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContext sslcontext = SSLContexts.custom()
                .loadTrustMaterial(new TrustStrategy() {
                    @Override
                    public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        return true;
                    }
                })
                .build();

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslcontext,
                new String[] { "TLSv1", "SSL", "SunJSSE" },
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        // Create a registry of custom connection socket factories for supported
        // protocol schemes.
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslsf)
                .build();

        // Create socket configuration
        SocketConfig socketConfig = SocketConfig.custom()
                .setTcpNoDelay(true)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxThread);
        cm.setDefaultSocketConfig(socketConfig);



        httpClient = HttpClients.custom().setConnectionManager(cm).build();
    }

    @PreDestroy
    public void destroy() throws IOException {
        httpClient.close();
    }
    */

    public AppleIapRsp doApplePay(AppleIapReq req) {
        if (req.session.isEmpty() ||
                req.receipt.isEmpty() ||
                req.productId.isEmpty() ||
                req.transactionId.isEmpty() ||
                req.cparam.length() > 250 ||
                !AppUtils.validateGameRoleId(req.gameRoleId) ||
                !AppUtils.validateCurrency(req.currencyCode) ||
                !AppUtils.validatePrice(req.price) ||
                !AppUtils.validateServerId(req.serverId)) {
            logger.info("Apple {} MsgErr", AppUtils.serialize(req));

            AppleIapRsp rsp = new AppleIapRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        App app = appDao.getAppById(req.appId);
        if (app == null) {
            logger.info("Apple {} NoAppConfig", AppUtils.serialize(req));

            AppleIapRsp rsp = new AppleIapRsp();
            rsp.code = ReturnCode.CODE_APP_NO_FIND;
            rsp.codeDesc = ReturnCode.CODE_APP_NO_FIND_DESC;
            return rsp;
        }

        AppleIapReceipt appleIapReceipt = doIapVerify(req.receipt, app.sandbox > 0 ? true : false);
        if (appleIapReceipt == null) {
            logger.info("Apple {} RecepitNull", AppUtils.serialize(req));

            AppleIapRsp rsp = new AppleIapRsp();
            rsp.code = ReturnCode.CODE_APPLE_IAP_VERIFY_NET_ERR;
            rsp.codeDesc = ReturnCode.CODE_APPLE_IAP_VERIFY_NET_ERR_DESC;
            return rsp;
        }

        // 在数据库中设置成正式环境，但苹果还是沙盒环境时重新去沙盒环境验证
        if (appleIapReceipt.status == 21007) {
            appleIapReceipt = doIapVerify(req.receipt, true);
            if (appleIapReceipt == null) {
                logger.info("Apple {} SandboxReceiptNull", AppUtils.serialize(req));

                AppleIapRsp rsp = new AppleIapRsp();
                rsp.code = ReturnCode.CODE_APPLE_IAP_VERIFY_NET_ERR;
                rsp.codeDesc = ReturnCode.CODE_APPLE_IAP_VERIFY_NET_ERR_DESC;
                return rsp;
            }

            // 强制本次交易记录为沙盒
            app.sandbox = 1;
        }


        if (appleIapReceipt.status != 0) {
            logger.info("Apple {} {} ReceiptFail", AppUtils.serialize(req), AppUtils.serialize(appleIapReceipt));

            AppleIapRsp rsp = new AppleIapRsp();
            rsp.code = ReturnCode.CODE_APPLE_IAP_DATA_ERR;
            rsp.codeDesc = ReturnCode.CODE_APPLE_IAP_DATA_ERR_DESC;
            return rsp;
        }

        if (appleIapReceipt.receipt.in_app.size() == 0) {
            // ios6兼容处理
            if (!appleIapReceipt.receipt.product_id.equals(req.productId) ||
                    !appleIapReceipt.receipt.transaction_id.equals(req.transactionId)) {

                logger.info("Apple {} {} IOS6_Receipt_Data_Diff", AppUtils.serialize(req), AppUtils.serialize(appleIapReceipt));

                AppleIapRsp rsp = new AppleIapRsp();
                rsp.code = ReturnCode.CODE_APPLE_IAP_DATA_ERR;
                rsp.codeDesc = ReturnCode.CODE_APPLE_IAP_DATA_ERR_DESC;
                return rsp;
            }
        } else {
            // ios7处理
            AppleIapReceipt.InApp inApp = null;
            for (int i = 0; i < appleIapReceipt.receipt.in_app.size(); i++) {
                inApp = appleIapReceipt.receipt.in_app.get(i);
                if (inApp.product_id.equals(req.productId) && inApp.transaction_id.equals(req.transactionId)) {
                    break;
                }
                inApp = null;
            }

            // 未找到交易信息
            if (inApp == null) {
                logger.info("Apple {} {} IOS_Receipt_Data_Diff", AppUtils.serialize(req), AppUtils.serialize(appleIapReceipt));

                AppleIapRsp rsp = new AppleIapRsp();
                rsp.code = ReturnCode.CODE_APPLE_IAP_DATA_ERR;
                rsp.codeDesc = ReturnCode.CODE_APPLE_IAP_DATA_ERR_DESC;
                return rsp;
            }
        }

        // 是否验证过
        if (redisTemplate.opsForValue().setIfAbsent("Apple" + req.transactionId, req.transactionId)) {
            redisTemplate.expire("Apple" + req.transactionId, 7, TimeUnit.DAYS);

            // 开始入库并推送
            String order = IDGenerate.getOrder();

            // 生成订单
            PayInfo pmodel = new PayInfo();
            pmodel.order = order;
            pmodel.appId = req.appId;
            pmodel.userId = req.userId;
            pmodel.gameRoleId = req.gameRoleId;
            pmodel.serverId = req.serverId;
            pmodel.payStatus = Const.PAY_STATUS_WAITPUSH;
            pmodel.productId = req.productId;
            pmodel.amount = req.price;
            pmodel.currency = req.currencyCode;
            pmodel.channelType = Const.PAY_CHANNEL_APPLE;
            pmodel.channelOrder = req.transactionId;
            pmodel.sandbox = app.sandbox;
            pmodel.cparam = AppUtils.b64encode(req.cparam);

            // 插入数据库
            if (!payInfoDao.insertOfficalPay(pmodel)) {
                logger.info("Apple {} {} {} DBErr", AppUtils.serialize(req), AppUtils.serialize(appleIapReceipt), AppUtils.serialize(pmodel));
                AppleIapRsp rsp = new AppleIapRsp();
                rsp.code = ReturnCode.CODE_DB_ERR;
                rsp.codeDesc = ReturnCode.CODE_DB_ERR_DESC;
                return rsp;
            }

            if (!app.notifyUrl.isEmpty()) {
                pushTask.submit(app.notifyUrl, pmodel, app.appSecret);
            }

            AppleIapRsp rsp = new AppleIapRsp();
            rsp.codeDesc = ReturnCode.CODE_OK_DESC;
            rsp.code = ReturnCode.CODE_OK;
            rsp.userId = req.userId;
            rsp.productId = req.productId;
            rsp.order = order;

            logger.info("Apple {} {} {} OK", AppUtils.serialize(req), AppUtils.serialize(appleIapReceipt), AppUtils.serialize(pmodel));
            return rsp;
        } else {
            logger.info("Apple {} {} USED", AppUtils.serialize(req), AppUtils.serialize(appleIapReceipt));

            AppleIapRsp rsp = new AppleIapRsp();
            rsp.code = ReturnCode.CODE_IAP_ORDER_EXIST;
            rsp.codeDesc = ReturnCode.CODE_IAP_ORDER_EXIST_DESC;
            return rsp;
        }
    }

    private AppleIapReceipt doIapVerify(String receiptData, boolean sandbox) {

        String VERIFY_URL = "https://buy.itunes.apple.com/verifyReceipt";
        if (sandbox)
            VERIFY_URL = "https://sandbox.itunes.apple.com/verifyReceipt";

        //JsonObject receiptJsonObj = new JsonObject();
        //receiptJsonObj.put("receipt-data", receiptData);
        //receiptJsonObj.put("password", "");

        String verifyBody = "{\"receipt-data\" : \"" + receiptData + "\"}";
        Map<String, String> headers = new HashMap<>();
        try {
            headers.put("content-length", "" + verifyBody.getBytes("UTF-8").length);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        headers.put("content-type", "application/json; charset=UTF-8");
        headers.put("Accept", "application/json");

        Http http = new Http();
        String response = http.sendHttpsPost(VERIFY_URL, verifyBody, headers);
        if (!response.isEmpty()) {
            try {
                JsonNode root = mapper.readTree(response);
                if (root.get("status").asInt() == 0)
                    return mapper.readValue(response, AppleIapReceipt.class);
                else {
                    AppleIapReceipt appleIapReceipt = new AppleIapReceipt();
                    appleIapReceipt.status = root.get("status").asInt();
                    return appleIapReceipt;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }


    /*
    {
       "orderId":"GPA.1234-5678-9012-34567",
       "packageName":"com.example.app",
       "productId":"exampleSku",
       "purchaseTime":1345678900000,
       "purchaseState":0,
       "developerPayload":"bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ",
       "purchaseToken":"opaque-token-up-to-1000-characters"
     }
     通过payload添加productid，增加安全性
     */
    public GoogleIabRsp doGooglePay(GoogleIabReq req) {
        if (req.session.isEmpty() ||
                req.googleSignature.isEmpty() ||
                req.googleOriginalJson.isEmpty() ||
                req.cparam.length() > 250 ||
                !AppUtils.validateGameRoleId(req.gameRoleId) ||
                !AppUtils.validateCurrency(req.currencyCode) ||
                !AppUtils.validatePrice(req.price) ||
                !AppUtils.validateServerId(req.serverId)) {

            logger.info("Google {} MsgErr", AppUtils.serialize(req));

            GoogleIabRsp rsp = new GoogleIabRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountBySession(req.session);
        // 验证数据的可靠性
        if (account == null || account.userId != req.userId) {
            logger.info("Google {} NoUserOrUserDiff", AppUtils.serialize(req));

            GoogleIabRsp rsp = new GoogleIabRsp();
            rsp.code = ReturnCode.CODE_SESSION_NO;
            rsp.codeDesc = ReturnCode.CODE_SESSION_NO_DESC;
            return rsp;
        }

        App app = appDao.getAppById(req.appId);
        if (app == null || app.appId != req.appId) {
            logger.info("Google {} NoAppConfig", AppUtils.serialize(req));

            GoogleIabRsp rsp = new GoogleIabRsp();
            rsp.code = ReturnCode.CODE_APP_NO_FIND;
            rsp.codeDesc = ReturnCode.CODE_APP_NO_FIND_DESC;
            return rsp;
        }

        if (app.googleKey.isEmpty()) {
            logger.info("Google {} NoGoogleKey", AppUtils.serialize(req));

            GoogleIabRsp rsp = new GoogleIabRsp();
            rsp.code = ReturnCode.CODE_GOOGLE_IAP_VERIFY_NO_KEY;
            rsp.codeDesc = ReturnCode.CODE_GOOGLE_IAP_VERIFY_NO_KEY_DESC;
            return rsp;
        }

        // 提取谷歌订单
        // 注意：测试环境下orderid不存在
        String signedData = AppUtils.b64decode(req.googleOriginalJson);
        GooglePurchase purchase = null;
        try {
            purchase = mapper.readValue(signedData, GooglePurchase.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (purchase == null) {
            logger.info("Google {} GoogleOriginalJsonErr", AppUtils.serialize(req));

            GoogleIabRsp rsp = new GoogleIabRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        // 验证
        boolean result = verifyGoogleIap(app.googleKey, signedData, req.googleSignature);
        if (!result) {
            logger.info("Google {} {} VerifyErr", AppUtils.serialize(req), AppUtils.serialize(purchase));

            GoogleIabRsp rsp = new GoogleIabRsp();
            rsp.code = ReturnCode.CODE_GOOGLE_IAP_VERIFY_ERR;
            rsp.codeDesc = ReturnCode.CODE_GOOGLE_IAP_VERIFY_ERR_DESC;
            return rsp;
        }

        if (!purchase.orderId.isEmpty()) {
            if (!redisTemplate.opsForValue().setIfAbsent("Google" + purchase.orderId, purchase.orderId)) {
                logger.info("Google {} {} USED", AppUtils.serialize(req), AppUtils.serialize(purchase));

                GoogleIabRsp rsp = new GoogleIabRsp();
                rsp.code = ReturnCode.CODE_IAP_ORDER_EXIST;
                rsp.codeDesc = ReturnCode.CODE_IAP_ORDER_EXIST_DESC;
                return rsp;
            }
            redisTemplate.expire("Google" + purchase.orderId, 7, TimeUnit.DAYS);
        }

        String order = IDGenerate.getOrder();

        // 生成订单
        PayInfo pmodel = new PayInfo();
        pmodel.order = order;
        pmodel.appId = req.appId;
        pmodel.userId = req.userId;
        pmodel.gameRoleId = req.gameRoleId;
        pmodel.serverId = req.serverId;
        pmodel.payStatus = Const.PAY_STATUS_WAITPUSH;
        pmodel.productId = purchase.productId;
        pmodel.amount = req.price;
        pmodel.currency = req.currencyCode;
        pmodel.channelType = Const.PAY_CHANNEL_GOOGLE;
        pmodel.channelOrder = purchase.orderId;
        pmodel.sandbox = (purchase.orderId.isEmpty() ? 1 : app.sandbox); // 订单号为空强制成沙盒环境
        pmodel.cparam = AppUtils.b64encode(req.cparam);

        if (!payInfoDao.insertOfficalPay(pmodel)) {
            logger.info("Google {} {} {} DBErr", AppUtils.serialize(req), AppUtils.serialize(purchase), AppUtils.serialize(pmodel));

            GoogleIabRsp rsp = new GoogleIabRsp();
            rsp.code = ReturnCode.CODE_DB_ERR;
            rsp.codeDesc = ReturnCode.CODE_DB_ERR_DESC;
            return rsp;
        }

        if (!app.notifyUrl.isEmpty()) {
            pushTask.submit(app.notifyUrl, pmodel, app.appSecret);
        }

        GoogleIabRsp rsp = new GoogleIabRsp();
        rsp.code = ReturnCode.CODE_OK;
        rsp.codeDesc = ReturnCode.CODE_OK_DESC;
        rsp.userId = req.userId;
        rsp.googleOrder = purchase.orderId;
        rsp.productId = purchase.productId;
        rsp.order = order;

        logger.info("Google {} {} {} OK", AppUtils.serialize(req), AppUtils.serialize(purchase), AppUtils.serialize(pmodel));
        return rsp;
    }

    /**
     * 根据游戏的public key验证支付时从Google Market返回的signedData与signature的值是否对应
     *
     * @param base64key
     *            ：配置在Google Play开发者平台上的公钥
     * @param originalJson
     *            ：支付成功时响应的物品信息
     * @param signature
     *            ：已加密后的签名
     * @return boolean：true 验证成功<br/>
     *         false 验证失败
     */
    public static boolean verifyGoogleIap(String base64key, String originalJson, String signature) {
        try {
            // 解密出验证key
            byte[] decodedKey = Base64.getDecoder().decode(base64key);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));

            // 验证票据
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(publicKey);
            sig.update(originalJson.getBytes());

            return sig.verify(Base64.getDecoder().decode(signature));
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (InvalidKeyException ex) {
            ex.printStackTrace();
        } catch (SignatureException ex) {
            ex.printStackTrace();
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
