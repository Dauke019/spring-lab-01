package kz.iitu.springlab.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // rules are checked top to bottom, the first match wins
                .requestMatchers("/", "/login", "/css/**", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/books/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/books/**").hasRole("ADMIN")      // create, edit, delete
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/h2-console/**").hasRole("ADMIN") // database client for the demo
                // the second way to the same BookService: any signed-in user may call the API,
                // what they may change is decided by @PreAuthorize on the service (task 3.2)
                .requestMatchers("/api/books/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().denyAll())                             // forgotten URL = closed, not open
            // variant 1: our own login page, with error and logout messages
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            // the H2 console posts its own forms without our token and runs inside frames;
            // it is a development tool and only ADMIN reaches it (see the rule above)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /** Answers "is this the right password?": BCrypt with a random salt per hash and cost factor 10. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
