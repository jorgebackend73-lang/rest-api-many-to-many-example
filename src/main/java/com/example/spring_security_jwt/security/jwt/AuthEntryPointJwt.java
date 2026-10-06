package com.example.spring_security_jwt.security.jwt;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        LOGGER.error("Unauthorized error: {}", authException.getMessage());

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error: Unauthorized");
    }
}

/*
 * Explanation
 * 
 * What an AuthenticationEntryPoint is. It's the interface Spring Security calls
 * when someone who is not authenticated tries to access a protected resource.
 * The name comes from web applications with a login page, where the entry point
 * redirects you to the login form. A REST API has no login page, so ours
 * answers with an HTTP status instead.
 * 
 * @Component. WebSecurityConfig will receive this class by injection and
 * register it in the security chain (step 6), so Spring must create it as a
 * bean.
 * 
 * Imports. AuthenticationException must be
 * org.springframework.security.core.AuthenticationException. VS Code may also
 * offer javax.naming.AuthenticationException, which is a different class.
 * 
 * commence(...). The method name is imposed by the interface. Spring calls it
 * with the request, the response, and the exception that explains why access
 * was denied. It does two things:
 * 
 * Logs the reason, so you can diagnose it on the console.
 * response.sendError(401, ...) answers with status 401. This also hands control
 * to Spring Boot's /error handling, which builds the JSON body. That's why
 * /error needs to be public in step 6.
 * 
 * 401 vs 403, two codes that are often confused:
 * 
 * Code Meaning Example Who answers
 * 401 "I don't know who you are" (not authenticated) No token, or expired token
 * AuthEntryPointJwt
 * 403 "I know who you are, but you can't do this" (not authorized) A USER
 * trying to DELETE Spring's access-denied handling
 * 
 * Remember the warning from the start: your ControllerExceptionHandler has a
 * catch-all @ExceptionHandler(Exception.class). With @PreAuthorize, the
 * exception for a 403 is thrown from inside the controller, so that handler
 * will turn it into a 500. We'll fix it in step 8.
 * 
 * Differences with the example
 * username becomes email, and the call is getEmailFromJwtToken (the name we
 * gave it in step 4).
 * I removed the long [IA] comment blocks to keep the files readable, but their
 * lessons are worth remembering. They describe the three classic mistakes of
 * this filter:
 * Reading the token but not validating it or filling the SecurityContext.
 * Comparing with "Bearer: " (with a colon) instead of "Bearer ", which left a
 * space in front of the token and broke the signature.
 * Forgetting filterChain.doFilter(...), which kills the request.
 * Log messages are in English and a bit shorter.
 * Checking
 * Run ./mvnw compile and wait for BUILD SUCCESS.
 * Start the application. Nothing visible changes yet: the filter isn't
 * registered anywhere and AuthEntryPointJwt is a bean nobody uses. It's still
 * the default Spring Boot security. Both pieces start working in step 6.
 * 
 * When you have it compiling, let me know and we move to step 6:
 * WebSecurityConfig, the class that assembles everything built so far (BCrypt,
 * the authentication manager, the filter, the entry point, stateless sessions)
 * and decides which routes are public and which require a token.
 */
