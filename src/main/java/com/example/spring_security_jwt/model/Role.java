package com.example.spring_security_jwt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "roles")
@NoArgsConstructor 
@AllArgsConstructor 
@Data 
@Builder 
public class Role {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole name;
}

/*
 * Los imports (jakarta.persistence.* y lombok.*) los añade tu editor al
 * escribir las anotaciones.
 * 
 * @Entity (de jakarta.persistence, la API de JPA que Hibernate implementa) le
 * dice a Hibernate que esta clase es una tabla.
 * 
 * @Table(name = "roles") pone el nombre de la tabla. Sin ella se llamaría role.
 * 
 * @NoArgsConstructor, @AllArgsConstructor, @Data y @Builder son de Lombok, que
 * ya tienes en el pom.xml. Generan código por ti:
 * 
 * @NoArgsConstructor: constructor vacío, que JPA exige para crear objetos al
 * leer de la base de datos.
 * 
 * @AllArgsConstructor: constructor con todos los campos. @Builder lo necesita
 * para funcionar.
 * 
 * @Data: getters, setters, equals, hashCode y toString.
 * 
 * @Builder: permite escribir Role.builder().name(ERole.ROLE_USER).build(), que
 * usaremos en los datos iniciales.
 * 
 * @Id marca la clave primaria, y @GeneratedValue(strategy =
 * GenerationType.IDENTITY) hace que MySQL la genere con autoincremento.
 * 
 * @Enumerated(EnumType.STRING) guarda el rol como texto ("ROLE_ADMIN"). Si no
 * lo pones, guarda la posición (0, 1), y si algún día reordenas el enum, todos
 * los roles de la base de datos quedan cambiados sin avisar.
 * 
 * @Column(length = 20) limita la columna a 20 caracteres, de sobra para
 * ROLE_ADMIN.
 * 
 */