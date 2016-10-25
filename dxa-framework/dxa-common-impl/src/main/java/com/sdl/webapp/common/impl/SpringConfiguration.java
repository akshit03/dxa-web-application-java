package com.sdl.webapp.common.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.webapp.common.impl.interceptor.StaticContentInterceptor;
import com.sdl.webapp.common.impl.interceptor.ThreadLocalInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

@Configuration
@ComponentScan("com.sdl.webapp.common.impl")
public class SpringConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(staticContentInterceptor());
        registry.addInterceptor(threadLocalInterceptor());
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(mappingJackson2HttpMessageConverter());
        addDefaultHttpMessageConverters(converters);
    }

    @Bean
    public HandlerInterceptor staticContentInterceptor() {
        return new StaticContentInterceptor();
    }

    @Bean
    public HandlerInterceptor threadLocalInterceptor() {
        return new ThreadLocalInterceptor();
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setObjectMapper(objectMapper);
        return jsonConverter;
    }
}
