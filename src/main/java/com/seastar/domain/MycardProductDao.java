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
public class MycardProductDao {
    private String prefix = "mycardProduct";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    public MycardProduct getProduct(int appId, String itemCode) {
        // 先从redis获取
        String result = redisTemplate.opsForValue().get(prefix + appId);
        if (result != null) {
            try {
                return mapper.readValue(result, MycardProduct.class);
            } catch (IOException e) {
                e.printStackTrace();

                return null;
            }
        }

        // 查询商品信息
        try {
            Map<String, Object> mycardMap = jdbcTemplate.queryForMap(
                    "select appId, paymentType, itemCode, productName, amount, currency from t_mycard_product where appId=? and itemCode=?",
                    appId, itemCode);
            MycardProduct product = mapper.readValue(mapper.writeValueAsString(mycardMap), MycardProduct.class);
            if (product != null) {
                redisTemplate.opsForValue().set(prefix + appId + itemCode, mapper.writeValueAsString(product), 365, TimeUnit.DAYS);
            }

            return product;
        } catch (IOException | DataAccessException e) {
            e.printStackTrace();
        }

        return null;
    }
}
