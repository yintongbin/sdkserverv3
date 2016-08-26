package com.seastar.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seastar.comm.Const;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by os on 2016/8/20.
 */
@Component
public class AccountDao {
    private String keyPrefixId = "userid";
    private String keyPrefixName = "username";
    private String keyPrefixThirdId = "thirdid";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    public Account getAccountByName(String userName) {
        String userId = redisTemplate.opsForValue().get(keyPrefixName + userName);
        Account account = getAccountByIdFromRedis(userId);
        if (account != null)
            return account;

        Map<String, Object> accountMap = null;
        try {
            accountMap = jdbcTemplate.queryForMap(
                    "select userId, userName, password, email, status, deviceId, locale from t_user_base where userName=?", userName);
        } catch (DataAccessException e) {
            e.printStackTrace();

            return null;
        }

        return fillThirdData(accountMap);
    }

    public Account getAccountByThirdId(String thirdUserId, int loginType) {
        String userId = redisTemplate.opsForValue().get(keyPrefixThirdId + thirdUserId + loginType);
        Account account = getAccountByIdFromRedis(userId);
        if (account != null)
            return account;

        Map<String, Object> accountMap = null;
        try {
            Map<String, Object> thirdMap = jdbcTemplate.queryForMap("select userId from t_user_login_channel where thirdUserId=? and loginType=?",
                    thirdUserId, loginType);

            accountMap = jdbcTemplate.queryForMap("select userId, userName, password, email, status, deviceId, locale from t_user_base where userId=?",
                    thirdMap.get("userId"));
        } catch (DataAccessException e) {
            e.printStackTrace();

            return null;
        }

        return fillThirdData(accountMap);
    }

    @Transactional
    public void insertAccount(Account account) {
        jdbcTemplate.update("insert into t_user_base (userId, userName, password, email, status, createTime, deviceId, locale)" +
                " values (?, ?, ?, ?, ?, now(), ?, ?)",
                account.userId, account.userName, account.password, account.email, account.status, account.deviceId, account.locale);

        if (!account.facebookUserId.isEmpty()) {
            jdbcTemplate.update("insert into t_user_login_channel (userId, thirdUserId, loginType, createTime, appId) values (?, ?, ?, now(), ?)",
                    account.userId, account.facebookUserId, Const.TYPE_FACEBOOK, account.appId);
        } else if (!account.googleUserId.isEmpty()) {
            jdbcTemplate.update("insert into t_user_login_channel (userId, thirdUserId, loginType, createTime, appId) values (?, ?, ?, now(), ?)",
                    account.userId, account.googleUserId, Const.TYPE_GOOGLE, account.appId);
        } else if (!account.gamecenterUserId.isEmpty()) {
            jdbcTemplate.update("insert into t_user_login_channel (userId, thirdUserId, loginType, createTime, appId) values (?, ?, ?, now(), ?)",
                    account.userId, account.gamecenterUserId, Const.TYPE_GAMECENTER, account.appId);
        } else if (!account.guestUserId.isEmpty()) {
            jdbcTemplate.update("insert into t_user_login_channel (userId, thirdUserId, loginType, createTime, appId) values (?, ?, ?, now(), ?)",
                    account.userId, account.guestUserId, Const.TYPE_GUEST, account.appId);
        }

        cacheAccount(account);
    }

    public boolean bindUser(Account account) {
        try {
            if (!account.facebookUserId.isEmpty()) {
                jdbcTemplate.update("insert into t_user_login_channel (userId, thirdUserId, loginType, createTime, appId) values (?, ?, ?, now(), ?)",
                        account.userId, account.facebookUserId, Const.TYPE_FACEBOOK, account.appId);
            } else if (!account.googleUserId.isEmpty()) {
                jdbcTemplate.update("insert into t_user_login_channel (userId, thirdUserId, loginType, createTime, appId) values (?, ?, ?, now(), ?)",
                        account.userId, account.googleUserId, Const.TYPE_GOOGLE, account.appId);
            } else if (!account.gamecenterUserId.isEmpty()) {
                jdbcTemplate.update("insert into t_user_login_channel (userId, thirdUserId, loginType, createTime, appId) values (?, ?, ?, now(), ?)",
                        account.userId, account.gamecenterUserId, Const.TYPE_GAMECENTER, account.appId);
            }

            cacheAccount(account);

            return true;
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateUserPwd(Account account) {
        try {
            jdbcTemplate.update("update t_user_base set password=? where userId=?",
                    account.password, account.userId);

            redisTemplate.opsForValue().set(keyPrefixId + account.userId, mapper.writeValueAsString(account), 60, TimeUnit.DAYS);

            return true;
        } catch (IOException | DataAccessException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteUserChannel(Account account, String thirdUserId, int loginType) {
        try {
            jdbcTemplate.update("delete from t_user_login_channel where thirdUserId = ? and loginType = ?", thirdUserId, loginType);

            if (loginType == Const.TYPE_FACEBOOK)
                account.facebookUserId = "";
            else if (loginType == Const.TYPE_GOOGLE)
                account.googleUserId = "";
            else if (loginType == Const.TYPE_GAMECENTER)
                account.gamecenterUserId = "";
            redisTemplate.opsForValue().set(keyPrefixId + account.userId, mapper.writeValueAsString(account), 60, TimeUnit.DAYS);
            redisTemplate.delete(keyPrefixThirdId + thirdUserId + loginType);

            return true;
        } catch (IOException | DataAccessException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    private Account getAccountByIdFromRedis(String userId) {
        if (userId != null) {
            String result = redisTemplate.opsForValue().get(keyPrefixId + userId);
            if (result != null) {
                try {
                    return mapper.readValue(result, Account.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    public Account getAccountBySession(String session) {
        String userId = redisTemplate.opsForValue().get("session" + session);
        if (userId == null) {
            return null;
        }

        return getAccountByIdFromRedis(userId);
    }

    private Account fillThirdData(Map<String, Object> accountMap) {
        // 提取第三方数据
        try {
            List<Map<String, Object>> thirdList = jdbcTemplate.queryForList("select thirdUserId, loginType from t_user_login_channel where userId=?",
                    accountMap.get("userId"));
            for (Map<String, Object> third : thirdList) {
                int type = (Integer) third.get("loginType");
                String tId = (String) third.get("thirdUserId");
                if (type == Const.TYPE_GUEST)
                    accountMap.put("guestUserId", tId);
                else if (type == Const.TYPE_FACEBOOK)
                    accountMap.put("facebookUserId", tId);
                else if (type == Const.TYPE_GOOGLE)
                    accountMap.put("googleUserId", tId);
                else if (type == Const.TYPE_GAMECENTER)
                    accountMap.put("gamecenterUserId", tId);
            }
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        try {
            Account account = mapper.readValue(mapper.writeValueAsString(accountMap), Account.class);
            cacheAccount(account);
            return account;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void cacheAccount(Account account) {
        try {
            if (!account.gamecenterUserId.isEmpty())
                redisTemplate.opsForValue().set(keyPrefixThirdId + account.gamecenterUserId + Const.TYPE_GAMECENTER,
                        account.userId + "", 60, TimeUnit.DAYS);

            if (!account.googleUserId.isEmpty())
                redisTemplate.opsForValue().set(keyPrefixThirdId + account.googleUserId + Const.TYPE_GOOGLE,
                        account.userId + "", 60, TimeUnit.DAYS);

            if (!account.facebookUserId.isEmpty())
                redisTemplate.opsForValue().set(keyPrefixThirdId + account.facebookUserId + Const.TYPE_FACEBOOK,
                        account.userId + "", 60, TimeUnit.DAYS);

            if (!account.guestUserId.isEmpty())
                redisTemplate.opsForValue().set(keyPrefixThirdId + account.guestUserId + Const.TYPE_GUEST,
                        account.userId + "", 60, TimeUnit.DAYS);

            redisTemplate.opsForValue().set(keyPrefixName + account.userName, account.userId + "", 60, TimeUnit.DAYS);
            redisTemplate.opsForValue().set(keyPrefixId + account.userId, mapper.writeValueAsString(account), 60, TimeUnit.DAYS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
