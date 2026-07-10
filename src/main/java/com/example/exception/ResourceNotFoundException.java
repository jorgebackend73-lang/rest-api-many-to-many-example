package com.example.exception;

public class ResourceNotFoundException extends RuntimeException {

    // Esto es todo para tener nuestra propia excepción personalizada.

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String msg) {
        super(msg);

    
    }

}