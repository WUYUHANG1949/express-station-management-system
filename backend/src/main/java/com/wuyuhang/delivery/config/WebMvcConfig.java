package com.wuyuhang.delivery.config;

import com.wuyuhang.delivery.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册登录拦截器 + 跨域配置。
 *
 * @author 吴宇航
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /**
     * 注册 JWT 登录拦截器。
     * <p>
     * 拦截 /api 下的全部接口，只放行登录、注册以及接口文档相关的地址。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        // 收件人自助查询取件码，免登录
                        "/api/public/**",
                        "/error",
                        "/doc.html",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                );
    }

    /**
     * 跨域配置。
     * <p>
     * 前端开发服务器运行在 5173 端口，与后端 8080 端口不同源，因此需要放行跨域。
     * 生产部署时通过 Nginx 反向代理成同源，可去掉这里的配置。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 让前端能读到文件名，用于导出下载
                .exposedHeaders("Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
