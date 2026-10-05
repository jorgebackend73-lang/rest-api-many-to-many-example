package com.example.spring_security_jwt.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
    })
@NoArgsConstructor 
@AllArgsConstructor 
@Data 
@Builder 
public class User {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank 
    @Size (max = 20)
    private String username;

    @NotBlank
    @Size(max = 45)
    @Email 
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    @ManyToMany (fetch = FetchType.LAZY)
    @JoinTable (name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}

/*
 * La tabla se llama users (plural) porque user es palabra reservada en algunas
 * bases de datos y da problemas.
 * uniqueConstraints hace que la base de datos rechace dos usuarios con el mismo
 * username o email. Tu enunciado usa el email para loguear, así que debe ser
 * único; de lo contrario, un login podría coincidir con dos personas.
 * 
 * @NotBlank, @Size y @Email son de jakarta.validation (la dependencia
 * starter-validation del paso 1). Exigen que el campo no esté vacío, limitan su
 * longitud y comprueban el formato de email. Aquí protegen la entidad al
 * guardarla, y en el paso 7 los DTOs usarán las mismas anotaciones para validar
 * lo que llega por HTTP.
 * password admite 120 caracteres porque no se guarda la contraseña, sino su
 * hash BCrypt, que ocupa unos 60. Nunca se almacenan contraseñas en claro.
 * 
 * @ManyToMany indica que un usuario puede tener varios roles y un rol
 * pertenecer a varios usuarios.
 * FetchType.LAZY carga los roles solo cuando se piden, no al leer el usuario.
 * Esto tendrá consecuencias en el paso 3: por eso UserDetailsServiceImpl
 * llevará @Transactional.
 * 
 * @JoinTable define la tabla intermedia user_roles con sus dos columnas
 * (user_id y role_id).
 * La relación es unidireccional: User conoce sus roles, pero Role no conoce sus
 * usuarios. En tu Tutorial y Tag es bidireccional (con mappedBy en Tag), pero
 * aquí no hace falta ver los usuarios de un rol.
 * Set y no List, para que un usuario no pueda tener el mismo rol dos veces.
 * 
 * @Builder.Default es necesario: sin él, @Builder ignora el = new HashSet<>() y
 * los usuarios creados con User.builder() tendrían roles a null.
 * 
 * El ejemplo incluye serialVersionUID, pero estas clases no implementan
 * Serializable, así que no sirve de nada y lo dejamos fuera. Y @Data en
 * entidades JPA no es la mejor práctica en proyectos grandes (por el equals y
 * hashCode con relaciones), pero aquí no da problemas porque Role no apunta de
 * vuelta a User.
 * 
 */