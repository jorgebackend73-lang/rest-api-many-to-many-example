package com.example;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.spring_security_jwt.model.ERole;
import com.example.spring_security_jwt.model.Role;
import com.example.spring_security_jwt.model.User;
import com.example.spring_security_jwt.repository.RoleRepository;
import com.example.spring_security_jwt.repository.UserRepository;

@Configuration
public class CreatesSamplesData {

    @Bean
    CommandLineRunner samplesData(RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // Roles must exist first: users reference them through the user_roles table
            Role userRole = roleRepository.save(Role.builder().name(ERole.ROLE_USER).build());

            Role adminRole = roleRepository.save(Role.builder().name(ERole.ROLE_ADMIN).build());

            // Two users to test the endpoints, one per role
            userRepository.save(User.builder()
                    .username("admin1")
                    .email("admin1@gmail.com")
                    .roles(Set.of(adminRole))
                    .password(passwordEncoder.encode("Temp2026$$##"))
                    .build());

            userRepository.save(User.builder()
                    .username("user1")
                    .email("user1@gmail.com")
                    .roles(Set.of(userRole))
                    .password(passwordEncoder.encode("Temp2026$$##"))
                    .build());
        };
    }
}

/*
 * Step 8 has three parts, and I suggest doing them in this order, then
 * compiling once at the end:
 * 
 * Part What Why
 * A Create roles and two sample users at startup create-drop empties the
 * database on every start, so without roles no signup can work
 * B @PreAuthorize in TutorialController and TagController Requirement 1: only
 * ADMIN can create, update and delete
 * C New handlers in ControllerExceptionHandler Turn malformed JSON, wrong
 * credentials and denied access into 400, 401 and 403 instead of 500
 * Part A: roles and sample users at startup
 * 
 * 
 * Explanation
 * CommandLineRunner is a Spring Boot interface with a single method. Spring
 * Boot finds every bean of this type and runs it once, when the application has
 * finished starting. It's the standard place for initial data. The args
 * parameter holds the command-line arguments, which we don't use.
 * 
 * @Configuration + @Bean: the method declares a runner bean, and the lambda
 * args -> { ... } is its run method. Because the class is in com.example,
 * component scanning finds it.
 * The method parameters (RoleRepository, UserRepository, PasswordEncoder) are
 * injected by type. The PasswordEncoder is the BCrypt bean from step 6.
 * Role.builder().name(...).build() uses the @Builder from step 2. save()
 * returns the saved object with its generated id, so we use that returned
 * instance (userRole, adminRole) afterwards.
 * .roles(Set.of(adminRole)) assigns the role. Set.of creates an immutable set
 * with one element.
 * passwordEncoder.encode(...) stores the BCrypt hash. The raw password is never
 * saved, but you'll use it to log in.
 * Why roles first: user_roles has foreign keys to both tables, so a role row
 * must exist before a user can point to it.
 * These users let you test the exercise without registering anyone. The
 * credentials are in the table below (it's local sample data: never hardcode
 * passwords like this in a real project).
 * Email Password Role
 * admin1@gmail.com Temp2026$$## ROLE_ADMIN
 * user1@gmail.com Temp2026$$## ROLE_USER
 * 
 * One caveat: this only works cleanly because of ddl-auto=create-drop (empty
 * tables on every start). If you switched to update, the seed would run again
 * on a populated database and try to create duplicates.
 * 
 * Part B: @PreAuthorize on the controllers
 * What @PreAuthorize does
 * 
 * It comes from org.springframework.security.access.prepost.PreAuthorize. It
 * evaluates an expression before the method runs. If it's false, the method
 * never executes and an AccessDeniedException is thrown. It works because step
 * 6's @EnableMethodSecurity makes Spring wrap your controllers in a proxy that
 * performs that check first. Without that annotation, @PreAuthorize would be
 * silently ignored.
 * 
 * The text inside is a SpEL expression (Spring Expression Language), evaluated
 * at runtime.
 * hasRole('ADMIN') checks whether the current user (the one stored in the
 * SecurityContext by AuthTokenFilter) has the authority ROLE_ADMIN. Spring adds
 * the ROLE_ prefix itself, which is why we named our roles that way in step 2.
 * hasRole('ADMIN') or hasRole('USER') combines two checks with or. It's the
 * same as hasAnyRole('ADMIN', 'USER').
 * Two layers of security
 * 
 * Your application now checks access twice:
 * 
 * Layer Where Question it answers Failure
 * URL rules filterChain (step 6) Is there a valid logged-in user? 401
 * Method rules @PreAuthorize Does this user have the right role? 403
 * 
 * The GET endpoints are already protected against anonymous users by
 * anyRequest().authenticated(). We still annotate them with hasRole('ADMIN') or
 * hasRole('USER') as the example does: it makes the intention explicit and
 * keeps them protected if someone later relaxes the URL rules.
 * 
 * Which role for which endpoint
 * 
 * Requirement 1 says only ADMIN can create, update and delete, so:
 * 
 * Endpoint Role
 * GET /api/tutorials, GET /api/tutorials/{id}, GET /api/tutorials/published
 * USER or ADMIN
 * GET /api/tags, GET /api/tutorials/{id}/tags, GET /api/tags/{id}/tutorials
 * USER or ADMIN
 * POST /api/tutorials, PUT /api/tutorials/{id} ADMIN
 * DELETE /api/tutorials/{id}, DELETE /api/tutorials ADMIN
 * POST /api/tutorials/{id}/tags, PUT /api/tags/{id} ADMIN
 * DELETE /api/tutorials/{tutorialId}/tags/{tagId}, DELETE /api/tags/{id} ADMIN
 * 
 * Note that POST /api/tutorials/{id}/tags counts as a creation (it can create a
 * new tag), and DELETE /api/tutorials deletes everything, so both are ADMIN
 * only.
 */
