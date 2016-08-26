package com.seastar.web;

import com.seastar.domain.Restful;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Future;

/**
 * Created by wjl on 2016/8/19.
 * 引入spring-boot-starter-jdbc或spring-boot-starter-data-jpa后自动注册DataSourceTransactionManager或JpaTransactionManager。
 * 此时可以直接用@Transactional注解进行事务的使用.
 *
 * 隔离级别是指若干个并发的事务之间的隔离程度，与我们开发时候主要相关的场景包括：脏读取、重复读、幻读。
 *      DEFAULT：这是默认值，表示使用底层数据库的默认隔离级别。对大部分数据库而言，通常这值就是：READ_COMMITTED。
 *      READ_UNCOMMITTED：该隔离级别表示一个事务可以读取另一个事务修改但还没有提交的数据。该级别不能防止脏读和不可重复读，因此很少使用该隔离级别。
 *      READ_COMMITTED：该隔离级别表示一个事务只能读取另一个事务已经提交的数据。该级别可以防止脏读，这也是大多数情况下的推荐值。
 *      REPEATABLE_READ：该隔离级别表示一个事务在整个过程中可以多次重复执行某个查询，并且每次返回的记录都相同。即使在多次查询之间有新增的数据满足该查询，这些新增的记录也会被忽略。该级别可以防止脏读和不可重复读。
 *      SERIALIZABLE：所有的事务依次逐个执行，这样事务之间就完全不可能产生干扰，也就是说，该级别可以防止脏读、不可重复读以及幻读。但是这将严重影响程序的性能。通常情况下也不会用到该级别。
 *      设置方法@Transactional(isolation = Isolation.DEFAULT)
 *
 * 传播行为是指，如果在开始当前事务之前，一个事务上下文已经存在，此时有若干选项可以指定一个事务性方法的执行行为。即事务嵌套。
 *      REQUIRED：如果当前存在事务，则加入该事务；如果当前没有事务，则创建一个新的事务。
 *      SUPPORTS：如果当前存在事务，则加入该事务；如果当前没有事务，则以非事务的方式继续运行。
 *      MANDATORY：如果当前存在事务，则加入该事务；如果当前没有事务，则抛出异常。
 *      REQUIRES_NEW：创建一个新的事务，如果当前存在事务，则把当前事务挂起。
 *      NOT_SUPPORTED：以非事务方式运行，如果当前存在事务，则把当前事务挂起。
 *      NEVER：以非事务方式运行，如果当前存在事务，则抛出异常。
 *      NESTED：如果当前存在事务，则创建一个事务作为当前事务的嵌套事务来运行；如果当前没有事务，则该取值等价于REQUIRED。
 *      设置方法@Transactional(propagation = Propagation.REQUIRED)
 *
 * 大部分情况下使用默认设置就可以。
 *
 * @EnableCaching启用缓存。
 * @CacheConfig(cacheNames = "users") 配置缓存,指定缓存存放于users对象中。
 * @Cacheable指定函数返回值存储到缓存中，下次从缓存中取值。
 *      value、cacheNames: 都可以用来指定缓存对象名称，同CacheConfig。
 *      key: 缓存的key值，自己设置时需要使用spel表达式。@Cacheable(key = "#p0")：使用函数第一个参数作为缓存的key值。 http://docs.spring.io/spring/docs/current/spring-framework-reference/html/cache.html#cache-spel-context
 *      condition: 缓存对象的条件，非必需，也需使用SpEL表达式，只有满足表达式条件的内容才会被缓存。
 *      cacheManager：用于指定使用哪个缓存管理器，非必需。
 *      cacheResolver：用于指定使用那个缓存解析器，非必需。
 * @CachePut：配置于函数上，能够根据参数定义条件来进行缓存，它与@Cacheable不同的是，它每次都会真是调用函数，所以主要用于数据新增和修改操作上。
 * @CacheEvict：配置于函数上，通常用在删除方法上，用来从缓存中移除相应数据。
 *
 * 依次扫描，确定cacheManager用哪个：
 *      Generic
 *      JCache
 *      EhCache 2.x
 *      Hazelcast
 *      Infinispan
 *      Redis
 *      Guava
 *      Simple
 * 可以通过spring.cache.type强制指定用哪个。
 *
 *
 * 在主类用@EnableAsync启用。
 *
 * 主类添加@EnableScheduling启用。
 * @Scheduled(fixedRate = 5000) ：上一次开始执行时间点之后5秒再执行。
 * @Scheduled(fixedDelay = 5000) ：上一次执行完毕时间点之后5秒再执行。
 * @Scheduled(initialDelay=1000, fixedRate=5000) ：第一次延迟1秒后执行，之后按fixedRate的规则每5秒执行一次。
 * @Scheduled(cron="") ：通过cron表达式定义规则。
 */
@CacheConfig(cacheNames = "users")
//@RestController
//@RequestMapping("/restful")
public class RestfulController {

    public static Random random = new Random();

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    // 线程安全的map
    private static Map<Long, Restful> users = Collections.synchronizedMap(new HashMap<Long, Restful>());

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JavaMailSender mailSender;


    @Transactional
    @RequestMapping("/save")
    public String save() {
        return "ok";
    }

    @RequestMapping("/mail")
    public void sendSimpleMail() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("dyc87112@qq.com");
        message.setTo("dyc87112@qq.com");
        message.setSubject("主题：简单邮件");
        message.setText("测试邮件内容");

        mailSender.send(message);
    }

    // 有附件
    @RequestMapping("/attachment")
    public void sendAttachmentsMail() throws Exception {

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom("dyc87112@qq.com");
        helper.setTo("dyc87112@qq.com");
        helper.setSubject("主题：有附件");
        helper.setText("有附件的邮件");

        FileSystemResource file = new FileSystemResource(new File("weixin.jpg"));
        helper.addAttachment("附件-1.jpg", file);
        helper.addAttachment("附件-2.jpg", file);

        mailSender.send(mimeMessage);
    }

    // 嵌入静态资源
    @RequestMapping("/inline")
    public void sendInlineMail() throws Exception {

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom("dyc87112@qq.com");
        helper.setTo("dyc87112@qq.com");
        helper.setSubject("主题：嵌入静态资源");
        helper.setText("<html><body><img src=\"cid:weixin\" ></body></html>", true);

        FileSystemResource file = new FileSystemResource(new File("weixin.jpg"));
        helper.addInline("weixin", file);

        mailSender.send(mimeMessage);

    }

    @Cacheable
    @ApiOperation(value="获取用户列表", notes="")
    @RequestMapping(value="/", method=RequestMethod.GET)
    public List<Restful> getUserList() {
        // 处理"/users/"的GET请求，用来获取用户列表
        // 还可以通过@RequestParam从页面中传递参数来进行查询条件或者翻页信息的传递
        List<Restful> r = new ArrayList<Restful>(users.values());
        return r;
    }
    @ApiOperation(value="创建用户", notes="根据User对象创建用户")
    @ApiImplicitParam(name = "user", value = "用户详细实体user", required = true, dataType = "Restful")
    @RequestMapping(value="/", method=RequestMethod.POST)
    public String postUser(@ModelAttribute Restful user) {
        // 处理"/users/"的POST请求，用来创建User
        // 除了@ModelAttribute绑定参数之外，还可以通过@RequestParam从页面中传递参数
        users.put(user.id, user);
        return "success";
    }

    @ApiOperation(value="获取用户详细信息", notes="根据url的id来获取用户详细信息")
    @ApiImplicitParam(name = "id", value = "用户ID", required = true, dataType = "Long")
    @RequestMapping(value="/{id}", method=RequestMethod.GET)
    public Restful getUser(@PathVariable Long id) {
        // 处理"/users/{id}"的GET请求，用来获取url中id值的User信息
        // url中的id可通过@PathVariable绑定到函数的参数中
        return users.get(id);
    }

    @ApiOperation(value="更新用户详细信息", notes="根据url的id来指定更新对象，并根据传过来的user信息来更新用户详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "用户ID", required = true, dataType = "Long"),
            @ApiImplicitParam(name = "user", value = "用户详细实体user", required = true, dataType = "Restful")
    })
    @RequestMapping(value="/{id}", method=RequestMethod.PUT)
    public String putUser(@PathVariable Long id, @ModelAttribute Restful user) {
        // 处理"/users/{id}"的PUT请求，用来更新User信息
        Restful u = users.get(id);
        u.name = user.name;
        u.age = user.age;
        users.put(id, u);
        return "success";
    }

    @ApiOperation(value="删除用户", notes="根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "用户ID", required = true, dataType = "Long")
    @RequestMapping(value="/{id}", method=RequestMethod.DELETE)
    public String deleteUser(@PathVariable Long id) {
        // 处理"/users/{id}"的DELETE请求，用来删除User
        users.remove(id);
        return "success";
    }

    @RequestMapping("/async")
    public void testAsync() throws Exception {
        // 方法完全异步执行
        doTaskOne();
        doTaskTwo();

        // 可以获得结果的异步执行
        long start = System.currentTimeMillis();

        Future<String> task1 = doFutureTaskOne();
        Future<String> task2 = doFutureTaskTwo();

        while(true) {
            if(task1.isDone() && task2.isDone()) {
                // 三个任务都调用完成，退出循环等待
                break;
            }
            Thread.sleep(1000);
        }

        long end = System.currentTimeMillis();

        System.out.println("任务全部完成，总耗时：" + (end - start) + "毫秒");
    }

    //@Scheduled(fixedRate = 5000)
    public void reportCurrentTime() {
        System.out.println("现在时间：" + dateFormat.format(new Date()));
    }

    @Async
    public void doTaskOne() throws Exception {
        System.out.println("开始做任务一");
        long start = System.currentTimeMillis();
        Thread.sleep(random.nextInt(10000));
        long end = System.currentTimeMillis();
        System.out.println("完成任务一，耗时：" + (end - start) + "毫秒");
    }

    @Async
    public void doTaskTwo() throws Exception {
        System.out.println("开始做任务二");
        long start = System.currentTimeMillis();
        Thread.sleep(random.nextInt(10000));
        long end = System.currentTimeMillis();
        System.out.println("完成任务二，耗时：" + (end - start) + "毫秒");
    }

    // 有结果的异步任务
    @Async
    public Future<String> doFutureTaskOne() throws Exception {
        System.out.println("开始做任务一");
        long start = System.currentTimeMillis();
        Thread.sleep(random.nextInt(10000));
        long end = System.currentTimeMillis();
        System.out.println("完成任务一，耗时：" + (end - start) + "毫秒");
        return new AsyncResult<>("任务一完成");
    }

    @Async
    public Future<String> doFutureTaskTwo() throws Exception {
        System.out.println("开始做任务二");
        long start = System.currentTimeMillis();
        Thread.sleep(random.nextInt(10000));
        long end = System.currentTimeMillis();
        System.out.println("完成任务二，耗时：" + (end - start) + "毫秒");
        return new AsyncResult<>("任务二完成");
    }
}
