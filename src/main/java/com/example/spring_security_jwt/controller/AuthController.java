package com.example.spring_security_jwt.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_security_jwt.model.ERole;
import com.example.spring_security_jwt.model.Role;
import com.example.spring_security_jwt.model.User;
import com.example.spring_security_jwt.payload.request.LoginRequest;
import com.example.spring_security_jwt.payload.request.SignupRequest;
import com.example.spring_security_jwt.payload.response.JwtResponse;
import com.example.spring_security_jwt.payload.response.MessageResponse;
import com.example.spring_security_jwt.repository.RoleRepository;
import com.example.spring_security_jwt.repository.UserRepository;
import com.example.spring_security_jwt.security.jwt.JwtUtils;
import com.example.spring_security_jwt.security.service.UserDetailsImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder encoder;

    private final JwtUtils jwtUtils;

    // Register a new user
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest,
            BindingResult validationResults) {

        if (validationResults.hasErrors()) {
            return validationErrorResponse(validationResults);
        }

        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        User user = User.builder()
                .username(signupRequest.getUsername())
                .email(signupRequest.getEmail())
                .password(encoder.encode(signupRequest.getPassword()))
                .build();

        Set<String> strRoles = signupRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {

            roles.add(findRole(ERole.ROLE_USER));

        } else {

            strRoles.forEach(role -> {
                if ("admin".equals(role)) {
                    roles.add(findRole(ERole.ROLE_ADMIN));
                } else {
                    roles.add(findRole(ERole.ROLE_USER));
                }
            });
        }

        user.setRoles(roles);

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    // Log in a registered user using email + password, and return a JWT
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
            BindingResult validationResults) {

        if (validationResults.hasErrors()) {
            return validationErrorResponse(validationResults);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        Set<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        LOGGER.info("User roles: {}", roles);

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    // Builds a 400 response listing ALL the validation errors found
    private ResponseEntity<MessageResponse> validationErrorResponse(BindingResult validationResults) {

        List<String> errors = validationResults.getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest()
                .body(new MessageResponse("Invalid request data: " + errors));
    }

    // Looks up a role in the database or fails if it doesn't exist
    private Role findRole(ERole name) {

        return roleRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Error: Role " + name + " not found"));
    }
}

/*
 * Explanation
 * 
 * Import traps. User exists in two places: choose
 * com.example.spring_security_jwt.model.User, not
 * org.springframework.security.core.userdetails.User. And Valid must be
 * jakarta.validation.Valid.
 * 
 * The class annotations.
 * 
 * @RestController is @Controller + @ResponseBody: every method returns an
 * object that Jackson converts to JSON.
 * 
 * @RequestMapping("/api/auth") sets the base path. It matches the permitAll()
 * rule from step 6 (/api/auth/**), and it doesn't clash with your existing
 * controllers, which use /api.
 * 
 * @RequiredArgsConstructor injects the five final dependencies. Two of them are
 * beans you declared in step 6: the AuthenticationManager and the
 * PasswordEncoder (injected by type: there's only one, BCrypt).
 * registerUser (signup), step by step
 * 
 * @Valid @RequestBody SignupRequest signupRequest:
 * 
 * @RequestBody tells Spring to convert the JSON body into the object.
 * 
 * @Valid runs the constraints (@NotBlank, @Size...) on it.
 * BindingResult validationResults must be declared right after the validated
 * parameter. It changes what happens on failure: without it, Spring throws an
 * exception and the method never runs. With it, Spring does not throw: it fills
 * the BindingResult and runs your method anyway. That's why the first thing we
 * do is check hasErrors(). If you forget that check, invalid data goes straight
 * to save() and fails in the database with a 500.
 * validationErrorResponse collects the errors. getFieldErrors() has one entry
 * per violated constraint, so the result lists everything wrong in a single
 * response, which is requirement 3.
 * Duplicates check. existsByUsername and existsByEmail give a friendly 400.
 * They are not 100% airtight (two simultaneous requests could both pass), but
 * the @UniqueConstraint from step 2 is the final safety net in the database.
 * encoder.encode(...) hashes the password with BCrypt. This is the only moment
 * the raw password is handled. What gets saved is the hash.
 * Roles.
 * If the client sends no roles (or an empty list), the user gets ROLE_USER. The
 * example forgot the empty list case: "role": [] created a user with no roles
 * at all, who could log in but never access anything.
 * If a role is "admin", the user gets ROLE_ADMIN. Anything else gives
 * ROLE_USER.
 * "admin".equals(role) and never role == "admin": == compares whether two
 * Strings are the same object in memory, not their content. Strings coming from
 * JSON are never the same object as your literal, so == would be false always
 * and nobody would ever become admin. Writing the literal first
 * ("admin".equals(...)) also avoids a NullPointerException.
 * findRole is a small helper I extracted to avoid repeating
 * findByName(...).orElseThrow(...) four times. If the role isn't in the
 * database, it throws. It happens when the roles table is empty, which with
 * create-drop is the case on every startup until step 8.
 * user.setRoles(roles) and userRepository.save(user) insert the row in users
 * and the rows in user_roles (the join table).
 * 
 * Security note. Letting any anonymous client register as admin through a
 * public endpoint is something you would never do in a real application. The
 * example does it so that you can create an admin to test the exercise, so we
 * keep it. In a real project, admins are created by other admins or by a seed
 * script.
 * 
 * authenticateUser (signin), step by step
 * Same validation as before, with the same helper.
 * authenticationManager.authenticate(new
 * UsernamePasswordAuthenticationToken(email, password)): this is the login
 * itself. Here the token has two arguments (principal = email as plain text,
 * credentials = raw password), which means "not yet authenticated". Compare it
 * with step 5, where the three-argument version meant "already authenticated".
 * Behind the scenes: the manager passes it to our DaoAuthenticationProvider,
 * which calls loadUserByUsername(email) (this is where the email login works),
 * and then passwordEncoder.matches(...). If all is fine, it returns a filled
 * Authentication. If not, it throws BadCredentialsException.
 * SecurityContextHolder...setAuthentication(authentication) stores the user in
 * the context of the current request. In a stateless API it has almost no
 * practical effect, because the context is discarded when the request ends and
 * the next request is authenticated by the token. It's in the example and it's
 * harmless, so we keep it.
 * jwtUtils.generateJwtToken(authentication) creates the token (step 4).
 * The roles to Set<String>. We cast the principal to UserDetailsImpl (as in
 * step 4), and a stream converts each authority to its text ("ROLE_ADMIN").
 * LOGGER.info("User roles: {}", roles). The example had "...{}" + roles, which
 * concatenates and leaves the {} printed literally. The fix is to pass roles as
 * a second argument.
 * new JwtResponse(jwt, id, username, email, roles) is the response, in the
 * order of the constructor.
 * 
 * Important: a wrong password will give a 500 for now. BadCredentialsException
 * is thrown from the controller, and your
 * catch-all @ExceptionHandler(Exception.class) (the one I warned you about in
 * step 1) turns it into a 500. In step 8 we add the specific handlers so that
 * it becomes a proper 401.
 * 
 * Why ResponseEntity<?>. ResponseEntity represents the full HTTP response
 * (status, headers, body). The ? wildcard allows different body types:
 * MessageResponse or JwtResponse.
 * 
 * Differences with the example
 * username becomes email in LoginRequest and in the authenticate(...) call.
 * LogginRequest is renamed LoginRequest.
 * The duplicated error code is extracted into validationErrorResponse, and the
 * role lookup into findRole.
 * An empty role list now gets ROLE_USER.
 * The log message is fixed ({} placeholder with a comma).
 * I removed the commented-out switch and the long comment blocks.
 * Checking
 * Run ./mvnw compile and wait for BUILD SUCCESS, then start the application.
 * Test the validation (this works without any data). In Postman, send a POST
 * with a JSON body, Content-Type: application/json:
 * POST http://localhost:8080/api/auth/signin with body {}. Expected: 400, with
 * a message listing email and password errors.
 * POST http://localhost:8080/api/auth/signup with body {"username": "",
 * "email": "bad", "password": "1"}. Expected: 400 listing all the problems,
 * including two messages for username (not blank and size) and one for each
 * other field.
 * The order of the errors can vary, and the text may show up in Spanish
 * depending on your system language. That's normal.
 * What you should not expect to work yet:
 * A valid signup returns a 500 (Role ROLE_USER not found), because the roles
 * table is empty. Step 8 fixes that.
 * Broken JSON syntax returns a 500, also handled in step 8.
 * 
 * When you have it compiling and the two validation tests working, let me know
 * and we move to step 8. It's the last step before the manual test, and it has
 * several pieces: the roles and sample users created at startup, @PreAuthorize
 * on TutorialController and TagController so only ADMIN can create, update and
 * delete, and the changes to your ControllerExceptionHandler for 400, 401 and
 * 403.
 */
