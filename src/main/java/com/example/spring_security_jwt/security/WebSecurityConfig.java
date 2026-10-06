package com.example.spring_security_jwt.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.spring_security_jwt.security.jwt.AuthEntryPointJwt;
import com.example.spring_security_jwt.security.jwt.AuthTokenFilter;
import com.example.spring_security_jwt.security.jwt.JwtUtils;
import com.example.spring_security_jwt.security.service.UserDetailsServiceImpl;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    private final AuthEntryPointJwt unauthorizedHandler;

    private final JwtUtils jwtUtils;

    // Our JWT filter, created by hand so we can pass it its two dependencies
    @Bean
    AuthTokenFilter authenticationJwtTokenFilter() {

        return new AuthTokenFilter(jwtUtils, userDetailsService);
    }

    // Checks a login: loads the user and compares the password with the stored hash
    @Bean
    DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);

        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    // Algorithm used to hash passwords (signup) and to compare them (login)
    @Bean
    PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    // The "door" that AuthController will call to log a user in
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) {

        return authConfig.getAuthenticationManager();
    }

    // The security rules of the application
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) {

        http.csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/error").permitAll()
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(authenticationJwtTokenFilter(),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

/*
 * Great, with that the three building blocks are in place (user lookup, token
 * handling, filter). Step 6 is the class that wires them together and turns
 * security on.
 * 
 * The big picture
 * 
 * WebSecurityConfig is the control panel of Spring Security. It doesn't contain
 * logic of its own: it creates and connects the pieces you've built so far and
 * declares the rules. The teacher's comment in the example says it well:
 * filterChain is the part you adapt to each project, and the rest is
 * boilerplate.
 * 
 * Piece Built in Role
 * UserDetailsServiceImpl Step 3 Finds the user by email
 * JwtUtils Step 4 Creates and validates tokens
 * AuthTokenFilter Step 5 Reads the token on each request
 * AuthEntryPointJwt Step 5 Answers 401
 * WebSecurityConfig Now Assembles all of them
 * 
 * Create the file in src/main/java/com/example/spring_security_jwt/security/,
 * the folder that contains jwt and service, not inside them.
 * 
 * 
 * Explanation
 * 
 * Imports. All the org.springframework.security.* ones come from
 * spring-boot-starter-security (step 1), which bundles several Spring Security
 * modules: config (the HttpSecurity builder and annotations), web (filters and
 * SecurityFilterChain), crypto (BCrypt) and core. Bean, Configuration... come
 * from Spring Framework itself.
 * 
 * The class annotations
 * 
 * @Configuration. It marks a class whose job is to declare beans. At startup,
 * Spring runs each @Bean method once and stores the returned object so it can
 * be injected anywhere. Spring also wraps this class in a proxy, so when
 * one @Bean method calls another (like passwordEncoder() inside
 * authenticationProvider()), it gets the same single instance, not a new one
 * every time.
 * 
 * @EnableMethodSecurity. It switches on security annotations on methods, which
 * in step 8 means @PreAuthorize on your controllers. Without it, those
 * annotations would be silently ignored and every endpoint would be open to any
 * authenticated user. A correction to the comment in the example: by default
 * this annotation enables only the @PreAuthorize/@PostAuthorize
 * family. @Secured and @RolesAllowed need extra flags (securedEnabled,
 * jsr250Enabled), which we aren't using.
 * 
 * @RequiredArgsConstructor and the three final fields. As before, Lombok builds
 * the constructor and Spring injects the three dependencies:
 * UserDetailsServiceImpl, AuthEntryPointJwt (which is a @Component) and
 * JwtUtils. I renamed the second field to unauthorizedHandler (the example has
 * a typo).
 * 
 * The beans, one by one
 * 
 * authenticationJwtTokenFilter(). Remember AuthTokenFilter has no @Component,
 * so Spring doesn't know it. Here we build it with new and hand it the two
 * collaborators it needs. Because it's a @Bean, it's still a Spring-managed
 * object.
 * 
 * passwordEncoder() with BCryptPasswordEncoder. A PasswordEncoder is Spring
 * Security's interface for password hashing. BCrypt is the standard,
 * recommended algorithm. Key ideas:
 * 
 * It's hashing, not encryption: one-way. There's no way to get the password
 * back from the hash.
 * A hash looks like $2a$10$N9qo8uLOickgx2ZMRZoMye... and is about 60 characters
 * long (the reason User.password has room for 120). $2a$ is the BCrypt version,
 * 10 is the cost (it runs 2¹⁰ rounds on purpose, to make brute-force attacks
 * slow), and the rest is a random salt plus the hash itself.
 * Because of the random salt, hashing the same password twice gives different
 * results. So you can never compare with equals. You use
 * passwordEncoder.matches(rawPassword, storedHash), which reads the salt out of
 * the stored hash and re-hashes.
 * It will be used in two places: in the signup (encode before saving, step 7)
 * and in every login (matches, done for you by the provider below).
 * I renamed the method from PasswordEncoder() (as in the example) to
 * passwordEncoder(). Methods start with lowercase by convention, and the method
 * name becomes the bean name.
 * 
 * authenticationProvider() with DaoAuthenticationProvider. An
 * AuthenticationProvider is a component that knows how to verify one kind of
 * login. The "Dao" one verifies against a data source: it takes the identifier,
 * calls your UserDetailsService.loadUserByUsername(email), and then checks the
 * password using the PasswordEncoder. You give it both through the constructor
 * and setPasswordEncoder. If the email doesn't exist or the password doesn't
 * match, it throws BadCredentialsException.
 * 
 * authenticationManager(...). The AuthenticationManager is the entry point to
 * log someone in: you hand it a UsernamePasswordAuthenticationToken(email,
 * password) and it delegates to the providers (ours) and returns an
 * authenticated user or throws. Spring builds one automatically from
 * AuthenticationConfiguration, but it only exposes it as a bean if you ask for
 * it like this. Step 7's AuthController will inject it.
 * 
 * filterChain: the important part
 * 
 * SecurityFilterChain is the declaration of the chain of filters every request
 * crosses. HttpSecurity is the builder you configure; the -> expressions are
 * lambdas, one configuration block per aspect.
 * 
 * csrf(csrf -> csrf.disable()). CSRF is an attack that abuses the fact that
 * browsers automatically attach cookies to requests. Our API doesn't use
 * cookies: the client explicitly sends the token in a header, which a malicious
 * site can't do. So this protection adds nothing and, left on, it would reject
 * your POST requests.
 * 
 * exceptionHandling(... authenticationEntryPoint(unauthorizedHandler)).
 * Registers AuthEntryPointJwt from step 5, so unauthenticated requests to
 * protected routes get our 401 and not Spring's default.
 * 
 * sessionManagement(... STATELESS). Tells Spring Security never to create or
 * use an HTTP session. Every request has to prove who it is with its token.
 * This is exactly what lets JWT replace sessions.
 * 
 * authorizeHttpRequests(...): who can reach what.
 * 
 * .requestMatchers("/api/auth/**", "/error").permitAll() lists the public
 * routes. /api/auth/** means "/api/auth/ and anything below it" (signup and
 * signin, step 7). Without it, nobody could log in, because they'd need a token
 * to get a token.
 * .anyRequest().authenticated() means
 * "everything else requires a valid logged-in user".
 * The order matters: rules are checked top to bottom and the first match wins,
 * and anyRequest() must come last (Spring throws an error at startup if you add
 * rules after it).
 * Why /error? When a request fails, Spring Boot internally forwards to /error
 * to build the error response. Our filter doesn't run on those forwards, so the
 * SecurityContext would be empty there. With /error protected, every real error
 * (400, 404, 403...) would be disguised as a 401, making debugging very
 * confusing. This is the fix from the [IA] comment in the example.
 * Note that this chain only separates public from authenticated. The
 * distinction between USER and ADMIN is made later, per method,
 * with @PreAuthorize (step 8).
 * 
 * http.authenticationProvider(authenticationProvider()). Registers our provider
 * explicitly in this chain. Spring would probably find the bean on its own, but
 * being explicit leaves no doubt about which provider is used.
 * 
 * http.addFilterBefore(ourFilter, UsernamePasswordAuthenticationFilter.class).
 * Inserts our filter in the chain. UsernamePasswordAuthenticationFilter is
 * Spring's filter for form logins, and we don't use it: it only serves as a
 * position marker. Placing our filter before it makes sure the SecurityContext
 * is already filled when the later filter checks the rules. In simplified form,
 * the journey of a request is:
 * 
 * Request
 * → AuthTokenFilter (reads the token, fills the SecurityContext)
 * → ... other Spring filters ...
 * → authorization check (public / authenticated rules from above)
 * → DispatcherServlet
 * → @PreAuthorize (role check, step 8)
 * → your controller
 * 
 * return http.build(). Finishes the configuration and produces the chain Spring
 * will actually use.
 * 
 * Differences with the example
 * passwordEncoder() and unauthorizedHandler renamed to conventional names.
 * The authorizeHttpRequests block is written in one readable chain.
 * I removed the long comment blocks (their content is explained above).
 * Nothing else changes. All your adaptation to the email login lives in the
 * previous steps and in step 7.
 * Checking
 * Run ./mvnw compile and wait for BUILD SUCCESS.
 * Start the application, then make a request without any token to a protected
 * route, for example GET http://localhost:8080/api/tutorials (adjust the port
 * if you changed it) from Postman or the browser. You should get a 401, and in
 * the console a line starting with Unauthorized error: from AuthEntryPointJwt.
 * That proves the chain is active and our entry point is in charge.
 * 
 * You can't test a login yet: /api/auth/** is now public, but no controller
 * exists there (you'd get a 404 until step 7), and there are no roles or users
 * in the database until step 8.
 * 
 * Heads-up for later: when we reach step 8, remember your
 * catch-all @ExceptionHandler(Exception.class). It will turn the 403
 * from @PreAuthorize into a 500 unless we handle it.
 * 
 * When you have this compiling and the 401 working, let me know and we move to
 * step 7: the DTOs (with the validations your exercise requires, including
 * login by email) and the AuthController with signin and signup.
 */