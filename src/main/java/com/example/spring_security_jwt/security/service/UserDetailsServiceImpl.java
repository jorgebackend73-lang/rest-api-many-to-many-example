package com.example.spring_security_jwt.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_security_jwt.model.User;
import com.example.spring_security_jwt.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) 
        throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                // TODO. cambiar UsernameNotFoundException por EmailNotFoundException
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));

        return UserDetailsImpl.build(user);
    }
}

/*
 * Explicación
 * 
 * @Service. Es una anotación de Spring que marca la clase como componente de la
 * capa de servicio. Al arrancar, Spring escanea com.example y sus subpaquetes,
 * encuentra la clase y crea un único objeto (un bean) que podrá inyectar donde
 * se necesite. Sin ella, Spring no sabría que existe.
 * 
 * @RequiredArgsConstructor y private final UserRepository. Lombok genera un
 * constructor con todos los campos final, y Spring, al ver un único
 * constructor, le pasa el UserRepository automáticamente (inyección de
 * dependencias por constructor, sin necesidad de @Autowired). Con final, el
 * campo no puede quedar a null ni cambiarse después. El UserRepository que se
 * inyecta es la implementación que Spring Data creó por ti.
 * 
 * implements UserDetailsService. Es el contrato de Spring Security. De él viene
 * el nombre del método, loadUserByUsername, que no puedes cambiar, aunque
 * dentro busquemos por email. Por eso el parámetro lo he llamado email: Java
 * permite cambiar el nombre de un parámetro al implementar un método, y así el
 * código se lee con claridad.
 * 
 * findByEmail(email).orElseThrow(...). El repositorio devuelve un
 * Optional<User>, que puede contener un usuario o estar vacío. orElseThrow
 * significa "dame el usuario, o lanza esta excepción si no existe". La parte ()
 * -> new ... es una lambda: la excepción solo se crea si hace falta.
 * 
 * UsernameNotFoundException. Es la excepción propia de Spring Security. Aunque
 * aquí digamos "User not found with email", cuando falla un login Spring la
 * transforma por defecto en un BadCredentialsException ("Bad credentials"), y
 * así quien intenta entrar no puede saber si falló el email o la contraseña. Es
 * una protección contra el user enumeration. Si el fallo ocurre dentro de
 * AuthTokenFilter, esa clase captura la excepción y la registra en el log.
 * 
 * return UserDetailsImpl.build(user). Usa la factoría del archivo anterior para
 * entregarle a Spring Security el usuario en su formato.
 * 
 * @Transactional: la parte más delicada. Recuerda que en User pusimos los roles
 * como FetchType.LAZY: Hibernate no los carga al leer el usuario, sino la
 * primera vez que se tocan. Y UserDetailsImpl.build toca user.getRoles(). Para
 * cargarlos, Hibernate necesita una sesión abierta. En una petición web normal,
 * Spring Boot mantiene abierta la sesión durante todo el controlador, pero eso
 * lo hace un componente de Spring MVC que actúa después de los filtros de
 * seguridad. Nuestro método se ejecuta antes (en el login y en el filtro JWT),
 * así que, sin @Transactional, aparecería un LazyInitializationException. Con
 * la anotación, Spring abre una transacción al entrar en el método y la cierra
 * al salir, y build queda dentro, con la sesión abierta.
 * 
 * La anotación @Transactional existe en dos versiones. El ejemplo usa
 * jakarta.transaction.Transactional, y yo uso la de Spring
 * (org.springframework.transaction.annotation.Transactional), que es la del
 * propio framework que gestiona la transacción. Ambas funcionan.
 * 
 * Diferencias con el ejemplo
 * findByEmail en lugar de findByUsername, y el mensaje de error habla de email.
 * Sin @SuppressWarnings("null"), ni @Nullable, ni los comentarios TODO
 * Auto-generated. Eran restos generados por el IDE al implementar la interfaz.
 * Si VS Code te muestra algún aviso amarillo sobre nulidad, es solo un aviso y
 * no impide compilar.
 * 
 * @Transactional de Spring en lugar de la de Jakarta.
 */