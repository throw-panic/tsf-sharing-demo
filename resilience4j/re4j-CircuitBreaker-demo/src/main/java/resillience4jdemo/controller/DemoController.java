package resillience4jdemo.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author MyPC
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RestTemplate restTemplate;

    /**
     * todo:
     *  使用 RestTemplate 调用用户服务提供的 /user/get 接口，获取用户详情。
     *  在 #getUser(Integer id) 方法上，添加了 Resilience4j 提供的 @CircuitBreaker 注解：
     *      (1) 通过 name 属性，设置对应的 CircuitBreaker 熔断器实例名为 "backendA"，
     *          就是我们在配置文件中所添加的。
     *      (2) 通过 fallbackMethod 属性，设置执行发生 Exception 异常时，执行对应的
     *          #getUserFallback(Integer id, Throwable throwable) 方法。
     */
    @GetMapping("/get_user")
    @CircuitBreaker(name = "backendA", fallbackMethod = "getUserFallback")
    public String getUser(@RequestParam("id") Integer id) {
        logger.info("[getUser][准备调用 user-service 获取用户({})详情]", id);
        return restTemplate.getForEntity("http://127.0.0.1:18080/user/get?id="
                + id, String.class).getBody();
    }

    public String getUserFallback(Integer id, Throwable throwable) {
        logger.info("[getUserFallback][id({}) exception({})]", id, throwable.getClass().getSimpleName());
        return "mock:User:" + id;
    }

}
