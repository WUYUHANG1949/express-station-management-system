package com.wuyuhang.delivery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 面向快递驿站的快件收发管理系统 —— 启动类
 *
 * @author 吴宇航
 */
@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.wuyuhang.delivery.mapper")
public class DeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryApplication.class, args);
        System.out.println("""

                ==========================================================
                  快递驿站快件收发管理系统 后端服务启动成功
                  接口地址 : http://localhost:8080/api
                  接口文档 : http://localhost:8080/swagger-ui.html
                ==========================================================
                """);
    }
}
