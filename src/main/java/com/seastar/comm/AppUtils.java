package com.seastar.comm;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by wjl on 2016/6/3.
 */
public class AppUtils {

    private static ObjectMapper mapper = new ObjectMapper();

    public static boolean hasNull(Object obj) {
        try {
            Method[] methods = obj.getClass().getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().startsWith("get")) {
                    if (m.getReturnType().getName().equals("java.lang.String")) {
                        String ret = (String) m.invoke(obj);
                        if (ret == null)
                            return true;
                    }
                }
            }

            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }

    public static String serialize(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "";
        }
    }

    public static String createUUID() {
        // 尽可能保证不重复
        return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
    }


    public static boolean validateDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isEmpty() || !deviceId.matches("[a-zA-Z0-9-_:]{5,50}"))
            return false;
        return true;
    }

    public static boolean validateLocale(String locale) {
        if (locale == null || locale.isEmpty() || !locale.matches("[a-zA-Z-]{2,12}"))
            return false;

        return true;
    }

    public static boolean validateThirdPartyUserId(String id) {
        if (id == null || id.isEmpty() || !id.matches("[a-zA-Z0-9]{1,60}"))
            return false;

        return true;
    }

    public static boolean validateEmail(String email) {
        if (email == null || email.isEmpty() || !email.matches("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*"))
            return false;

        return true;
    }

    public static boolean validateUserName(String name) {
        if (name == null || name.isEmpty() || !name.matches("[a-zA-Z][a-zA-Z0-9]{5,16}"))
            return false;

        return true;
    }

    public static boolean validatePassword(String password) {
        if (password == null || password.isEmpty() || !password.matches("[a-zA-Z0-9]{8,32}"))
            return false;

        return true;
    }

    public static boolean validateGameRoleId(String id) {
        if (id == null || id.isEmpty() || !id.matches("[a-zA-Z0-9]{1,60}"))
            return false;

        return true;
    }

    public static boolean validateServerId(String id) {
        if (id == null)
            return false;

        if (id.isEmpty())
            return true;

        if (!id.matches("[a-zA-Z0-9]{1,10}"))
            return false;

        return true;
    }

    public static boolean validatePrice(String price) {
        if (price == null || price.isEmpty() || !price.matches("[0-9.]{1,20}"))
            return false;

        return true;
    }

    private static HashMap<String, Integer> currencies = new HashMap<String, Integer>() {
        {
            put("AED", 1); // 阿联酋迪拉姆
            put("AUD", 1); // 澳元
            put("MOP", 1); // 澳门元
            put("DZD", 1); // 阿尔及利亚第纳尔
            put("OMR", 1); // 阿曼里亚尔
            put("EGP", 1); // 埃及磅

            put("BYR", 1); // 白俄罗斯卢布
            put("BRL", 1); // 巴西雷亚尔
            put("PLN", 1); // 波兰兹罗提
            put("BHD", 1); // 巴林第纳尔
            put("BGN", 1); // 保加利亚列弗
            put("ISKD", 1); // 冰岛克朗
            put("DKKE", 1); // 丹麦克朗
            put("RUB", 1); // 俄罗斯卢布

            put("PHPG", 1); // 菲律宾比索
            put("HKD", 1); // 港元
            put("COP", 1); // 哥伦比亚比索
            put("CRCH", 1); // 哥斯达黎加科朗
            put("KRWJ", 1); // 韩元
            put("CAD", 1); // 加元
            put("CZK", 1); // 捷克克朗
            put("KHR", 1); // 柬埔寨瑞尔

            put("HRK", 1); // 克罗地亚库纳
            put("QAR", 1); // 卡塔尔里亚尔
            put("KWD", 1); // 科威特第纳尔
            put("KESL", 1); // 肯尼亚先令
            put("LAK", 1); // 老挝基普
            put("RON", 1); // 罗马尼亚列伊
            put("LBP", 1); // 黎巴嫩镑
            put("CNHM", 1); // 离岸人民币
            put("USD", 1); // 美元
            put("BUK", 1); // 缅甸元
            put("MYR", 1); // 马来西亚林吉特
            put("MAD", 1); // 摩洛哥道拉姆
            put("MXNN", 1); // 墨西哥元
            put("NOK", 1); // 挪威克朗
            put("ZAR", 1); // 南非兰特

            put("EURR", 1); // 欧元
            put("CNY", 1); // 人民币
            put("CHF", 1); // 瑞士法郎
            put("JPY", 1); // 日元
            put("SEKS", 1); // 瑞典克朗
            put("SAR", 1); // 沙特里亚尔
            put("LKR", 1); // 斯里兰卡卢比
            put("RSDT", 1); // 塞尔维亚第纳尔
            put("THB", 1); // 泰铢
            put("TZS", 1); // 坦桑尼亚先令

            put("BND", 1); // 文莱元
            put("UGXX", 1); // 乌干达先令
            put("ZMK", 1); // 新的赞比亚克瓦查
            put("SYP", 1); // 叙利亚镑
            put("NZD", 1); // 新西兰元
            put("TRY", 1); // 新土耳其里拉
            put("SGD", 1); // 新加坡元
            put("TWD", 1); // 新台币
            put("HUFY", 1); // 匈牙利福林
            put("GBP", 1); // 英镑
            put("JOD", 1); // 约旦第纳尔
            put("IQD", 1); // 伊拉克第纳尔
            put("VND", 1); // 越南盾
            put("ILS", 1); // 以色列新锡克尔
            put("INR", 1); // 印度卢比
            put("IDRZ", 1); // 印尼卢比
            put("CLP", 1); // 智利比索
        }

    };
    public static boolean validateCurrency(String currency) {
        if (currency == null || currency.isEmpty() || !currencies.containsKey(currency.toUpperCase()))
            return false;

        return true;
    }

    public static String md5encode(String s) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
                'f' };
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(s.getBytes());
            byte[] bytes = md5.digest();

            StringBuffer stringbuffer = new StringBuffer(2 * bytes.length);
            for (int l = 0; l < bytes.length; l++) {
                char c0 = hexDigits[(bytes[l] & 0xf0) >> 4];
                char c1 = hexDigits[bytes[l] & 0xf];
                stringbuffer.append(c0);
                stringbuffer.append(c1);
            }
            return stringbuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String b64encode(String s) {
        s = s.trim();
        if (s.isEmpty())
            return s;
        return Base64.getEncoder().encodeToString(s.getBytes());
    }

    public static String b64decode(String s) {
        if (s.isEmpty())
            return s;
        return new String(Base64.getDecoder().decode(s));
    }

    public static String sha256encode(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(s.getBytes());
            byte[] result = md.digest();

            String des = "";
            String tmp = null;
            for (int i = 0; i < result.length; i++) {
                tmp = (Integer.toHexString(result[i] & 0xFF));
                if (tmp.length() == 1) {
                    des += "0";
                }
                des += tmp;
            }

            return des;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

}
