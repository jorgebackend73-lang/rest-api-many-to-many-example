package com.example.spring_security_jwt.security.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.example.spring_security_jwt.security.service.UserDetailsImpl;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Component
public class JwtUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${demo.app.jwtSecret}")
    private String jwtSecret;

    @Value("${demo.app.jwtExpirationMs}")
    private long jwtExpirationMs;

    // Crea el token de un usuario que acaba de autenticarse correctamente
    public String generateJwtToken(Authentication authentication) {

        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        Date now = new Date();

        return Jwts.builder()
                .subject(userPrincipal.getEmail())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    // Construye la clave criptográfica a partir del secreto de application.properties
    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // Lee el email (el "subject") que va dentro del token
    public String getEmailFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Comprueba que el token es válido: bien formado, bien firmado y no caducado
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (SignatureException e) {
            LOGGER.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            LOGGER.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            LOGGER.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            LOGGER.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}

/*
 * Explicación
 * 
 * Imports.
 * 
 * javax.crypto.SecretKey es del propio JDK. Aquí javax es correcto: la
 * advertencia de antes sobre javax.persistence era solo para JPA.
 * org.slf4j.* es la librería de logs que Spring Boot ya trae.
 * org.springframework.beans.factory.annotation.Value,
 * ...security.core.Authentication y ...stereotype.Component son de Spring.
 * io.jsonwebtoken.* es jjwt (paso 1).
 * Cuidado con SignatureException: existen tres en el classpath (java.security,
 * io.jsonwebtoken y io.jsonwebtoken.security). VS Code te ofrecerá varias;
 * elige io.jsonwebtoken.security.SignatureException.
 * 
 * @Component. Es la anotación genérica de Spring para que cree un objeto de
 * esta clase (un bean) y lo ofrezca a quien lo pida. Se usa @Component y
 * no @Service porque la clase no es de lógica de negocio, sino una utilidad.
 * 
 * El LOGGER. LoggerFactory.getLogger(JwtUtils.class) crea un logger que
 * identifica en la consola qué clase escribe el mensaje. Es static final porque
 * basta con uno compartido por toda la clase. El {} es un hueco que se rellena
 * con el segundo argumento. Nunca registres el token ni el secreto completos.
 * 
 * @Value("${demo.app.jwtSecret}"). Es de Spring: busca esa propiedad en
 * application.properties y copia su valor en el campo. Si la propiedad no
 * existe o tiene otro nombre, la aplicación no arranca (error Could not resolve
 * placeholder), lo cual es bueno: avisa pronto. Si el valor de tu secreto tiene
 * espacios al final de la línea, pueden dar problemas al decodificarlo.
 * 
 * El campo jwtExpirationMs es long y no int como en el ejemplo. Un int no puede
 * pasar de unos 2.147 millones de milisegundos (~24 días), así que long evita
 * un desbordamiento si algún día alargas la caducidad.
 * 
 * generateJwtToken
 * Authentication es la interfaz de Spring Security que representa a un usuario
 * ya autenticado. Tras un login correcto, su getPrincipal() contiene el
 * UserDetails que devolvió tu UserDetailsServiceImpl.
 * (UserDetailsImpl) es un cast: getPrincipal() devuelve un Object genérico, y
 * tú sabes que en realidad es un UserDetailsImpl (lo construyó build). Con el
 * cast puedes llamar a getEmail().
 * Date now = new Date() guarda un único instante, para usar el mismo en
 * issuedAt y en el cálculo de la caducidad.
 * Jwts.builder() inicia la construcción, y cada método añade una pieza:
 * .subject(email) → el claim sub. Aquí está el cambio para el login por email.
 * .issuedAt(now) → el claim iat.
 * .expiration(...) → el claim exp: ahora más los milisegundos de
 * application.properties (24 h con tu valor).
 * .signWith(key()) → firma con tu clave. jjwt elige el algoritmo según el
 * tamaño de la clave (HS256, HS384 o HS512).
 * .compact() → genera el texto final del token y lo devuelve. Es el return que
 * al ejemplo le faltaba en una versión anterior: el builder no se modifica por
 * dentro, así que si no devuelves lo que devuelve compact(), el login
 * contestaría con token: null.
 * key()
 * Decoders.BASE64.decode(jwtSecret) convierte tu secreto (un texto en Base64)
 * en bytes. Por eso debe ser Base64 válido.
 * Keys.hmacShaKeyFor(bytes) construye con esos bytes una clave HMAC. Exige al
 * menos 256 bits (32 bytes); si es más corta, lanza WeakKeyException.
 * Es criptografía simétrica: la misma clave firma y verifica, por eso solo debe
 * conocerla el servidor.
 * El método devuelve SecretKey. El ejemplo devuelve java.security.Key y luego
 * necesita el cast (SecretKey) al verificar, porque verifyWith solo acepta
 * tipos concretos. Con SecretKey desde el principio, el cast sobra.
 * getEmailFromJwtToken
 * 
 * Se lee de dentro hacia afuera: Jwts.parser() crea el lector,
 * .verifyWith(key()) le da la clave para comprobar la firma, .build() lo
 * termina de montar, .parseSignedClaims(token) lee el token y verifica la firma
 * (si no es válida, lanza excepción), .getPayload() devuelve los claims y
 * .getSubject() el sub, el email. En el ejemplo se llama
 * getUserNameFromJwtToken; lo renombro porque ahora contiene un email.
 * 
 * validateJwtToken
 * 
 * Intenta leer el token. Si todo va bien, devuelve true. Cada catch cubre un
 * motivo de fallo distinto:
 * 
 * Excepción Cuándo ocurre
 * SignatureException La firma no coincide: token manipulado o firmado con otra
 * clave. Es la que faltaba en el ejemplo.
 * MalformedJwtException El texto no tiene estructura de JWT
 * ExpiredJwtException Ha pasado la fecha exp
 * UnsupportedJwtException Es un JWT pero de un tipo que no aceptamos (por
 * ejemplo, sin firma)
 * IllegalArgumentException El token llega vacío o nulo
 * 
 * En todos los casos se registra el motivo y se devuelve false. El método no
 * lanza la excepción hacia arriba: quien lo llama solo necesita saber
 * "válido o no".
 * 
 * Uso parseSignedClaims en lugar de parse (que usa el ejemplo) porque deja
 * claro que solo admitimos tokens firmados con claims, y es el mismo método que
 * usa getEmailFromJwtToken.
 * 
 * Diferencias con el ejemplo
 * El subject es getEmail() y no getUsername().
 * getUserNameFromJwtToken pasa a llamarse getEmailFromJwtToken. En el paso 5,
 * AuthTokenFilter tendrá que llamar a este nombre nuevo.
 * key() devuelve SecretKey, y se elimina el cast.
 * jwtExpirationMs es long.
 * Un único Date now para iat y exp.
 * Se añade el catch de SignatureException.
 * parseSignedClaims en lugar de parse.
 * Comprobación
 * ./mvnw compile y espera BUILD SUCCESS.
 * Arranca la aplicación. Esto es lo que verifica que @Value encuentra tus dos
 * propiedades: si ves Could not resolve placeholder 'demo.app.jwtSecret',
 * revisa el nombre exacto en application.properties.
 */