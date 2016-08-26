package com.seastar.task.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seastar.comm.AppUtils;
import com.seastar.domain.PayInfo;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wjl on 2016/8/24.
 */
@Component
public class PushTask {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ConcurrentHashMap<String, String> operativeDataMap = new ConcurrentHashMap<>();
    private PriorityBlockingQueue<PushItem> blockingQueue;
    private CloseableHttpAsyncClient asyncClient;

    private ObjectMapper mapper = new ObjectMapper();
    private Logger logger = LogManager.getLogger(PushTask.class);

    private long[] PUSH_GAP = new long[] { 5 * 60 * 1000, 15 * 60 * 1000, 60 * 60 * 1000, 3 * 60 * 60 * 1000 };

    @PostConstruct
    public void init() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(3000)
                .setConnectTimeout(3000)
                .build();
        asyncClient = HttpAsyncClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        asyncClient.start();

        blockingQueue = new PriorityBlockingQueue<>(200, (PushItem x, PushItem y) -> {
            if (x.time < y.time)
                return -1;
            else if (x.time > y.time)
                return 1;
            else
                return 0;
        });
    }

    @Async
    public void submit(String url, PayInfo payInfo, String appSecret) {
        // 提取附赠数据
        String key = payInfo.appId + payInfo.productId;
        String value = operativeDataMap.get(key);

        String coin = "0";
        String giveCoin = "0";
        String money = "0";
        if (value == null) {
            try {
                Map<String, Object> resultMap =
                        jdbcTemplate.queryForMap("select virtualCoin, giveVirtualCoin, money from t_operative where appId=? and productId=?",
                        payInfo.appId, payInfo.productId);

                coin = (String) resultMap.get("virtualCoin");
                giveCoin = (String) resultMap.get("giveVirtualCoin");
                money = (String) resultMap.get("money");
                operativeDataMap.put(key, coin + "," + giveCoin + money);
            } catch (DataAccessException ex) {
                ex.printStackTrace();
            }
        } else {
            String[] arr = value.split(",");
            if (arr.length == 3) {
                coin = arr[0];
                giveCoin = arr[1];
                money = arr[2];
            }
        }

        try {
            ObjectNode root = mapper.createObjectNode();

            root.put("order", payInfo.order);
            root.put("appId", payInfo.appId + "");
            root.put("userId", payInfo.userId + "");
            root.put("gameRoleId", payInfo.gameRoleId);
            root.put("serverId", payInfo.serverId);
            root.put("channelType", payInfo.channelType + "");
            root.put("productId", payInfo.productId);
            root.put("productAmount", 1 + "");
            root.put("money", money);
            root.put("currency", payInfo.currency);
            root.put("status", 0 + "");
            root.put("sandbox", payInfo.sandbox + "");
            root.put("cparam", payInfo.cparam);
            root.put("virtualCoin", coin);
            root.put("giveVirtualCoin", giveCoin);


            String md5Origin = payInfo.order + "|" +
                    payInfo.appId + "|" +
                    payInfo.userId + "|" +
                    payInfo.gameRoleId + "|" +
                    payInfo.serverId + "|" +
                    payInfo.channelType + "|" +
                    payInfo.productId + "|" +
                    1 + "|" +
                    //data.getString("money") +
                    payInfo.currency + "|" +
                    0 + "|" +
                    payInfo.sandbox + "|" +
                    payInfo.cparam + "|" +
                    coin + "|" +
                    giveCoin + "|" +
                    appSecret;

            root.put("sign", AppUtils.md5encode(md5Origin));

            PushItem item = new PushItem();
            item.url = url;
            item.data = mapper.writeValueAsString(root);
            item.order = payInfo.order;
            item.retry = 0;
            item.time = System.currentTimeMillis();

            blockingQueue.offer(item);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Scheduled(fixedDelay = 200)
    public void scanDelay() {
        try {
            long curTime = System.currentTimeMillis();
            while (true) {
                PushItem top = blockingQueue.peek();
                if (top == null || top.time > curTime)
                    break;

                PushItem item = blockingQueue.poll();

                // 开始推送
                logger.info("{} Push", AppUtils.serialize(item));
                asyncClient.execute(HttpAsyncMethods.createPost(item.url, item.data, ContentType.APPLICATION_JSON),
                        new PushResponseConsumer(),
                        new FutureCallback<String>() {

                            @Override
                            public void completed(final String response) {
                                onPushResult(trim(response), item);
                            }

                            @Override
                            public void failed(final Exception ex) {
                                onPushResult("", item);
                            }

                            @Override
                            public void cancelled() {

                            }

                            private void onPushResult(String response, PushItem item) {
                                if (response.equals(item.order)) {
                                    // 推送成功
                                    try {
                                        logger.info("{} PushSuccess", AppUtils.serialize(item));
                                        jdbcTemplate.update("update t_pay_info set payStatus=?, notifyTime=now() where `order`=?", 2, item.order);
                                    } catch (DataAccessException e) {
                                        e.printStackTrace();
                                        logger.info("{} {} PushSuccess", AppUtils.serialize(item), e);
                                    }
                                } else {
                                    // 推送失败，进入重试队列
                                    if (item.retry <= 3) {
                                        item.time = System.currentTimeMillis() + PUSH_GAP[item.retry];
                                        item.retry++;

                                        logger.info("{} {} WaitRetry", AppUtils.serialize(item), response);
                                        blockingQueue.offer(item);
                                    } else {
                                        // 彻底失败
                                        try {
                                            logger.info("{} {} PushFail", AppUtils.serialize(item), response);
                                            jdbcTemplate.update("update t_pay_info set payStatus=?, notifyTime=now() where `order`=?", 3, item.order);
                                        } catch (DataAccessException e) {
                                            e.printStackTrace();
                                            logger.info("{} {} {} PushFail", AppUtils.serialize(item), response, e);
                                        }
                                    }
                                }
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
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("{}", e);
        }
    }

    @PreDestroy
    public void destroy() throws IOException {
        asyncClient.close();
    }
}
