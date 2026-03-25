package com.lumenglover.yuemupicturebackend;

import org.apache.ibatis.annotations.Mapper;
import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.ComponentScan;

@EnableAsync
@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableScheduling
@ServletComponentScan
public class YuemuPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuemuPictureBackendApplication.class, args);
    }

}
