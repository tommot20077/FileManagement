package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import xyz.dowob.filemanagement.convert.EntityJsonSetMapper;

import java.util.Arrays;
import java.util.List;

/**
 * 此類用於配置 R2dbcCustomConversions
 * 設定自定義的轉換器 {@link EntityJsonSetMapper.SetConverter} {@link EntityJsonSetMapper.JsonConverter}
 *
 * @author yuan
 * @program FileManagement
 * @ClassName R2dbcConfig
 * @create 2025/1/21
 * @Version 1.0
 **/
@Configuration
public class R2dbcConfig {
    /**
     * 用於配置 R2dbcCustomConversions，引入自定義的轉換器
     *
     * @return R2dbcCustomConversions 返回一個 R2dbcCustomConversions 對象
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Object> converters = Arrays.asList(new EntityJsonSetMapper.SetConverter(), new EntityJsonSetMapper.JsonConverter());
        return new R2dbcCustomConversions(CustomConversions.StoreConversions.NONE, converters);
    }

}
