package com.wuyuhang.delivery.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 * <p>
 * 1. 分页插件：让 {@code Page<T>} 查询自动拼接 LIMIT；
 * 2. 防全表更新删除插件：拦截没有 WHERE 条件的 update/delete，避免误操作。
 *
 * @author 吴宇航
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件：指定数据库类型为 MySQL
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大条数限制，防止前端传 pageSize=999999 拖垮数据库
        pagination.setMaxLimit(500L);
        // 请求页码大于总页数时，返回首页而不是空数据
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);

        // 防止全表更新与删除
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }
}
