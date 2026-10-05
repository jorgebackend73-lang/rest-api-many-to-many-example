package com.example.spring_security_jwt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_security_jwt.model.ERole;
import com.example.spring_security_jwt.model.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(ERole name);

}

/*
 * JpaRepository<Entidad, TipoDelId> es de Spring Data JPA. Te da gratis save,
 * findById, findAll, deleteById... Spring crea la implementación al arrancar,
 * por eso son interfaces y no hace falta @Repository. Los tipos del id 
 * (Integer y Long) deben coincidir con el int de Role y el long de User.
 */
