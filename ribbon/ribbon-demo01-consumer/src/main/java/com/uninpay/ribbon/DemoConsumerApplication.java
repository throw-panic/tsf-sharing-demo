package com.uninpay.ribbon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author rrmai
 */
@SpringBootApplication
public class DemoConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoConsumerApplication.class, args);
    }

    @Configuration
    public class RestTemplateConfiguration {

        @Bean
        @LoadBalanced   // todo: 负载均衡
        public RestTemplate restTemplate() {
            /**
             * RestTemplate 是 Spring 提供的用于访问 Rest 服务的客户端，
             * RestTemplate 提供了多种便捷访问远程 HTTP 服务的方法，
             * 能够大大提高客户端的编写效率。
             */
            return new RestTemplate();
        }
    }

    @RestController
    static class TestController {

        @Autowired
        private RestTemplate restTemplate;

        /**
         * Spring Cloud Netflix Ribbon 提供了 RibbonLoadBalancerClient 实现;
         * loadBalancerClient 属性，LoadBalancerClient 对象，负载均衡客户端。
         * 从注册中心获取的服务 demo-provider 的实例列表中，选择一个进行 HTTP 调用。
         */
        @Autowired
        private LoadBalancerClient loadBalancerClient;


        @GetMapping("/hello02")
        public String hello02(String name) {
            // 直接使用 RestTemplate 调用服务 `demo-provider`
            String targetUrl = "http://demo-provider/echo?name=" + name;
            String response = restTemplate.getForObject(targetUrl, String.class);
            // 返回结果
            return "consumer:" + response;
        }

    }

}
