package org.iotp.server.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.iotp.server.exception.IoTPErrorResponseHandler;
import org.iotp.server.service.security.auth.jwt.JwtAuthenticationProvider;
import org.iotp.server.service.security.auth.jwt.JwtTokenAuthenticationProcessingFilter;
import org.iotp.server.service.security.auth.jwt.RefreshTokenAuthenticationProvider;
import org.iotp.server.service.security.auth.jwt.RefreshTokenProcessingFilter;
import org.iotp.server.service.security.auth.jwt.SkipPathRequestMatcher;
import org.iotp.server.service.security.auth.jwt.extractor.TokenExtractor;
import org.iotp.server.service.security.auth.rest.RestAuthenticationProvider;
import org.iotp.server.service.security.auth.rest.RestLoginProcessingFilter;
import org.iotp.server.service.security.auth.rest.RestPublicLoginProcessingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class IoTPSecurityConfiguration extends WebSecurityConfigurerAdapter {

  public static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
  public static final String JWT_TOKEN_QUERY_PARAM = "token";

  public static final String WEBJARS_ENTRY_POINT = "/webjars/**";
  public static final String DEVICE_API_ENTRY_POINT = "/api/v1/**";
  public static final String FORM_BASED_LOGIN_ENTRY_POINT = "/api/auth/login";
  public static final String PUBLIC_LOGIN_ENTRY_POINT = "/api/auth/login/public";
  public static final String TOKEN_REFRESH_ENTRY_POINT = "/api/auth/token";
  public static final String[] NON_TOKEN_BASED_AUTH_ENTRY_POINTS = new String[] { "/index.html", "/static/**",
      "/api/noauth/**", "/webjars/**" };
  public static final String TOKEN_BASED_AUTH_ENTRY_POINT = "/api/**";
  public static final String WS_TOKEN_BASED_AUTH_ENTRY_POINT = "/api/ws/**";

  @Autowired
  private IoTPErrorResponseHandler restAccessDeniedHandler;
  @Autowired
  private AuthenticationSuccessHandler successHandler;
  @Autowired
  private AuthenticationFailureHandler failureHandler;
  @Autowired
  private RestAuthenticationProvider restAuthenticationProvider;
  @Autowired
  private JwtAuthenticationProvider jwtAuthenticationProvider;
  @Autowired
  private RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider;

  @Autowired
  @Qualifier("jwtHeaderTokenExtractor")
  private TokenExtractor jwtHeaderTokenExtractor;

  @Autowired
  @Qualifier("jwtQueryTokenExtractor")
  private TokenExtractor jwtQueryTokenExtractor;

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private ObjectMapper objectMapper;

  @Bean
  protected RestLoginProcessingFilter buildRestLoginProcessingFilter() throws Exception {
    RestLoginProcessingFilter filter = new RestLoginProcessingFilter(FORM_BASED_LOGIN_ENTRY_POINT, successHandler,
        failureHandler, objectMapper);
    filter.setAuthenticationManager(this.authenticationManager);
    return filter;
  }

  @Bean
  protected RestPublicLoginProcessingFilter buildRestPublicLoginProcessingFilter() throws Exception {
    RestPublicLoginProcessingFilter filter = new RestPublicLoginProcessingFilter(PUBLIC_LOGIN_ENTRY_POINT,
        successHandler, failureHandler, objectMapper);
    filter.setAuthenticationManager(this.authenticationManager);
    return filter;
  }

  @Bean
  protected JwtTokenAuthenticationProcessingFilter buildJwtTokenAuthenticationProcessingFilter() throws Exception {
    List<String> pathsToSkip = new ArrayList(Arrays.asList(NON_TOKEN_BASED_AUTH_ENTRY_POINTS));
    pathsToSkip.addAll(Arrays.asList(WS_TOKEN_BASED_AUTH_ENTRY_POINT, TOKEN_REFRESH_ENTRY_POINT,
        FORM_BASED_LOGIN_ENTRY_POINT, PUBLIC_LOGIN_ENTRY_POINT, DEVICE_API_ENTRY_POINT, WEBJARS_ENTRY_POINT));
    SkipPathRequestMatcher matcher = new SkipPathRequestMatcher(pathsToSkip, TOKEN_BASED_AUTH_ENTRY_POINT);
    JwtTokenAuthenticationProcessingFilter filter = new JwtTokenAuthenticationProcessingFilter(failureHandler,
        jwtHeaderTokenExtractor, matcher);
    filter.setAuthenticationManager(this.authenticationManager);
    return filter;
  }

  @Bean
  protected RefreshTokenProcessingFilter buildRefreshTokenProcessingFilter() throws Exception {
    RefreshTokenProcessingFilter filter = new RefreshTokenProcessingFilter(TOKEN_REFRESH_ENTRY_POINT, successHandler,
        failureHandler, objectMapper);
    filter.setAuthenticationManager(this.authenticationManager);
    return filter;
  }

  @Bean
  protected JwtTokenAuthenticationProcessingFilter buildWsJwtTokenAuthenticationProcessingFilter() throws Exception {
    AntPathRequestMatcher matcher = new AntPathRequestMatcher(WS_TOKEN_BASED_AUTH_ENTRY_POINT);
    JwtTokenAuthenticationProcessingFilter filter = new JwtTokenAuthenticationProcessingFilter(failureHandler,
        jwtQueryTokenExtractor, matcher);
    filter.setAuthenticationManager(this.authenticationManager);
    return filter;
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) {
    auth.authenticationProvider(restAuthenticationProvider);
    auth.authenticationProvider(jwtAuthenticationProvider);
    auth.authenticationProvider(refreshTokenAuthenticationProvider);
  }

  @Bean
  protected BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.headers().cacheControl().disable().frameOptions().disable().and().cors().and().csrf().disable()
        .exceptionHandling().and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
        .authorizeRequests().antMatchers(WEBJARS_ENTRY_POINT).permitAll() // Webjars
        .antMatchers(DEVICE_API_ENTRY_POINT).permitAll() // Device HTTP
                                                         // Transport API
        .antMatchers(FORM_BASED_LOGIN_ENTRY_POINT).permitAll() // Login
                                                               // end-point
        .antMatchers(PUBLIC_LOGIN_ENTRY_POINT).permitAll() // Public login
                                                           // end-point
        .antMatchers(TOKEN_REFRESH_ENTRY_POINT).permitAll() // Token refresh
                                                            // end-point
        .antMatchers(NON_TOKEN_BASED_AUTH_ENTRY_POINTS).permitAll() // static
                                                                    // resources,
                                                                    // user
                                                                    // activation
                                                                    // and
                                                                    // password
                                                                    // reset
                                                                    // end-points
        .and().authorizeRequests().antMatchers(WS_TOKEN_BASED_AUTH_ENTRY_POINT).authenticated() // Protected
                                                                                                // WebSocket
                                                                                                // API
                                                                                                // End-points
        .antMatchers(TOKEN_BASED_AUTH_ENTRY_POINT).authenticated() // Protected
                                                                   // API
                                                                   // End-points
        .and().exceptionHandling().accessDeniedHandler(restAccessDeniedHandler).and()
        .addFilterBefore(buildRestLoginProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(buildRestPublicLoginProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(buildJwtTokenAuthenticationProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(buildRefreshTokenProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(buildWsJwtTokenAuthenticationProcessingFilter(), UsernamePasswordAuthenticationFilter.class);
  }

  @Bean
  @ConditionalOnMissingBean(CorsFilter.class)
  public CorsFilter corsFilter(@Autowired MvcCorsProperties mvcCorsProperties) {
    if (mvcCorsProperties.getMappings().size() == 0) {
      return new CorsFilter(new UrlBasedCorsConfigurationSource());
    } else {
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.setCorsConfigurations(mvcCorsProperties.getMappings());
      return new CorsFilter(source);
    }
  }
}
