package com.example.demo.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// ===== Spring Boot 3.x (Java 17+) သုံးရင် ဒီ ၂ ကြောင်း =====
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;

// ===== Spring Boot 2.x (Java 8/11) သုံးရင် အပေါ်က ၂ ကြောင်းကို ဖျက်ပြီး ဒီ ၂ ကြောင်း သုံးပါ =====
// import javax.servlet.*;
// import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

@Configuration
public class HeaderConfig {

    @Bean
    public FilterRegistrationBean<Filter> crossOriginIsolationFilter() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletResponse res = (HttpServletResponse) response;
                // Unity WebGL အတွက် မဖြစ်မနေ လိုတဲ့ Header ၂ ခု
                res.setHeader("Cross-Origin-Opener-Policy", "same-origin");
                res.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
                chain.doFilter(request, response);
            }
        });

        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}