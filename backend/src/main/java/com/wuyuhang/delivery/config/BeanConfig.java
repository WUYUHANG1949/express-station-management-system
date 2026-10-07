package com.wuyuhang.delivery.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 通用 Bean 配置。
 *
 * @author 吴宇航
 */
@Configuration
public class BeanConfig {

    /**
     * 密码加密器。
     * <p>
     * 采用 BCrypt 算法：同一个明文每次加密结果都不同（内置随机盐），
     * 校验时使用 {@code matches(明文, 密文)}，比 MD5 加固定盐更安全，
     * 即使数据库泄露也无法通过彩虹表反查明文。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 强度 10 是 Spring Security 的默认值，与初始化脚本中的密文一致
        return new BCryptPasswordEncoder(10);
    }

    /**
     * 在线接口文档信息，答辩演示时可打开 /swagger-ui.html 直接调接口。
     */
    @Bean
    public OpenAPI expressStationOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("面向快递驿站的快件收发管理系统 接口文档")
                .description("""
                        毕业设计项目后端接口文档。
                        使用方式：先调用 /api/auth/login 获取 token，
                        点击右上角 Authorize 按钮填入 token 后即可调试其余接口。
                        演示账号：admin / staff01 / user01，密码均为 123456。
                        """)
                .version("v1.0.0")
                .contact(new Contact().name("吴宇航")));
    }
}
