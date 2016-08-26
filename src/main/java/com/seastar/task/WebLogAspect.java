package com.seastar.task;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * Created by wjl on 2016/8/19.
 * @Aspect定义切面
 * @Pointcut定义切入点
 * @Before在切入点开始处切入内容
 * @After在切入点结尾处切入内容
 * @AfterReturning在切入点return内容之后切入内容（可以用来对处理返回值做一些加工处理）
 * @Around在切入点前后切入内容，并自己控制何时执行切入点自身的内容
 * @AfterThrowing用来处理当切入内容部分抛出异常之后的处理逻辑
 * @Order(i)注解来标识切面的优先级。i的值越小，优先级越高。
 *      在切入点前的操作，按order的值由小到大执行
 *      在切入点后的操作，按order的值由大到小执行
 */
@Aspect
@Component
@Order(5)
public class WebLogAspect {
    // 切入点执行的方法都不是线程安全的，使用线程本地变量来保证安全性
    ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Pointcut("execution(public * com.seastar.web.RestfulController.*(..))")
    public void webLog() {}

    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        // 接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 记录下请求内容
        System.out.println("URL : " + request.getRequestURL().toString());
        System.out.println("HTTP_METHOD : " + request.getMethod());
        System.out.println("IP : " + request.getRemoteAddr());
        System.out.println("CLASS_METHOD : " + joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        System.out.println("ARGS : " + Arrays.toString(joinPoint.getArgs()));

        startTime.set(System.currentTimeMillis());
    }

    @AfterReturning(returning = "ret", pointcut = "webLog()")
    public void doAfterReturning(Object ret) throws Throwable {
        // 处理完请求，返回内容
        System.out.println("RESPONSE : " + ret);

        System.out.println("SPEND TIME : " + (System.currentTimeMillis() - startTime.get()));
    }
}
