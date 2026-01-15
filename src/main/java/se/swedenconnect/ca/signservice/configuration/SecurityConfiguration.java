/*
 * Copyright (c) 2026.  Agency for Digital Government (DIGG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.swedenconnect.ca.signservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration class for setting up application security.
 * <p>
 * This class defines the security filter chain and a custom user details service
 * for managing authentication and authorization in the application.
 * <p>
 * The configuration allows all requests to be permitted and disables CSRF protection,
 * which is typically suitable for non-browser-based APIs. Additionally, it provides
 * a no-op user details service, ensuring no users are present, and any authentication
 * attempt will result in failure.
 */
@Configuration
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(csrf -> csrf.disable())   // usually needed for non-browser APIs; optional
        .build();
  }

  @Bean
  UserDetailsService userDetailsService() {
    // No users. If anything tries to authenticate, it will fail.
    return username -> { throw new UsernameNotFoundException(username); };
  }
}
