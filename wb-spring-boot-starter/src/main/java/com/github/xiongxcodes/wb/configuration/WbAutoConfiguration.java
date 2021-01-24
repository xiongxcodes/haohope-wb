package com.github.xiongxcodes.wb.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.github.xiongxcodes.wb.configuration.stat.WbCoreConfiguration;
import com.github.xiongxcodes.wb.configuration.stat.WbRedisConfiguration;

@Configuration
@Import({WbCoreConfiguration.class, WbRedisConfiguration.class})
public class WbAutoConfiguration {

}
