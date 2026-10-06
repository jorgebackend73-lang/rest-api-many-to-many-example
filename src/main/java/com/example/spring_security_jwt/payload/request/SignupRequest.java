package com.example.spring_security_jwt.payload.request;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SignupRequest {

    @NotBlank
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank
    @Size(max = 45)
    @Email
    private String email;

    private Set<String> role;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;
}

/*
 * Explanation.
 * 
 * @Size(min, max) limits the length. The limits match your User entity
 * (username up to 20, email up to 45), so bad data is rejected with a clear 400
 * and not with a database error.
 * The password: 40 here, 120 in User. The DTO limits what the user types. The
 * entity stores the BCrypt hash, which is longer (about 60 characters).
 * One field can fail several rules at once. An empty password breaks
 * both @NotBlank and @Size(min = 6), and Spring reports each violated
 * constraint separately. That's what gives us "all the errors" for requirement
 * 3.
 * Set<String> role is optional (no annotations). The name is singular because
 * it's the JSON key: "role": ["admin"].
 */
