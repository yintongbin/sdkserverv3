package com.seastar;

import com.seastar.comm.IDGenerate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by wjl on 2016/8/24.
 * @Order 注解的执行优先级是按value值从小到大顺序。
 */
@Component
@Order(value = 1)
public class StartupRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        long maxUserId = 0;
        long maxOrder = 0;
        try {
            Map<String, Object> queryMap = jdbcTemplate.queryForMap("select userId from t_user_base order by userId desc limit 1");
            maxUserId = (Long) queryMap.get("userId");
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        try {
            Map<String, Object> queryMap = jdbcTemplate.queryForMap("select `order` from t_pay_info order by createTime desc limit 1");
            String order = (String) queryMap.get("order");
            order = order.substring(10);
            maxOrder = Long.parseLong(order);
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        IDGenerate.init(maxUserId, maxOrder);
    }
}
