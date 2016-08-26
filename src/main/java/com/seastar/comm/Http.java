package com.seastar.comm;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Created by wjl on 2016/8/19.
 * 1×× 　　保留
 * 2×× 　　表示请求成功地接收
 * 3×× 　　为完成请求客户需进一步细化请求
 * 4×× 　　客户错误
 * 5×× 　　服务器错误
 */
public class Http {
    private final String USER_AGENT = "Mozilla/5.0";

    // 无条件接受服务器端证书
    private static class MyX509TrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }
    }

    public String sendGet(String url, Map<String,String> headers) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            URL uri = new URL(url);
            conn = (HttpURLConnection) uri.openConnection();

            // setting
            // optional default is GET
            setConnection(conn, "GET");

            //add request header
            addHeaders(conn, headers);

            int responseCode = conn.getResponseCode();

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            if (responseCode != 200) {
                System.out.println(url + " " + responseCode + " " + response.toString());
                response = new StringBuffer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(conn, null, reader);
        }

        return response.toString();
    }

    public String sendPost(String url, String body, Map<String,String> headers) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        DataOutputStream writer = null;
        StringBuffer response = new StringBuffer();

        try {
            URL uri = new URL(url);
            conn = (HttpURLConnection) uri.openConnection();

            // setting
            setConnection(conn, "POST");

            // add request header
            addHeaders(conn, headers);

            writer = new DataOutputStream(conn.getOutputStream());
            writer.write(body.getBytes("UTF-8"));
            writer.flush();

            int responseCode = conn.getResponseCode();

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            if (responseCode != 200) {
                System.out.println(url + " " + responseCode + " " + response.toString());
                response = new StringBuffer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(conn, writer, reader);
        }

        return response.toString();
    }

    public String sendHttpsGet(String url, Map<String,String> headers) {
        HttpsURLConnection conn = null;
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();

        try {
            // 创建SSLContext对象，并使用我们指定的信任管理器初始化
            TrustManager[] tm = {new MyX509TrustManager()};
            SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
            sslContext.init(null, tm, new java.security.SecureRandom());
            // 从上述SSLContext对象中得到SSLSocketFactory对象
            SSLSocketFactory ssf = sslContext.getSocketFactory();

            URL uri = new URL(url);
            conn = (HttpsURLConnection) uri.openConnection();

            // setting
            // optional default is GET
            conn.setSSLSocketFactory(ssf);
            setConnection(conn, "GET");

            //add request header
            addHeaders(conn, headers);

            int responseCode = conn.getResponseCode();

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            if (responseCode != 200) {
                System.out.println(url + " " + responseCode + " " + response.toString());
                response = new StringBuffer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(conn, null, reader);
        }

        return response.toString();
    }

    public String sendHttpsPost(String url, String body, Map<String,String> headers) {
        HttpsURLConnection conn = null;
        BufferedReader reader = null;
        DataOutputStream writer = null;
        StringBuffer response = new StringBuffer();

        try {
            // 创建SSLContext对象，并使用我们指定的信任管理器初始化
            TrustManager[] tm = {new MyX509TrustManager()};
            SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
            sslContext.init(null, tm, new java.security.SecureRandom());
            // 从上述SSLContext对象中得到SSLSocketFactory对象
            SSLSocketFactory ssf = sslContext.getSocketFactory();

            URL uri = new URL(url);
            conn = (HttpsURLConnection) uri.openConnection();

            // setting
            conn.setSSLSocketFactory(ssf);
            setConnection(conn, "POST");

            // add request header
            addHeaders(conn, headers);

            writer = new DataOutputStream(conn.getOutputStream());
            writer.write(body.getBytes("UTF-8"));
            writer.flush();

            int responseCode = conn.getResponseCode();

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            if (responseCode != 200) {
                System.out.println(url + " " + responseCode + " " + response.toString());
                response = new StringBuffer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(conn, writer, reader);
        }

        return response.toString();
    }

    private void setConnection(HttpURLConnection conn, String method) throws Exception {
        conn.setRequestMethod(method);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
    }

    private void addHeaders(HttpURLConnection conn, Map<String,String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        conn.setRequestProperty("Restful-Agent", USER_AGENT);
    }

    private void close(HttpURLConnection conn, DataOutputStream writer, BufferedReader reader) {
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception e) {

            }
        }

        if (reader != null) {
            try {
                reader.close();
            } catch (Exception e) {

            }
        }

        if (conn != null) {
            try {
                conn.disconnect();
            } catch (Exception e) {

            }
        }
    }
}
