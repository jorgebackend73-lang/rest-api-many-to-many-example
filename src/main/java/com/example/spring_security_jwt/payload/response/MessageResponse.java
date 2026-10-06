package com.example.spring_security_jwt.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class MessageResponse {

    private String message;
}

/*
 * A minimal wrapper to answer with {"message": "..."}. A plain String would
 * send raw text, not JSON.
 */
