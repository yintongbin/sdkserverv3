package com.seastar.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by wjl on 2016/8/23.
 */
@Component
public class PayInfoDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public boolean insertOfficalPay(PayInfo payInfo) {
        try {
            jdbcTemplate.update("insert into t_pay_info (`order`, appId, userId, gameRoleId, serverId, payStatus, productId, amount, currency," +
                            "channelType, channelOrder, createTime, sandbox, cparam) values (" +
                            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, ?)",
                    payInfo.order,
                    payInfo.appId,
                    payInfo.userId,
                    payInfo.gameRoleId,
                    payInfo.serverId,
                    payInfo.payStatus,
                    payInfo.productId,
                    payInfo.amount,
                    payInfo.currency,
                    payInfo.channelType,
                    payInfo.channelOrder,
                    payInfo.sandbox,
                    payInfo.cparam);
            return true;
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void insertMycardTrade(String paymentType, String tradeSeq, String myCardTradeNo, String facTradeSeq, String customerId,
                                  String amount, String currency) {
        try {
            jdbcTemplate.update(
                    "insert into t_mycard_trade(paymentType,tradeSeq,myCardTradeNo,facTradeSeq,customerId,amount,currency,tradeDateTime) values (?,?,?,?,?,?,?,now())",
                    paymentType, tradeSeq, myCardTradeNo, facTradeSeq, customerId, amount, currency);
        } catch (DataAccessException e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> getMycardTrades(String startDateTime, String endDateTime) {
        try {
            return jdbcTemplate.queryForList("select paymentType,tradeSeq,mycardTradeNo,facTradeSeq,customerId,amount,currency,tradeDateTime from t_mycard_trade where tradeDateTime > ? and tradeDateTime < ?",
                    startDateTime, endDateTime);
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Map<String, Object>> getMycardTrade(String tradeNo) {
        try {
            return jdbcTemplate.queryForList("select paymentType,tradeSeq,myCardTradeNo,facTradeSeq,customerId,amount,currency,tradeDateTime from t_mycard_trade where myCardTradeNo = ?",
                    tradeNo);
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return null;
    }
}
