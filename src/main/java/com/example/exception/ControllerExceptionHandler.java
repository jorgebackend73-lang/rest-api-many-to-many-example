/*Rest Exception Handler with Controller Advice in Spring.

@RestControllerAdvise

Rest Exception Handler with Controller Advice in Spring Spring supports
exception handling by a global Exception Handler (@ExceptionHandler) with
Controller Advice (@RestControllerAdvice). The @RestControllerAdvice annotation
is a specialization of @Component annotation so that it is auto-detected via
classpath scanning. It is a kind of interceptor that surrounds the logic in our
Controllers and allows us to apply some common logic to them.

Rest Controller Advice’s methods (annotated with @ExceptionHandler) are shared
globally across multiple @Controller components to capture exceptions and
translate them to HTTP responses. The @ExceptionHandler annotation indicates
which type of Exception we want to handle. The exception instance and the
request will be injected via method arguments. By using two annotations
together, we can:Data Management • control the body of the response along with
status code • handle several exceptions in the same method How about
@ResponseStatus? @RestControllerAdvice annotation tells a controller that the
object returned is automatically serialized into JSON and passed to the
HttpResponse object. You only need to return 

Java  body object instead of ResponseEntity object. But the status could be
always OK (200) although the data corresponds to an exception signal (404 – Not
Found for example). @ResponseStatus can help to set the HTTP status code for the
response: @RestControllerAdvice with @ResponseEntity If you use
@RestControllerAdvice without @ResponseBody and @ResponseStatus, you can return
ResponseEntity object instead


*/

// parece que estas excepciones personalizadas nos van a evitar tanto try/catch.

package com.example.exception;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ErrorMessage resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.NOT_FOUND.value(),
                new Date(),
                ex.getMessage(),
                request.getDescription(false));

        return message;
    }

    // 400: the request body is missing, is not valid JSON, or has values of the
    // wrong type
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorMessage malformedJsonException(HttpMessageNotReadableException ex, WebRequest request) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                new Date(),
                "Malformed JSON request: the body is missing, has a syntax error or contains values of the wrong type",
                request.getDescription(false));

        return message;
    }

    // 401: wrong email or password in the signin
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public ErrorMessage badCredentialsException(BadCredentialsException ex, WebRequest request) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.UNAUTHORIZED.value(),
                new Date(),
                "Invalid email or password",
                request.getDescription(false));

        return message;
    }

    // 403: authenticated user without the role required by @PreAuthorize
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    public ErrorMessage accessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.FORBIDDEN.value(),
                new Date(),
                "You do not have permission to perform this action",
                request.getDescription(false));

        return message;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorMessage globalExceptionHandler(Exception ex, WebRequest request) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                new Date(),
                ex.getMessage(),
                request.getDescription(false));

        return message;
    }

}

/*
 * Explanation
 * 
 * Imports: the traps.
 * 
 * AccessDeniedException must be
 * org.springframework.security.access.AccessDeniedException. VS Code may also
 * offer java.nio.file.AccessDeniedException, which is a completely different
 * class.
 * HttpMessageNotReadableException is org.springframework.http.converter..., and
 * BadCredentialsException is org.springframework.security.authentication....
 * 
 * The three new handlers follow the exact pattern you already
 * use: @ExceptionHandler says which exception, @ResponseStatus sets the HTTP
 * code, and the method builds your ErrorMessage.
 * 
 * The 400 handler. Jackson fails while reading the body, before your controller
 * method even starts, and throws HttpMessageNotReadableException. It covers
 * three cases: a missing body, a syntax error (a missing comma or brace), and
 * values of the wrong type (for example "role": "admin" where an array is
 * expected). It complements the BindingResult validation from step 7: that one
 * handles valid JSON with bad values, this one handles JSON that can't even be
 * read.
 * 
 * The 401 handler. We catch the precise BadCredentialsException. Remember
 * DaoAuthenticationProvider hides "user not found" as "bad credentials", so one
 * fixed message covers both cases and never tells an attacker whether the email
 * exists.
 * 
 * The 403 handler. In current Spring Security, a @PreAuthorize failure throws
 * AuthorizationDeniedException, which is a subclass of AccessDeniedException,
 * so this handler catches it. The previous 403 vs 401 distinction from step 5
 * applies: you only reach this handler when you are logged in (an anonymous
 * user is stopped earlier with a 401).
 * 
 * Fixed messages on purpose. For 400 and 401 I don't use ex.getMessage(). The
 * raw text of a JSON error exposes parser internals and class names that a
 * client has no reason to see.
 * 
 * A limit of this class: it only sees what happens inside controllers.
 * Exceptions raised in filters never reach a @RestControllerAdvice. So the 401
 * for a request with no token or an invalid one still comes from
 * AuthEntryPointJwt (step 5) and has Spring Boot's default error JSON, which is
 * slightly different from your ErrorMessage format. The status code is correct;
 * making that body identical is an optional refinement for later.
 * 
 * Differences with the example
 * The seed class creates only roles and users (your project has no products).
 * Same emails and password as the example, so the tests in step 10 can follow
 * its pattern.
 * The @PreAuthorize expressions are exactly the example's: hasRole('ADMIN') for
 * writes and hasRole('ADMIN') or hasRole('USER') for reads.
 * The example's project has no global exception handler, so part C is specific
 * to your project.
 * Checking
 * Run ./mvnw compile and wait for BUILD SUCCESS, then start the application.
 * In the console you should now see insert into roles ... and insert into users
 * ... lines, and insert into user_roles ... after them. No errors.
 * A quick smoke test with Postman (the full test plan is step 9):
 * POST http://localhost:8080/api/auth/signin with {"email": "admin1@gmail.com",
 * "password": "Temp2026$$##"}. Expected: 200 with a token, "type": "Bearer" and
 * "roles": ["ROLE_ADMIN"].
 * The same request with a wrong password. Expected: 401 with
 * "Invalid email or password" (this was a 500 before part C).
 * The same request with a broken body, like {"email": "admin1@gmail.com",.
 * Expected: 400 with "Malformed JSON request...".
 * 
 * If any of the three doesn't behave as described, tell me what you got (status
 * code and body) and we'll look at it before moving on.
 * 
 * When all three work, we continue with step 9: a complete manual test in
 * Postman of the whole matrix (no token, USER token and ADMIN token against
 * each kind of endpoint), which is also how you'll confirm that requirement 1
 * is satisfied before writing the automated tests.
 */
