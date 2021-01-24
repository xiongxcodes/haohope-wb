package com.github.xiongxcodes.wb.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.github.xiongxcodes.wb.configuration.constant.WbConstant;

import lombok.Data;

@ConfigurationProperties(WbConstant.WB_PREFIX)
@Data
public class WbStatProperties {
    private String away = "memory";
    private String webpath = "";
}
