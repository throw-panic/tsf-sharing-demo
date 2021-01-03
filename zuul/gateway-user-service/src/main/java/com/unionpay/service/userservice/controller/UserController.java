package com.unionpay.service.userservice.controller;


import com.unionpay.service.userservice.dto.UserAddDTO;
import com.unionpay.service.userservice.dto.UserDTO;
import org.springframework.web.bind.annotation.*;

/**
 * @author MyPC
 */


/**
 *      服务注册到 注册中心 网关根据 service-id 路由到服务
 *      网关路由到服务，返回路由结果。
 *
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/get")
    public UserDTO get(@RequestParam("id") Integer id) {
        return new UserDTO().setId(id)
                .setName("没有昵称：" + id)
                .setGender(id % 2 + 1); // 1 - 男；2 - 女
    }

    @PostMapping("/add")
    public Integer add(UserAddDTO addDTO) {
        return (int) (System.currentTimeMillis() / 1000); // 随便返回一个 id
    }

    @GetMapping("/sleep")
    public void sleep() throws InterruptedException {
        Thread.sleep(10 * 1000L);
    }

}
