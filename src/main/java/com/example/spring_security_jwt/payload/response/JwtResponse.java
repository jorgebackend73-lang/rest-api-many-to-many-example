package com.example.spring_security_jwt.payload.response;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class JwtResponse {

    private String token;

    private final String type = "Bearer";

    private Long id;

    private String username;

    private String email;

    private Set<String> roles;
}

/*
 * Explanation.
 * 
 * It's what the client receives after a successful login: {"token": "eyJ...",
 * "type": "Bearer", "id": 1, "username": "...", "email": "...", "roles":
 * ["ROLE_ADMIN"]}.
 * type is a constant that reminds the client how to send the token:
 * Authorization: Bearer <token> (step 5).
 * A Lombok detail: @AllArgsConstructor skips fields that are final and already
 * initialized, so the constructor has five parameters, in this order: token,
 * id, username, email, roles. The controller will call it exactly like that.
 */
