package com.example.spring_security_jwt.security.service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.spring_security_jwt.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private long id;
    private String username;
    private String email;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    public static UserDetails build(User user) {

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
/*
 * Explicación
 * 
 * package e imports. El package debe coincidir con la ruta de carpetas. Los
 * imports vienen de varios sitios:
 * 
 * java.util.*: JDK.
 * org.springframework.security.*: Spring Security (starter-security, paso 1).
 * com.example...: tu propia clase User.
 * com.fasterxml.jackson.annotation.JsonIgnore: Jackson, la librería de JSON que
 * ya traes con starter-webmvc.
 * lombok.*: Lombok.
 * 
 * implements UserDetails. Con esto prometes que la clase ofrece lo que Spring
 * Security necesita. La interfaz fuerza tres métodos: getAuthorities(),
 * getPassword() y getUsername(). Otros cuatro (isAccountNonExpired,
 * isAccountNonLocked, isCredentialsNonExpired, isEnabled) ya vienen
 * implementados en la propia interfaz devolviendo true, así que no hace falta
 * escribirlos. Solo los sobrescribirías si quisieras bloquear o caducar
 * cuentas.
 * 
 * ¿Por qué una clase aparte y no User implements UserDetails? Por separación de
 * responsabilidades: User es una entidad de base de datos, y UserDetailsImpl es
 * una copia sencilla de solo lo necesario para seguridad. Además, tiene los
 * roles ya cargados en una lista normal, sin depender de la sesión de Hibernate
 * (esto es importante, lo ves en el archivo 2).
 * 
 * serialVersionUID. UserDetails extiende Serializable, es decir, sus objetos
 * podrían convertirse en bytes (para guardarlos en una sesión HTTP, por
 * ejemplo). Ese número es la "versión" de la clase para ese proceso. En tu
 * aplicación sin sesiones no se llega a usar, pero la constante evita el aviso
 * del compilador. Antes te dije que en User no hacía falta porque no es
 * Serializable; aquí sí.
 * 
 * Las anotaciones de Lombok.
 * 
 * @NoArgsConstructor: constructor vacío.
 * 
 * @AllArgsConstructor: constructor con los cinco campos, en el orden en que
 * están declarados.
 * 
 * @Data: getters, setters, equals, hashCode y toString.
 * 
 * @Builder: permite UserDetailsImpl.builder().id(1).build(), útil para tests.
 * 
 * Los campos. Guardan lo imprescindible: id, username, email, password (el
 * hash) y authorities. El id, username y email no los exige Spring Security,
 * pero los usaremos en la respuesta del login. El tipo Collection<? extends
 * GrantedAuthority> significa
 * "una colección de cualquier tipo que sea un GrantedAuthority" y es
 * exactamente el tipo que exige la interfaz.
 * 
 * @JsonIgnore en password. Si alguna vez este objeto se convirtiera a JSON (por
 * un descuido en un controlador), este campo no saldría. Es una red de
 * seguridad: el hash de una contraseña nunca debe viajar por la red.
 * 
 * public static UserDetails build(User user). Es un método factoría: static
 * significa que se llama sobre la clase (UserDetailsImpl.build(user)), sin
 * crear antes un objeto. Dentro hace dos cosas:
 * 
 * Convierte los roles en autoridades, paso a paso:
 * user.getRoles() devuelve el Set<Role> del usuario.
 * .stream() lo convierte en un flujo que se puede transformar.
 * .map(role -> new SimpleGrantedAuthority(role.getName().name())) convierte
 * cada Role en una autoridad. Cuidado con la confusión de nombres:
 * role.getName() devuelve un ERole (porque tu campo se llama name), y .name()
 * es un método que todos los enums tienen y devuelve el texto de la constante,
 * por ejemplo "ROLE_ADMIN". SimpleGrantedAuthority es la implementación básica
 * de GrantedAuthority.
 * .collect(Collectors.toList()) agrupa todo en una lista. No uso .toList() (más
 * corto) porque devolvería List<SimpleGrantedAuthority>, que Java no acepta
 * como List<GrantedAuthority>.
 * Crea el UserDetailsImpl con el constructor de cinco argumentos. Ojo:
 * username, email y password son los tres String, así que si cambiaras el orden
 * por error, compilaría igual y tendrías los datos cruzados.
 * 
 * Los tres @Override. @Override le dice al compilador
 * "esto implementa un método de la interfaz", y si te equivocas en la firma, te
 * avisa. Lombok ya generaría estos getters, pero los escribimos para que se vea
 * con claridad qué pide el contrato.
 * 
 * Una decisión de diseño importante
 * 
 * getUsername() devuelve el username, no el email. Por contrato, ese método
 * debería devolver el identificador con el que se entra, que en tu ejercicio es
 * el email. Aun así, en nuestro flujo funciona bien, porque Spring Security no
 * usa el valor de getUsername() para buscar usuarios: el identificador de
 * búsqueda viaja como parámetro de loadUserByUsername, y ahí buscaremos por
 * email. Así, la respuesta del login puede mostrar a la vez username y email.
 * Lo único que debes recordar es que el token llevará el email (paso 4).
 */
