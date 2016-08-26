package com.seastar.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by os on 2016/8/20.
 */
@Component
public class AppDao {
    private String prefix = "app";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    public App getAppById(int appId) {
        // 先从redis获取
        String result = redisTemplate.opsForValue().get(prefix + appId);
        if (result != null) {
            try {
                return mapper.readValue(result, App.class);
            } catch (IOException e) {
                e.printStackTrace();

                return null;
            }
        }

        // 获取app基本配置
        Map<String, Object> appMap = null;
        try {
            appMap = jdbcTemplate.queryForMap(
                    "select appId, status, appKey, appSecret, sandbox, notifyUrl, payType from t_app where appId=?", appId);
        } catch (DataAccessException e) {
            e.printStackTrace();

            return null;
        }

        // 获取google基本配置
        try {
            Map<String, Object> googleMap = jdbcTemplate.queryForMap(
                    "select googleKey from t_app_google where appId=?", appId);
            for (Map.Entry<String, Object> entry : googleMap.entrySet()) {
                appMap.put(entry.getKey(), entry.getValue());
            }
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        // 获取mycard配置
        try {
            Map<String, Object> mycardMap = jdbcTemplate.queryForMap("select facServiceId, tradeType, hashKey, sandBoxMode from t_app_mycard");
            for (Map.Entry<String, Object> entry : mycardMap.entrySet()) {
                appMap.put(entry.getKey(), entry.getValue());
            }
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        // 转换成App对象
        try {
            App app = mapper.readValue(mapper.writeValueAsString(appMap), App.class);
            if (app != null) {
                // 一年内不更新
                redisTemplate.opsForValue().set(prefix + appId, mapper.writeValueAsString(app), 365, TimeUnit.DAYS);
            }

            return app;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
