package com.unionpay.sleuthFeign.springmvcdemo.controller;

import com.unionpay.sleuthFeign.springmvcdemo.feign.UserServiceFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author rrmai
 */
@RestController
@RequestMapping("/feign")
public class FeignController {

    @Autowired
    private UserServiceFeignClient userServiceFeignClient;

    @GetMapping("/get")
    public String get(@RequestParam("id") Integer id) {
        return userServiceFeignClient.get(id);
    }

}
