package com.wendao.config;

import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.annotation.Resource;

@Component
public class Config implements WebMvcConfigurer {

    @Resource
    PayProConfig payProConfig;

    /**
     * @description 一些全局变量，放在这里
     * @return void
    */
    @Resource
    private void configureThymeleafStaticVars(ThymeleafViewResolver viewResolver) {
        if(viewResolver != null) {
            viewResolver.addStaticVariable("title",payProConfig.getTitle());
            viewResolver.addStaticVariable("indexTitle",payProConfig.getIndexTitle());
            viewResolver.addStaticVariable("alipayUserId",payProConfig.getAlipayUserId());
            viewResolver.addStaticVariable("alipayCustomQrUrl",payProConfig.getAlipayCustomQrUrl());
            viewResolver.addStaticVariable("mobile",payProConfig.getMobile());
            viewResolver.addStaticVariable("name",payProConfig.getName());
            viewResolver.addStaticVariable("supportMail",payProConfig.getSupportMail());
            viewResolver.addStaticVariable("site",payProConfig.getSite());
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600)
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        // 添加外部静态资源映射，用于访问上传的自定义二维码
        registry.addResourceHandler("/static/qrcodes/**")
                .addResourceLocations("file:/app/static/qrcodes/");

        // 添加固定金额二维码的静态资源映射
        registry.addResourceHandler("/static/qr/**")
                .addResourceLocations("file:/app/static/qr/");

        registry.addResourceHandler("/assets/qr/**")
                .addResourceLocations("file:/app/static/qr/");
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public Snowflake snowflake(){
        return new Snowflake(0,0);
    }

}
