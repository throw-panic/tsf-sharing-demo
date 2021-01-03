package resillience4jdemo.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author rrmai
 */
@RestController
@RequestMapping("/rate-limiter-demo")
public class RateLimiterDemoController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @GetMapping("/get_user")
    @RateLimiter(name = "backendB", fallbackMethod = "getUserFallback")
    public String getUser(@RequestParam("id") Integer id) {
        return "User:" + id;
    }

    public String getUserFallback(Integer id, Throwable throwable) {
        logger.info("[getUserFallback][id({}) exception({})]", id, throwable.getClass().getSimpleName());
        return "mock:User:" + id;
    }
}
