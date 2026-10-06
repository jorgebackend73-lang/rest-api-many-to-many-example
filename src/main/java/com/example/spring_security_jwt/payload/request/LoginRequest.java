package com.example.spring_security_jwt.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}

/*
 * Step 7 builds the two public endpoints: POST /api/auth/signup (register) and
 * POST /api/auth/signin (log in and receive a token). It also covers
 * requirements 2 (login by email) and 3 (validating both requests and returning
 * all the errors) of your exercise.
 * 
 * The big picture
 * 
 * DTOs. A DTO (Data Transfer Object) is a class that models only what travels
 * over HTTP, and it's not stored in the database. We don't use the User entity
 * directly because the client shouldn't decide fields like id or the password
 * hash, and the response must never expose the hash. We create four:
 * 
 * Class Direction Content
 * LoginRequest in email + password
 * SignupRequest in username, email, password, optional roles
 * JwtResponse out token + user data, after a login
 * MessageResponse out a simple {"message": "..."}
 * 
 * Two kinds of "bad JSON". Requirement 3 can mean two different things, and
 * they are detected at different moments:
 * 
 * Kind Example Detected by Result
 * Broken JSON syntax {"email": "a@b.com", Jackson, while reading the body,
 * before the method runs HttpMessageNotReadableException. We'll handle it in
 * step 8
 * Valid JSON with invalid values {"email": "bad"} Bean Validation (@Valid)
 * Collected in a BindingResult: handled in this step
 * 
 * Folders. Inside src/main/java/com/example/spring_security_jwt/ create
 * payload/request, payload/response and controller.
 * 
 * Explanation.
 * 
 * email instead of username: this is requirement 2. The JSON the client sends
 * will be {"email": "...", "password": "..."}.
 * 
 * @NotBlank (from jakarta.validation.constraints, thanks to starter-validation
 * in step 1) rejects null, "" and text with only spaces. A missing field is
 * null, so it's rejected too.
 * 
 * @Email checks the format. It's lenient (user@localhost passes), and it
 * accepts empty or null values, which is why @NotBlank is also there: each
 * annotation covers a different case.
 * Lombok annotations. Jackson builds the object by calling the empty
 * constructor and then filling each field through setters. That's
 * why @NoArgsConstructor and @Data are needed. @AllArgsConstructor and @Builder
 * are useful for tests.
 * I named it LoginRequest. The example has a typo (LogginRequest).
 */