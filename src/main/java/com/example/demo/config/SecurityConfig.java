package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    // 允許的前端來源，從 application.properties 的 app.cors.allowed-origin 讀進來，
    // 本機開發沒設環境變數時預設是 http://localhost:5173；
    // 部署到雲端後用環境變數 CORS_ALLOWED_ORIGIN 蓋掉即可，多個來源用逗號分隔。
    @Value("${app.cors.allowed-origin}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .csrf(csrf -> csrf.disable())
            // 改用 JWT 之後，伺服器不再需要記住任何登入狀態，明確關閉 Session 建立，
            // 也不會再核發 JSESSIONID cookie，身份驗證完全靠請求帶來的 JWT 自己證明。
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // /error 也要放行：Spring Boot 遇到「錯誤狀態碼但沒有內容」的回應時，
                // 會內部轉發到 /error 產生預設錯誤頁，這個轉發本身也會被 Security 檢查一次
                .requestMatchers("/api/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            // 在 Spring Security 內建的帳密登入 Filter 之前，先插入我們自己的 JWT 驗證 Filter，
            // 讓每個請求先被檢查有沒有帶合法的 Authorization: Bearer token。
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
