package com.example.spring_security_jwt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_security_jwt.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

}

/*
 * JpaRepository<Entidad, TipoDelId> es de Spring Data JPA. Te da gratis save,
 * findById, findAll, deleteById... Spring crea la implementación al arrancar,
 * por eso son interfaces y no hace falta @Repository. Los tipos del id (Integer
 * y Long) deben coincidir con el int de Role y el long de User.
 * Los métodos se traducen solos a SQL por su nombre (derived queries):
 * findByEmail hace SELECT ... WHERE email = ?. Por eso el nombre debe coincidir
 * con el campo de la entidad: email, username, name.
 * Optional<> obliga a gestionar el caso "no existe". Veremos cómo en el paso 3
 * con .orElseThrow(...).
 * existsByUsername y existsByEmail devuelven true o false, y se usan en el
 * registro para avisar de duplicados antes de guardar.
 * Respecto al ejemplo, he puesto findByEmail en lugar de findByUsername, porque
 * ahora el login es por email y el segundo ya no se usaría.
 */
