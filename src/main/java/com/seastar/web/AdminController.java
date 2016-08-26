package com.seastar.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by wjl on 2016/8/26.
 */
@Controller
public class AdminController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper mapper = new ObjectMapper();
    private Logger logger = LogManager.getLogger(AdminController.class);

    @RequestMapping(value = "/admin", method = RequestMethod.POST)
    public String onAdminLogin(@RequestParam(value = "username") String username,
                               @RequestParam(value = "password") String password,
                               HttpSession httpSession,
                               ModelMap map) {
        if (username == null || password == null) {
            map.addAttribute("message", "login fail");
            map.addAttribute("url", "/admin");
            return "error";
        }

        if ((username.equals("admin1") && password.equals("K!K@C*P!")) ||
                (username.equals("admin2") && password.equals("TkHjLILM")) ||
                (username.equals("admin3") && password.equals("%Aqm5ZxK")) ||
                (username.equals("admin4") && password.equals("ARRyo&OX"))) {

            httpSession.setAttribute("username", username);
            httpSession.setAttribute("password", password);

            logger.info("{} {} login", username, password);

            return "redirect:dashboard.html";
        }

        map.addAttribute("message", "login fail");
        map.addAttribute("url", "/admin");
        return "error";
    }

    @RequestMapping(value = "/admin/app_list", method = RequestMethod.GET)
    @ResponseBody
    public String getAppList(HttpSession httpSession) {
        ObjectNode node = mapper.createObjectNode();
        ArrayNode array = node.putArray("apps");

        if (hasSession(httpSession)) {
            // 遍历数据库
            logger.info("{} applist", httpSession.getAttribute("username"));

            try {
                List<Map<String, Object>> list = jdbcTemplate.queryForList("select * from t_app");
                for (Map map: list) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    map.put("createTime", sdf.format(map.get("createTime")));
                    array.add(mapper.readTree(mapper.writeValueAsString(map)));
                }

                return mapper.writeValueAsString(node);
            } catch (DataAccessException | IOException e) {
                e.printStackTrace();
            }
        }

        return "";
    }

    @RequestMapping(value = "/admin/app_details", method = RequestMethod.GET)
    @ResponseBody
    public String getAppDetails(@RequestParam(value = "appId") String appId, HttpSession httpSession) {
        if (hasSession(httpSession) && appId != null) {
            logger.info("{} appdetails", httpSession.getAttribute("username"));

            Map<String, Object> map = null;
            JsonNode node = null;
            try {
                map = jdbcTemplate.queryForMap("select * from t_app where appId=?", appId);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                map.put("createTime", sdf.format(map.get("createTime")));

                node = mapper.readTree(mapper.writeValueAsString(map));
                ((ObjectNode) node).put("code", 1);
            } catch (DataAccessException | IOException e) {
                e.printStackTrace();
            }

            try {
                // 提取google配置
                Map<String, Object> google = jdbcTemplate.queryForMap("select * from t_app_google where appId=?", appId);
                if (google != null && google.size() > 0) {
                    ((ObjectNode) node).put("googleKey", (String) google.get("googleKey"));
                }
            } catch (DataAccessException e) {
                e.printStackTrace();
            }

            try {
                if (node == null) {
                    ObjectNode node1 = mapper.createObjectNode();
                    node1.put("code", 0);
                    return mapper.writeValueAsString(node1);
                } else {
                    return mapper.writeValueAsString(node);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return "";
    }

    @RequestMapping(value = "/admin/app_modify", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public String doAppModify(@RequestBody String body, HttpSession httpSession) {
        try {
            ObjectNode result = mapper.createObjectNode();
            if (hasSession(httpSession)) {
                logger.info("{} {} appmodify", httpSession.getAttribute("username"), body);

                JsonNode node = mapper.readTree(URLDecoder.decode(body, "UTF-8"));
                if (node != null) {
                    jdbcTemplate.update("update t_app set appName=?, status=?, appKey=?, appSecret=?, sandbox=?, version=?, notifyUrl=?, payType=? where appId=?",
                            node.get("appName").asText(),
                            node.get("status").asInt(),
                            node.get("appKey").asText(),
                            node.get("appSecret").asText(),
                            node.get("sandbox").asInt(),
                            node.get("version").asText(),
                            node.get("notifyUrl").asText(),
                            node.get("payType").asInt(),
                            node.get("appId").asInt());

                    int rowCount = jdbcTemplate.queryForObject("select count(appId) from t_app_google where appId=" + node.get("appId").asText(), Integer.class);
                    if (rowCount > 0) {
                        jdbcTemplate.update("update t_app_google set googleKey=? where appId=?",
                                node.get("googleKey").asText(),
                                node.get("appId").asInt());
                    } else {
                        jdbcTemplate.update("insert into t_app_google (appId, createTime, googleKey) values (?,now(),?)",
                                node.get("appId").asInt(),
                                node.get("googleKey").asText());
                    }

                    result.put("code", 1);
                    System.out.println(body + " " + httpSession.getAttribute("username"));
                    return mapper.writeValueAsString(result);
                }
            }

            result.put("code", 0);
            return mapper.writeValueAsString(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @RequestMapping(value = "/admin/app_add", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public String doAppAdd(@RequestBody String body, HttpSession httpSession) {
        try {
            logger.info("{} {} appadd", httpSession.getAttribute("username"), body);

            ObjectNode result = mapper.createObjectNode();
            if (hasSession(httpSession)) {
                JsonNode node = mapper.readTree(URLDecoder.decode(body, "UTF-8"));
                if (node != null) {

                    int appId = jdbcTemplate.queryForObject("select max(appId) from t_app", Integer.class);
                    jdbcTemplate.update("insert into t_app(appId, appName, status, createTime, appKey, appSecret, sandbox, `version`, notifyUrl, payType)" +
                                    "values (?, ?, ?, now(), ?, ?, ?, ?, ?, ?)",
                            appId,
                            node.get("appName").asText(),
                            node.get("status").asInt(),
                            node.get("appKey").asText(),
                            node.get("appSecret").asText(),
                            node.get("sandbox").asInt(),
                            node.get("version").asText(),
                            node.get("notifyUrl").asText(),
                            node.get("payType").asInt());
                    jdbcTemplate.update("insert into t_app_google (appId, createTime, googleKey) values (?,now(),?)",
                            appId,
                            node.get("googleKey").asText());

                    result.put("code", 1);
                    return mapper.writeValueAsString(result);
                }
            }

            result.put("code", 0);
            return mapper.writeValueAsString(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @RequestMapping(value = "/admin/order_query", method = RequestMethod.POST)
    @ResponseBody
    public String getOrder(@RequestBody String body, HttpSession httpSession) {
        try {
            logger.info("{} {} order", httpSession.getAttribute("username"), body);

            ObjectNode result = mapper.createObjectNode();
            if (hasSession(httpSession)) {
                JsonNode node = mapper.readTree(URLDecoder.decode(body, "UTF-8"));
                if (node != null) {
                    String sql = null;
                    if (node.get("op").asInt() == 1) {
                        // 通过平台id查找
                        sql = String.format("select * from t_pay_info where `order`='%s'", node.get("order").asText());
                    } else if (node.get("op").asInt() == 2) {
                        // 通过平台ID和渠道ID查找
                        sql = String.format("select * from t_pay_info where channelOrder='%s' and channelType=%d",
                                node.get("order").asText(),
                                node.get("channel").asInt());
                    } else if (node.get("op").asInt() == 3) {
                        // 通过日期查找 %Y-%m-%d %H:%i:%s
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                        Date begin = sdf.parse(node.get("begin").asText());
                        Date end = sdf.parse(node.get("end").asText());
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        sql = String.format("select * from t_pay_info where createTime between '%s' and '%s'",
                                sdf.format(begin), sdf.format(end));
                    } else if (node.get("op").asInt() == 4) {
                        // 按照平台帐号id
                        sql = String.format("select * from t_pay_info where userId=%d",
                                node.get("platformId").asLong());
                    } else if (node.get("op").asInt() == 5) {
                        // 按照角色id
                        sql = String.format("select * from t_pay_info where gameRoleId='%s'",
                                node.get("roleId").asText());
                    }

                    result.put("code", 1);
                    ArrayNode array = result.putArray("orders");

                    List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
                    for (Map map: list) {
                        int channelType = (Integer) map.get("channelType");
                        if (channelType == 0) {
                            map.put("channelType", "google");
                        } else if (channelType == 1) {
                            map.put("channelType", "apple");
                        } else if (channelType == 2) {
                            map.put("channelType", "MyCard");
                        }

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        map.put("createTime", sdf.format(map.get("createTime")));

                        JsonNode tmpNode = mapper.readTree(mapper.writeValueAsString(map));
                        array.add(tmpNode);
                    }

                    return mapper.writeValueAsString(result);
                }
            }

            result.put("code", 0);
            return mapper.writeValueAsString(result);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    @RequestMapping(value = "/admin/user_query", method = RequestMethod.POST)
    @ResponseBody
    public String getUser(@RequestBody String body, HttpSession httpSession) {
        try {
            logger.info("{} {} user", httpSession.getAttribute("username"), body);

            ObjectNode result = mapper.createObjectNode();
            if (hasSession(httpSession)) {
                JsonNode node = mapper.readTree(URLDecoder.decode(body, "UTF-8"));
                if (node != null) {
                    String sql = null;
                    if (node.get("op").asInt() == 1) {
                        // 通过平台id查找
                        sql = String.format("select * from t_user_base where userId=%d", node.get("id").asLong());
                    } else if (node.get("op").asInt() == 2) {
                        // 通过平台ID和渠道ID查找
                        sql = String.format("select * from t_user_base where userName='%s'",
                                node.get("name").asText());
                    }

                    result.put("code", 1);
                    ArrayNode array = result.putArray("users");

                    List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
                    for (Map map: list) {
                        int payStatus = (Integer) map.get("status");
                        if (payStatus == 0) {
                            map.put("payStatus", "禁止登录");
                        } else if (payStatus == 1) {
                            map.put("payStatus", "正常");
                        }

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        map.put("createTime", sdf.format(map.get("createTime")));

                        JsonNode tmpNode = mapper.readTree(mapper.writeValueAsString(map));
                        array.add(tmpNode);
                    }

                    return mapper.writeValueAsString(result);
                }
            }

            result.put("code", 0);
            return mapper.writeValueAsString(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @RequestMapping(value = "/admin/gm_order_query", method = RequestMethod.POST)
    @ResponseBody
    public String getGmOrder(@RequestBody String body, HttpServletRequest request) {
        try {
            logger.info("{} {} gmorder", getIpAddress(request), body);

            ObjectNode result = mapper.createObjectNode();
            if (getIpAddress(request).equals("52.76.78.107")) {
                JsonNode node = mapper.readTree(URLDecoder.decode(body, "UTF-8"));
                if (node != null && node.get("user").asText().equals("gm") && node.get("password").asText().equals("pwd")) {
                    String sql = null;
                    if (node.get("op").asInt() == 1) {
                        // 按照平台帐号id
                        sql = String.format("select userId,gameRoleId,productId,amount,currency,channelType,createTime from t_pay_info where userId=%d",
                                node.get("platformId").asLong());
                    } else if (node.get("op").asInt() == 2) {
                        // 按照角色id
                        sql = String.format("select userId,gameRoleId,productId,amount,currency,channelType,createTime from t_pay_info where gameRoleId='%s'",
                                node.get("roleId").asText());
                    }

                    result.put("code", 1);
                    ArrayNode array = result.putArray("orders");

                    List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
                    for (Map map: list) {
                        int channelType = (Integer) map.get("channelType");
                        switch (channelType) {
                            case 0:
                                map.put("channelType", "google");
                                break;
                            case 1:
                                map.put("channelType", "apple");
                                break;

                            case 2:
                                map.put("channelType", "MyCard");
                                break;

                            default:
                                break;
                        }

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        map.put("createTime", sdf.format(map.get("createTime")));

                        JsonNode tmpNode = mapper.readTree(mapper.writeValueAsString(map));
                        array.add(tmpNode);
                    }

                    return mapper.writeValueAsString(result);
                }
            }

            result.put("code", 0);
            return mapper.writeValueAsString(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @RequestMapping(value = "/admin/gm_user_query", method = RequestMethod.POST)
    @ResponseBody
    public String getGmUser(@RequestBody String body, HttpServletRequest request) {
        try {
            logger.info("{} {} gmuser", getIpAddress(request), body);

            ObjectNode result = mapper.createObjectNode();
            if (getIpAddress(request).equals("52.76.78.107")) {
                JsonNode node = mapper.readTree(URLDecoder.decode(body, "UTF-8"));
                if (node != null && node.get("user").asText().equals("gm") && node.get("password").asText().equals("pwd")) {
                    String sql = null;
                    if (node.get("op").asInt() == 1) {
                        // 通过平台id查找
                        sql = String.format("select userId,userName,email from t_user_base where userId=%d", node.get("id").asLong());
                    } else if (node.get("op").asInt() == 2) {
                        // 通过平台ID和渠道ID查找
                        sql = String.format("select userId,userName,email from t_user_base where userName='%s'",
                                node.get("name").asText());
                    }

                    result.put("code", 1);
                    ArrayNode array = result.putArray("users");

                    List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
                    for (Map map: list) {

                        JsonNode tmpNode = mapper.readTree(mapper.writeValueAsString(map));
                        array.add(tmpNode);
                    }

                    return mapper.writeValueAsString(result);
                }
            }

            result.put("code", 0);
            return mapper.writeValueAsString(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取用户真实IP地址，不使用request.getRemoteAddr();的原因是有可能用户使用了代理软件方式避免真实IP地址,
     *
     * 可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值，究竟哪个才是真正的用户端的真实IP呢？
     * 答案是取X-Forwarded-For中第一个非unknown的有效IP字符串。
     *
     * 如：X-Forwarded-For：192.168.1.110, 192.168.1.120, 192.168.1.130,
     * 192.168.1.100
     *
     * 用户真实IP为： 192.168.1.110
     *
     * @param request
     * @return
     */
    public String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private boolean hasSession(HttpSession httpSession) {
        if (httpSession.getAttribute("username") != null &&
                httpSession.getAttribute("password") != null) {
            return true;
        }

        return false;
    }

}
