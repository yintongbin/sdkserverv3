package com.seastar;

import com.seastar.comm.Http;
import com.seastar.domain.PayInfo;
import com.seastar.task.push.PushTask;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncCharConsumer;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.protocol.HttpContext;
import org.junit.*;
import org.junit.runner.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
//@WebMvcTest(RestfulController.class)
public class AppApplicationTests {

	//@Autowired
	private MockMvc mvc;

	@Test
	public void contextLoads() {
	}

	//@Test
	public void testUserController() throws Exception {
		// 测试UserController
		RequestBuilder request = null;

		// 1、get查一下user列表，应该为空
		request = get("/users/");
		mvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(content().string("[]"));

		// 2、post提交一个user
		request = post("/users/")
				.param("id", "1")
				.param("name", "测试大师")
				.param("age", "20");
		mvc.perform(request)
				.andExpect(content().string("success"));

		// 3、get获取user列表，应该有刚才插入的数据
		request = get("/users/");
		mvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(content().string("[{\"id\":1,\"name\":\"测试大师\",\"age\":20}]"));

		// 4、put修改id为1的user
		request = put("/users/1")
				.param("name", "测试终极大师")
				.param("age", "30");
		mvc.perform(request)
				.andExpect(content().string("success"));

		// 5、get一个id为1的user
		request = get("/users/1");
		mvc.perform(request)
				.andExpect(content().string("{\"id\":1,\"name\":\"测试终极大师\",\"age\":30}"));

		// 6、del删除id为1的user
		request = delete("/users/1");
		mvc.perform(request)
				.andExpect(content().string("success"));

		// 7、get查一下user列表，应该为空
		request = get("/users/");
		mvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(content().string("[]"));

	}

	//@Test
	public void testHttp() throws Exception {
		Http http = new Http();

		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "*/*");
		headers.put("Content-Type", "text/html");
		headers.put("Content-Length", "0");
		System.out.println(http.sendGet("http://www.baidu.com", headers));

		headers = new HashMap<>();
		headers.put("Accept", "*/*");
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		headers.put("Content-Length", "" + "abc".getBytes("UTF-8").length);
		System.out.println(http.sendHttpsPost("https://test.b2b.mycard520.com.tw/MyBillingPay/api/AuthGlobal", "abc", headers));
	}

}
