package com.example.spring_security_jwt.security.jwt;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.spring_security_jwt.security.service.UserDetailsServiceImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {

            String jwt = parseJwt(request);

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                String email = jwtUtils.getEmailFromJwtToken(jwt);

                UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception e) {

            LOGGER.error("Cannot set user authentication: {}", e.getMessage());
        }

        // Always pass the request to the next filter in the chain
        filterChain.doFilter(request, response);
    }

    // Extracts the token from the header "Authorization: Bearer <token>"
    private String parseJwt(HttpServletRequest request) {

        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {

            return headerAuth.substring(7);
        }

        return null;
    }
}

/*
 * Step 5 is where the JWT stops being a loose piece and starts affecting
 * requests. We create two classes in the jwt folder you made in step 4.
 * 
 * The big picture
 * 
 * Spring Security works as a chain of filters that every HTTP request crosses
 * before reaching your controllers. A filter is a class that can look at the
 * request, act on it, and then pass it to the next filter. Our AuthTokenFilter
 * will be inserted into that chain (step 6) and has one job: if the request
 * carries a valid token, tell Spring Security who the user is.
 * 
 * It never rejects anything by itself. It only authenticates when it can:
 * 
 * Request What the filter does What happens next
 * Valid token Validates it, loads the user, stores them as "the current user"
 * Request continues authenticated, and @PreAuthorize checks the role
 * No token, or invalid or expired token Nothing: the request stays anonymous
 * Public endpoint: works. Protected endpoint: AuthEntryPointJwt answers 401
 * 
 * The decision to deny access is taken later in the chain, not here.
 * AuthEntryPointJwt is the piece that decides what the 401 response looks like.
 * 
 * 
 * Explanation
 * 
 * Imports: pick the right one. Several of these class names exist in more than
 * one library, so check what VS Code offers:
 * 
 * StringUtils: choose org.springframework.util.StringUtils, not the Apache one.
 * jakarta.servlet.* comes with the embedded Tomcat that the web starter brings.
 * These are the standard servlet classes (HttpServletRequest, FilterChain...).
 * WebAuthenticationDetailsSource is in
 * org.springframework.security.web.authentication.
 * 
 * extends OncePerRequestFilter. It's a Spring base class for filters. It
 * guarantees that your logic runs once per request (a request can be
 * re-dispatched internally, for example to /error, and a plain filter could run
 * twice). Instead of doFilter, you implement doFilterInternal, which is where
 * your logic goes. By default it also doesn't run on those internal error
 * redispatches, which is why in step 6 we'll have to make /error public.
 * 
 * No @Component. The class is deliberately not annotated. In step 6,
 * WebSecurityConfig will create it explicitly with new
 * AuthTokenFilter(jwtUtils, userDetailsService).
 * 
 * @RequiredArgsConstructor and the two final fields. Lombok generates the
 * constructor that step 6 will call. The filter needs JwtUtils (from step 4) to
 * validate and read the token, and UserDetailsServiceImpl (step 3) to load the
 * user.
 * 
 * parseJwt (the header). Clients send the token in an HTTP header named
 * Authorization, with this exact form: Authorization: Bearer eyJhbGciOi....
 * Bearer is the standard scheme name, meaning "whoever bears this token". The
 * method:
 * 
 * StringUtils.hasText(...) is true if the text is not null, empty, or only
 * spaces.
 * startsWith("Bearer ") checks the scheme, with a space and without a colon.
 * substring(7) removes the first 7 characters. "Bearer " has exactly 7 (six
 * letters plus the space), so what's left is only the token.
 * If the header is missing or has another format, it returns null.
 * 
 * The if. jwt != null && jwtUtils.validateJwtToken(jwt) means
 * "there is a token and it's authentic and not expired". Otherwise the block is
 * skipped and the request stays anonymous. Java evaluates && from left to right
 * and stops at the first false, so validateJwtToken never receives a null.
 * 
 * Inside the if, four steps:
 * 
 * getEmailFromJwtToken(jwt) reads the email from the token. This is the line
 * adapted to your exercise: the example read a username here, but your token
 * carries an email.
 * loadUserByUsername(email) fetches the user and their current roles from the
 * database (the extra query per request I mentioned in step 3).
 * new UsernamePasswordAuthenticationToken(userDetails, null,
 * userDetails.getAuthorities()) builds the Authentication object (the "ticket"
 * saying who this is). The name is misleading: no username or password is being
 * used here, it's just Spring's standard implementation. The three arguments
 * are the principal (the user), the credentials (null: the token already proved
 * identity, so there's no password to carry) and the authorities (the roles).
 * Using this three-argument constructor marks the object as already
 * authenticated. The two-argument one doesn't.
 * setDetails(...) attaches extra info about the request (client IP address,
 * session id). It's informative, for auditing, and not required for it to work.
 * 
 * SecurityContextHolder.getContext().setAuthentication(authentication). This is
 * the key line. The SecurityContext is where Spring Security keeps the answer
 * to "who is making this request?". By default it's stored in a ThreadLocal,
 * meaning it's tied to the thread handling this request and disappears when the
 * request ends, which fits our stateless setup. Everything later reads it from
 * here: the rule anyRequest().authenticated() and each @PreAuthorize.
 * 
 * catch (Exception e). If anything fails (the user was deleted so
 * UsernameNotFoundException is thrown, the database is down...), the error is
 * logged and the request continues unauthenticated. If the endpoint is
 * protected, the user gets a 401 afterwards. The log is important: an empty
 * catch would hide the cause.
 * 
 * filterChain.doFilter(request, response): outside the try, and mandatory. It
 * hands the request to the next filter. It sits outside the try for two
 * reasons: it must run whether authentication succeeded or not, and if it were
 * inside, the catch would also swallow exceptions thrown by later filters and
 * your controllers. If you forget this line, the chain stops here, the request
 * never reaches your controllers, and you get an empty 200.
 */