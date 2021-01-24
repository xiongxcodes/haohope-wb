package com.github.xiongxcodes.wb.configuration.stat;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.wb.common.Base;
import com.wb.common.WebSocket;
import com.wb.util.DbUtil;

@ConditionalOnWebApplication
public class WbCoreConfiguration {
    @Bean
    @Primary
    public TomcatServletWebServerFactory tomcatServletWebServerFactory(DataSourceProperties dataSourceProperties) {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
                tomcat.enableNaming();
                return super.getTomcatWebServer(tomcat);
            }

            @Override
            protected void postProcessContext(Context context) {
                try {
                    File file =
                        new File("D:\\Work\\Eclipse\\Workspaces\\hhwb-parent\\hhwb-core\\src\\main\\resources\\webapp");// org.springframework.util.ResourceUtils.getFile("classpath:webapp");
                    context.setDocBase(file.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.postProcessContext(context);
            }
        };
        return tomcat;
    }

    @Bean
    public DbUtil dbUtil() {
        return new DbUtil();
    }

    @Bean
    public Base base() {
        return new Base();
    }

    @Bean
    @Primary
    public WebSocket webSocket() {
        return new WebSocket();
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Bean
    public FilterRegistrationBean<Base> wbStatFilterRegistrationBean() {
        FilterRegistrationBean<Base> registrationBean = new FilterRegistrationBean<Base>();
        Base base = new Base();
        registrationBean.setFilter(base);
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }

}
