package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyuhang.delivery.entity.SmsCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 验证码 Mapper。
 * <p>
 * 查询条件较简单（按手机号 + 账号 + 场景取最新一条），直接用 MyBatis-Plus 的
 * LambdaQueryWrapper 在 Service 层拼装即可，无需自定义 SQL。
 *
 * @author 吴宇航
 */
@Mapper
public interface SmsCodeMapper extends BaseMapper<SmsCode> {
}
