package com.seastar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seastar.comm.*;
import com.seastar.domain.*;
import com.seastar.entity.MycardCompleteReq;
import com.seastar.entity.MycardCompleteRsp;
import com.seastar.entity.MycardReqAuthCodeReq;
import com.seastar.entity.MycardReqAuthCodeRsp;
import com.seastar.task.push.PushTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wjl on 2016/8/23.
 */
@Service
public class MycardPayService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AccountDao accountDao;

    @Autowired
    private AppDao appDao;

    @Autowired
    private MycardProductDao mycardProductDao;

    @Autowired
    private PayInfoDao payInfoDao;

    @Autowired
    private PushTask pushTask;

    private ObjectMapper mapper = new ObjectMapper();

    private Logger logger = LogManager.getLogger(MycardPayService.class);

    public MycardReqAuthCodeRsp doReqAuthCode(MycardReqAuthCodeReq req) {
        if (req.cparam.length() > 250 ||
                req.session.isEmpty() ||
                !AppUtils.validateGameRoleId(req.customerId) ||
                !req.itemCode.matches("[0-9a-zA-Z.]{1,15}") ||
                !AppUtils.validateServerId(req.serverId)) {

            logger.info("MyCard_ReqAuthCode {} MsgErr", AppUtils.serialize(req));

            MycardReqAuthCodeRsp rsp = new MycardReqAuthCodeRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountBySession(req.session);
        if (account == null || account.userId != req.userId) {
            logger.info("MyCard_ReqAuthCode {} NoSession", AppUtils.serialize(req));

            MycardReqAuthCodeRsp rsp = new MycardReqAuthCodeRsp();
            rsp.code = ReturnCode.CODE_SESSION_NO;
            rsp.codeDesc = ReturnCode.CODE_SESSION_NO_DESC;
            return rsp;
        }

        App app = appDao.getAppById(req.appId);
        if (app == null) {
            logger.info("MyCard_ReqAuthCode {} NoAppConfig", AppUtils.serialize(req));

            MycardReqAuthCodeRsp rsp = new MycardReqAuthCodeRsp();
            rsp.code = ReturnCode.CODE_MYCARD_NO_CONFIG;
            rsp.codeDesc = ReturnCode.CODE_MYCARD_NO_CONFIG_DESC;
            return rsp;
        }

        // 获取商品
        MycardProduct mycardProduct = mycardProductDao.getProduct(req.appId, req.itemCode);
        if (mycardProduct == null) {
            logger.info("MyCard_ReqAuthCode {} NoProductConfig", AppUtils.serialize(req));

            MycardReqAuthCodeRsp rsp = new MycardReqAuthCodeRsp();
            rsp.code = ReturnCode.CODE_MYCARD_NO_ITEM;
            rsp.codeDesc = ReturnCode.CODE_MYCARD_NO_ITEM_DESC;
            return rsp;
        }

        String order = IDGenerate.getOrder();
        String productName = "";
        try {
            productName = URLEncoder.encode(mycardProduct.productName, "utf-8").toLowerCase();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        String preHashValue = app.facServiceId +
                order +
                app.tradeType +
                req.customerId +
                productName +
                mycardProduct.amount +
                mycardProduct.currency +
                app.sandBoxMode + app.hashKey;

        preHashValue = AppUtils.sha256encode(preHashValue);

        // 厂商用户不需要在mycard后台配置任何商品id，所以也不需要传商品ID过去
        StringBuffer sb = new StringBuffer();
        sb.append("FacServiceId").append("=").append(app.facServiceId).append("&");
        sb.append("FacTradeSeq").append("=").append(order).append("&");
        sb.append("TradeType").append("=").append(app.tradeType).append("&");
        //sb.append("ServerId").append("=").append(req.serverId).append("&");
        sb.append("CustomerId").append("=").append(req.customerId).append("&");
        //sb.append("PaymentType").append("=").append(myCardProductModel.getPaymentType()).append("&");
        //sb.append("ItemCode").append("=").append(myCardProductModel.getItemCode()).append("&");
        sb.append("ProductName").append("=").append(productName).append("&");
        sb.append("Amount").append("=").append(mycardProduct.amount).append("&");
        sb.append("Currency").append("=").append(mycardProduct.currency).append("&");
        sb.append("SandBoxMode").append("=").append(app.sandBoxMode).append("&");
        sb.append("Hash").append("=").append(preHashValue);
        String postBody = sb.toString();

        String url = "https://b2b.mycard520.com.tw/MyBillingPay/api/AuthGlobal";
        if (app.sandBoxMode.equals("true"))
            url = "https://test.b2b.mycard520.com.tw/MyBillingPay/api/AuthGlobal";

        Map<String, String> headers = new HashMap<>();
        try {
            headers.put("content-length", "" + postBody.getBytes("UTF-8").length);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept", "*/*");

        Http http = new Http();
        String response = http.sendHttpsPost(url, postBody, headers);
        if (response.isEmpty()) {
            logger.info("MyCard_ReqAuthCode {} {} {} HttpFail", AppUtils.serialize(req), url, postBody);

            MycardReqAuthCodeRsp rsp = new MycardReqAuthCodeRsp();
            rsp.code = ReturnCode.CODE_MYCARD_GET_AUTHCODE_FAIL;
            rsp.codeDesc = ReturnCode.CODE_MYCARD_GET_AUTHCODE_FAIL_DESC;
            return rsp;
        }

        try {
            JsonNode root = mapper.readTree(response);
            if (root.get("ReturnCode").asText().equals("1")) {
                String authCode = root.get("AuthCode").asText();
                String tradeSeq = root.get("TradeSeq").asText();
                String inGameSaveType = root.get("InGameSaveType").asText();

                MycardReqAuthCodeRsp rsp = new MycardReqAuthCodeRsp();
                rsp.sandBoxMode = (app.sandBoxMode.equals("true") ? true : false);
                rsp.code = ReturnCode.CODE_OK;
                rsp.codeDesc = ReturnCode.CODE_OK_DESC;
                rsp.authCode = authCode;


                // 将本次数据存储到redis
                ObjectNode node = mapper.createObjectNode();
                node.put("appId", req.appId);
                node.put("userId", req.userId);
                node.put("createTime", System.currentTimeMillis());
                node.put("facTradeSeq", order); // 交易序号，厂商自定义'
                node.put("tradeType", app.tradeType); // '交易模式，1androidsdk，2web'
                node.put("customerId", req.customerId); // 会员代号，用户id
                node.put("serverId", req.serverId);
                node.put("paymentType", mycardProduct.paymentType); // mycard付费方式, INGAME点卡，COSTPOINT会员扣点, FA018上海webatm, FA029中华电信HiNet连扣, FA200000002测试用
                node.put("itemCode", mycardProduct.itemCode); // mycard品项代码
                node.put("productName", mycardProduct.productName);
                node.put("amount", mycardProduct.amount); // 交易金额，可以为整数，若有小鼠最多2位
                node.put("currency", mycardProduct.currency); // 货币种类, TWD/HKD/USD
                node.put("sandBoxMode", app.sandBoxMode); // 是否为测试环境
                node.put("authCode", authCode); // 授权码
                node.put("tradeSeq", tradeSeq); // mycard交易序号
                node.put("inGameSaveType", inGameSaveType);
                node.put("cparam", req.cparam);
                node.put("payStatus", 0); // 0-未完成 1-已经完成并入库 2-已经完成但入库失败

                redisTemplate.opsForValue().set("mycard" + node.get("facTradeSeq").asText(), mapper.writeValueAsString(node), 7, TimeUnit.DAYS);

                logger.info("MyCard_ReqAuthCode {} {} {} OK", AppUtils.serialize(req), mapper.writeValueAsString(node), response);

                return rsp;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        logger.info("MyCard_ReqAuthCode {} {} ReqFail", AppUtils.serialize(req), response);

        MycardReqAuthCodeRsp rsp = new MycardReqAuthCodeRsp();
        rsp.code = ReturnCode.CODE_MYCARD_GET_AUTHCODE_FAIL;
        rsp.codeDesc = ReturnCode.CODE_MYCARD_GET_AUTHCODE_FAIL_DESC;
        return rsp;
    }

    public MycardCompleteRsp doComplete(MycardCompleteReq req) {
        if (req.facTradeSeq.isEmpty() || req.session.isEmpty()) {
            logger.info("MyCard_Complete {} MsgErr", AppUtils.serialize(req));

            MycardCompleteRsp rsp = new MycardCompleteRsp();
            rsp.code = ReturnCode.CODE_MSG_ERR;
            rsp.codeDesc = ReturnCode.CODE_MSG_ERR_DESC;
            return rsp;
        }

        App app = appDao.getAppById(req.appId);
        if (app == null) {
            logger.info("MyCard_Complete {} NoAppConfig", AppUtils.serialize(req));

            MycardCompleteRsp rsp = new MycardCompleteRsp();
            rsp.code = ReturnCode.CODE_MYCARD_NO_CONFIG;
            rsp.codeDesc = ReturnCode.CODE_MYCARD_NO_CONFIG_DESC;
            return rsp;
        }

        Account account = accountDao.getAccountBySession(req.session);
        if (account == null || req.appId != app.appId || req.userId != account.userId) {
            logger.info("MyCard_Complete {} NoSession", AppUtils.serialize(req));

            MycardCompleteRsp rsp = new MycardCompleteRsp();
            rsp.code = ReturnCode.CODE_SESSION_NO;
            rsp.codeDesc = ReturnCode.CODE_SESSION_NO_DESC;
            return rsp;
        }

        String result = redisTemplate.opsForValue().get("mycard" + req.facTradeSeq);
        if (result == null) {
            logger.info("MyCard_Complete {} NoTradeInfo", AppUtils.serialize(req));

            MycardCompleteRsp rsp = new MycardCompleteRsp();
            rsp.code = ReturnCode.CODE_MYCARD_NO_SEQ;
            rsp.codeDesc = ReturnCode.CODE_MYCARD_NO_SEQ_DESC;
            return rsp;
        }

        try {
            JsonNode trade = mapper.readTree(result);
            if (trade.get("payStatus").asInt() != 0) {
                logger.info("MyCard_Complete {} {} USED", AppUtils.serialize(req), result);

                MycardCompleteRsp rsp = new MycardCompleteRsp();
                rsp.code = ReturnCode.CODE_IAP_ORDER_EXIST;
                rsp.codeDesc = ReturnCode.CODE_IAP_ORDER_EXIST_DESC;
                return rsp;
            }

            // 验证交易
            if (!doTradeQuery(trade)) {
                logger.info("MyCard_Complete {} {} TradeQueryFail", AppUtils.serialize(req), mapper.writeValueAsString(trade));

                MycardCompleteRsp rsp = new MycardCompleteRsp();
                rsp.code = ReturnCode.CODE_MYCARD_VERIFY_AUTHCODE_FAIL;
                rsp.codeDesc = ReturnCode.CODE_MYCARD_VERIFY_AUTHCODE_FAIL_DESC;
                return rsp;
            }

            // 请款
            if (!doGetMoney(trade)) {
                logger.info("MyCard_Complete {} {} GetMoneyFail", AppUtils.serialize(req), mapper.writeValueAsString(trade));

                MycardCompleteRsp rsp = new MycardCompleteRsp();
                rsp.code = ReturnCode.CODE_MYCARD_GET_MONEY_FAIL;
                rsp.codeDesc = ReturnCode.CODE_MYCARD_GET_MONEY_FAIL_DESC;
                return rsp;
            }

            if (!saveTrade(trade, app)) {
                logger.info("MyCard_Complete {} {} DBErr", AppUtils.serialize(req), mapper.writeValueAsString(trade));

                MycardCompleteRsp rsp = new MycardCompleteRsp();
                rsp.code = ReturnCode.CODE_DB_ERR;
                rsp.codeDesc = ReturnCode.CODE_DB_ERR_DESC;
                return rsp;
            }

            MycardCompleteRsp rsp = new MycardCompleteRsp();
            rsp.code = ReturnCode.CODE_OK;
            rsp.codeDesc = ReturnCode.CODE_OK_DESC;
            rsp.facTradeSeq = trade.get("facTradeSeq").asText();
            rsp.itemCode = trade.get("itemCode").asText();

            logger.info("MyCard_Complete {} {} OK", AppUtils.serialize(req), mapper.writeValueAsString(trade));
            return rsp;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        MycardCompleteRsp rsp = new MycardCompleteRsp();
        rsp.code = ReturnCode.CODE_MYCARD_VERIFY_AUTHCODE_FAIL;
        rsp.codeDesc = ReturnCode.CODE_MYCARD_VERIFY_AUTHCODE_FAIL_DESC;
        return rsp;
    }

    public String doNotify(String notifyMsg) {
        try {
            JsonNode root = mapper.readTree(notifyMsg);
            if (root.get("ReturnCode").asText().equals("1")) {
                String facServiceId = root.get("FacServiceId").asText();
                int totalNum = root.get("TotalNum").asInt();
                JsonNode facTradeSeqs = root.get("FacTradeSeq");
                for (JsonNode facTradeSeq : facTradeSeqs) {
                    String tradeStr = redisTemplate.opsForValue().get("mycard" + facTradeSeq.asText());
                    if (tradeStr != null) {
                        JsonNode trade = mapper.readTree(tradeStr);

                        App app = appDao.getAppById(trade.get("appId").asInt());
                        if (app == null) {
                            logger.info("Mycard_Notify {} {} NoAppConfig", notifyMsg, trade);
                            continue;
                        }

                        // 开始请款
                        // 获取订单信息
                        // 验证交易
                        if (!doTradeQuery(trade)) {
                            logger.info("Mycard_Notify {} {} TradeQueryFail", notifyMsg, trade);
                            continue;
                        }

                        // 请款
                        if (!doGetMoney(trade)) {
                            logger.info("Mycard_Notify {} {} GetMoneyFail", notifyMsg, trade);
                            continue;
                        }

                        saveTrade(trade, app);
                    } else {
                        logger.info("Mycard_Notify {} {} NoDataInRedis", notifyMsg, facTradeSeq);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    public String doDiff(String body) {
        String result = "";
        List<Map<String, Object>> results = null;

        String myCardTradeNo = trim(body); //request.queryParams("MyCardTradeNo");
        if (!myCardTradeNo.contains("MyCardTradeNo")) {
            String[] array = myCardTradeNo.split("&");
            if (array.length != 2) {
                logger.info("Mycard_Diff {} DateErr", body);
                return "";
            }
            String startDateTimeStr = array[0]; //request.queryParams("StartDateTime");
            String endDateTimeStr = array[1]; //request.queryParams("EndDateTime");
            array = startDateTimeStr.split("=");
            if (array.length != 2) {
                logger.info("Mycard_Diff {} startDateTimeStrErr", body);
                return "";
            }
            startDateTimeStr = array[1];
            array = endDateTimeStr.split("=");
            if (array.length != 2) {
                logger.info("Mycard_Diff {} endDateTimeStrErr", body);
                return "";
            }
            endDateTimeStr = array[1];

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                Date startDateTime = sdf.parse(startDateTimeStr);
                Date endDateTime = sdf.parse(endDateTimeStr);

                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                startDateTimeStr = sdf.format(startDateTime);
                endDateTimeStr = sdf.format(endDateTime);
            } catch (ParseException ex) {
                ex.printStackTrace();

                logger.info("Mycard_Diff {} {}", body, ex);
                return "";
            }

            results = payInfoDao.getMycardTrades(startDateTimeStr, endDateTimeStr);
        } else {
            String[] array = myCardTradeNo.split("=");
            if (array.length != 2) {
                logger.info("Mycard_Diff {} myCardTradeNoErr", body);
                return "";
            }
            myCardTradeNo = array[1];
            results = payInfoDao.getMycardTrade(myCardTradeNo);
        }

        if (results != null) {
            for (Map map : results) {
                result += (String) map.get("paymentType") + ',';
                result += (String) map.get("tradeSeq") + ',';
                result += (String) map.get("myCardTradeNo") + ',';
                result += (String) map.get("facTradeSeq") + ',';
                result += (String) map.get("customerId") + ',';
                result += (String) map.get("amount") + ',';
                result += (String) map.get("currency") + ',';

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                String tradeDateStr = sdf.format(map.get("tradeDateTime"));

                result += tradeDateStr;
                result += "<BR>";
            }
        }

        logger.info("Mycard_Diff {} {}", body, result);
        return result;
    }

    private String trim(String str) {
        String dest = "";
        if (str!=null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }

    private boolean doTradeQuery(JsonNode trade) throws IOException {
        String url = "https://b2b.mycard520.com.tw/MyBillingPay/api/TradeQuery";
        if (trade.get("sandBoxMode").asText().equals("true") ? true : false)
            url = "https://test.b2b.mycard520.com.tw/MyBillingPay/api/TradeQuery";

        String reqBody = "Authcode=" + trade.get("authCode").asText();

        Map<String, String> headers = new HashMap<>();
        try {
            headers.put("content-length", "" + reqBody.getBytes("UTF-8").length);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept", "*/*");

        Http http = new Http();
        String result = http.sendHttpsPost(url, reqBody, headers);
        logger.info("Mycard_TradeQuery {} {}", reqBody, result);
        if (!result.isEmpty()) {
            JsonNode tradeQuery = mapper.readTree(result);
            if (tradeQuery.get("ReturnCode").asText().equals("1")) {
                String payResult = tradeQuery.get("PayResult").asText();
                String facTradeSeq = tradeQuery.get("FacTradeSeq").asText();
                if (payResult.equals("3")) {
                    String paymentType = tradeQuery.get("PaymentType").asText();
                    String amount = tradeQuery.get("Amount").asText();
                    String currency = tradeQuery.get("Currency").asText();

                    //1.PaymentType = INGAME 時，傳 MyCard 卡片號碼
                    //2.PaymentType = COSTPOINT 時，傳會員扣點交易序號，格式為 MMS 開頭+數字
                    //3.其餘 PaymentType 為 Billing 小額付款交易，傳 Billing 交易序號
                    //特別注意: 交易時，同一個 MyCard 卡片號碼、會員扣點交易序號和 Billing 交易序號只
                    //能被儲值成功一次，請廠商留意，以免造成重複儲值的情形
                    String myCardTradeNo = tradeQuery.get("MyCardTradeNo").asText();
                    String myCardType = tradeQuery.get("MyCardType").asText();
                    String promoCode = tradeQuery.get("PromoCode").asText();
                    String serialId = tradeQuery.get("SerialId").asText();

                    ((ObjectNode) trade).put("paymentType", paymentType);
                    ((ObjectNode) trade).put("amount", amount);
                    ((ObjectNode) trade).put("currency", currency);

                    ((ObjectNode) trade).put("myCardTradeNo", myCardTradeNo);
                    ((ObjectNode) trade).put("myCardType", myCardType);
                    ((ObjectNode) trade).put("promoCode", promoCode);
                    ((ObjectNode) trade).put("serialId", serialId);

                    return true;
                }
            }
        }

        return false;
    }

    private boolean doGetMoney(JsonNode trade) throws IOException {
        String url = "https://b2b.mycard520.com.tw/MyBillingPay/api/PaymentConfirm";
        if (trade.get("sandBoxMode").asText().equals("true") ? true : false)
            url = "https://test.b2b.mycard520.com.tw/MyBillingPay/api/PaymentConfirm";

        String reqBody = "Authcode=" + trade.get("authCode").asText();

        Map<String, String> headers = new HashMap<>();
        try {
            headers.put("content-length", "" + reqBody.getBytes("UTF-8").length);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept", "*/*");

        Http http = new Http();
        String result = http.sendHttpsPost(url, reqBody, headers);
        logger.info("Mycard_GetMoney {} {}", reqBody, result);
        if (result.isEmpty())
            return false;

        JsonNode getMoney = mapper.readTree(result);
        if (getMoney.get("ReturnCode").asText().equals("1")) {
            String tradeSeq = getMoney.get("TradeSeq").asText();
            String serialId = getMoney.get("SerialId").asText();

            ((ObjectNode) trade).put("serialId", serialId);

            return true;
        }

        return false;
    }

    private boolean saveTrade(JsonNode trade, App app) throws IOException {
        // 记录mycard交易信息，用于mycard交易差异比对
        payInfoDao.insertMycardTrade(
                trade.get("paymentType").asText(),
                trade.get("tradeSeq").asText(),
                trade.get("myCardTradeNo").asText(),
                trade.get("facTradeSeq").asText(),
                trade.get("customerId").asText(),
                trade.get("amount").asText(),
                trade.get("currency").asText()
        );

        // 存储交易
        PayInfo pmodel = new PayInfo();
        pmodel.order = trade.get("facTradeSeq").asText();
        pmodel.appId = trade.get("appId").asInt();
        pmodel.userId = trade.get("userId").asLong();
        pmodel.gameRoleId = trade.get("customerId").asText();
        pmodel.serverId = trade.get("serverId").asText();
        pmodel.payStatus = Const.PAY_STATUS_WAITPUSH;
        pmodel.productId = trade.get("itemCode").asText();
        pmodel.amount = trade.get("amount").asText();
        pmodel.currency = trade.get("currency").asText();
        pmodel.channelType = Const.PAY_CHANNEL_MYCARD;
        pmodel.channelOrder = trade.get("tradeSeq").asText();
        pmodel.sandbox = (trade.get("sandBoxMode").asText().equals("true") ? 1 : 0);
        pmodel.cparam = AppUtils.b64encode(trade.get("cparam").asText());

        if (!payInfoDao.insertOfficalPay(pmodel)) {
            ((ObjectNode)trade).put("payStatus", 2);// 0-未完成 1-已经完成并入库 2-已经完成但入库失败
            redisTemplate.opsForValue().set("mycard" + trade.get("facTradeSeq").asText(), mapper.writeValueAsString(trade), 7, TimeUnit.DAYS);

            logger.info("Mycard_SaveTrade {} DBErr", pmodel);
            return false;
        }

        ((ObjectNode)trade).put("payStatus", 1);// 0-未完成 1-已经完成并入库 2-已经完成但入库失败
        redisTemplate.opsForValue().set("mycard" + trade.get("facTradeSeq").asText(), mapper.writeValueAsString(trade), 7, TimeUnit.DAYS);

        if (!app.notifyUrl.isEmpty()) {
            pushTask.submit(app.notifyUrl, pmodel, app.appSecret);
        }

        return true;
    }
}
