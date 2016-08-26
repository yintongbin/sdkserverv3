package com.seastar.comm;

import java.util.Random;

/**
 * Created by wjl on 2016/4/11.
 * 本类非常重要，玩家id和订单id通过他来生成
 */
public class IDGenerate {
    private static long id1 = 0;
    private static long id2 = 0;

    public static void init(long curUserId, long curOrderId) {
        id1 = ++curUserId;
        id2 = ++curOrderId;
    }

    public synchronized static long getId() {
        if (id1 >= Long.MAX_VALUE)
            id1 = 0;
        return id1++;
    }

    public synchronized static String getOrder() {
        if (id2 >= Long.MAX_VALUE)
            id2 = 0;

        String prefix = getRandomString(8);
        return String.format("ST" + prefix + "%020d", id2++);
    }

    public static String getRandomString(int length) { //length表示生成字符串的长度
        String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }
}
