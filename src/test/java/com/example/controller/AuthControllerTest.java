package com.example.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class AuthControllerTest extends AbstractControllerTest {

    private ResultActions signin(String json) throws Exception {

        return perform("POST", "/api/auth/signin", null, json);
    }

    private ResultActions signup(String json) throws Exception {

        return perform("POST", "/api/auth/signup", null, json);
    }

    // Short random text, so every test registers a different user in the same
    // database
    private static String unique() {

        return UUID.randomUUID().toString().substring(0, 8);
    }

    // ================= signin (requirement 2: login by email) =================

    @Test
    @DisplayName("signin with correct email and password returns the token and the user data")
    void testSigninSuccess() throws Exception {

        // given
        String json = """
                {"email": "%s", "password": "%s"}
                """.formatted(ADMIN_EMAIL, PASSWORD);

        // when and then
        signin(json)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.type", is("Bearer")))
                .andExpect(jsonPath("$.username", is("test-admin")))
                .andExpect(jsonPath("$.email", is(ADMIN_EMAIL)))
                .andExpect(jsonPath("$.roles", contains("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("signin with a wrong password answers 401")
    void testSigninWrongPassword() throws Exception {

        // given
        String json = """
                {"email": "%s", "password": "wrong-password"}
                """.formatted(USER_EMAIL);

        // when and then
        signin(json)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("signin with an unknown email answers 401 with the same message")
    void testSigninUnknownEmail() throws Exception {

        // given
        String json = """
                {"email": "nobody-%s@example.com", "password": "whatever123"}
                """.formatted(unique());

        // when and then: the answer doesn't reveal that the email does not exist
        signin(json)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("signin with an empty JSON reports the errors of email and password")
    void testSigninEmptyBody() throws Exception {

        signin("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", allOf(
                        containsString("email"),
                        containsString("password"))));
    }

    @Test
    @DisplayName("signin with an email in a wrong format reports the email error")
    void testSigninInvalidEmailFormat() throws Exception {

        // given
        String json = """
                {"email": "not-an-email", "password": "secret123"}
                """;

        // when and then
        signin(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("email")));
    }

    @Test
    @DisplayName("signin with the username instead of the email is rejected: the login is by email")
    void testSigninWithUsernameInsteadOfEmail() throws Exception {

        // given
        String json = """
                {"username": "test-admin", "password": "%s"}
                """.formatted(PASSWORD);

        // when and then
        signin(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("email")));
    }

    @Test
    @DisplayName("signin with malformed JSON answers 400")
    void testSigninMalformedJson() throws Exception {

        signin("{\"email\": \"test-admin@example.com\",")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
    }

    // ================= signup (requirement 3: validation) =================

    @Test
    @DisplayName("signup registers a user who can then log in with the email")
    void testSignupThenSigninWithEmail() throws Exception {

        // given
        String username = "u" + unique();
        String email = username + "@example.com";

        String signupJson = """
                {"username": "%s", "email": "%s", "password": "secret123"}
                """.formatted(username, email);

        String signinJson = """
                {"email": "%s", "password": "secret123"}
                """.formatted(email);

        // when and then
        signup(signupJson)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("User registered successfully!")));

        signin(signinJson)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(username)))
                .andExpect(jsonPath("$.roles", contains("ROLE_USER")));
    }

    @Test
    @DisplayName("signup with an existing username answers 400")
    void testSignupDuplicateUsername() throws Exception {

        // given: "test-user" already exists (created by the base class)
        String json = """
                {"username": "test-user", "email": "new-%s@example.com", "password": "secret123"}
                """.formatted(unique());

        // when and then
        signup(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Error: Username is already taken!")));
    }

    @Test
    @DisplayName("signup with an existing email answers 400")
    void testSignupDuplicateEmail() throws Exception {

        // given
        String json = """
                {"username": "u%s", "email": "%s", "password": "secret123"}
                """.formatted(unique(), USER_EMAIL);

        // when and then
        signup(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Error: Email is already in use!")));
    }

    @Test
    @DisplayName("signup reports ALL the validation errors at once")
    void testSignupReportsAllValidationErrors() throws Exception {

        // given: username empty, email malformed, password too short
        String json = """
                {"username": "", "email": "bad", "password": "1"}
                """;

        // when and then: the three fields appear in the same response
        signup(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", allOf(
                        containsString("username"),
                        containsString("email"),
                        containsString("password"))));
    }

    @Test
    @DisplayName("signup with role admin creates an ADMIN user")
    void testSignupWithAdminRole() throws Exception {

        // given
        String username = "a" + unique();
        String email = username + "@example.com";

        String signupJson = """
                {"username": "%s", "email": "%s", "password": "secret123", "role": ["admin"]}
                """.formatted(username, email);

        String signinJson = """
                {"email": "%s", "password": "secret123"}
                """.formatted(email);

        // when and then
        signup(signupJson).andExpect(status().isOk());

        signin(signinJson)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", contains("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("signup with an empty role list creates a USER and not a user without roles")
    void testSignupWithEmptyRoleList() throws Exception {

        // given
        String username = "e" + unique();
        String email = username + "@example.com";

        String signupJson = """
                {"username": "%s", "email": "%s", "password": "secret123", "role": []}
                """.formatted(username, email);

        String signinJson = """
                {"email": "%s", "password": "secret123"}
                """.formatted(email);

        // when and then
        signup(signupJson).andExpect(status().isOk());

        signin(signinJson)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", contains("ROLE_USER")));
    }

    @Test
    @DisplayName("signup with malformed JSON answers 400")
    void testSignupMalformedJson() throws Exception {

        signup("{\"username\": \"ana\", \"email\":")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
    }
}

/*
 * This one has no mocks involved: it exercises the real signup and signin, and
 * with them requirements 2 and 3.
 */

/*
 * Explanation
 * unique() gives each signup test its own username and email. The tests write
 * real rows in MySQL that aren't rolled back (a @SpringBootTest isn't
 * transactional by default), so reusing the same values would collide on the
 * unique constraints. The database is dropped and recreated the next time the
 * context starts anyway. Usernames stay within the 3 to 20 characters allowed
 * ("u" plus 8 characters).
 * testSigninWithUsernameInsteadOfEmail is the direct proof of requirement 2:
 * the old identifier no longer opens the door.
 * testSignupReportsAllValidationErrors is the direct proof of requirement 3:
 * three bad fields, one response listing all of them. We check field names and
 * not the exact wording because Hibernate Validator's messages follow the
 * system language, so the same test passes on a Spanish or English machine.
 * testSignupWithEmptyRoleList locks in the fix from step 7: "role": [] used to
 * create a user with no roles at all.
 * testSignupThenSigninWithEmail goes through the full real chain: validation,
 * duplicate check, BCrypt hashing, save, then login by email on the stored
 * hash.
 */

/*
 * Running them
 * ./mvnw test
 * -Dtest="TutorialControllerTest,TagControllerTest,AuthControllerTest"
 * 
 * Expected result: 79 tests (33 + 32 + 14), no failures. The first one takes a
 * few seconds because the full application starts once (you'll see the Spring
 * banner and the SQL), and the rest are quick.
 * 
 * Then run the whole suite:
 * 
 * ./mvnw test
 * 
 * You should get 113 tests (13 repository + 20 service + 79 controller + the
 * original contextLoads) and BUILD SUCCESS. Running it all together also checks
 * that the classes don't interfere with each other.
 * 
 * Two sanity checks worth doing once (undo each afterward):
 * 
 * Remove @PreAuthorize("hasRole('ADMIN')") from deleteAllTutorials in
 * TutorialController. The test USER DELETE /api/tutorials -> 403 must fail.
 * In WebSecurityConfig, temporarily change .anyRequest().authenticated() to
 * .anyRequest().permitAll(). All the "without token -> 401" tests must fail.
 * 
 * If neither breaks the suite, those tests aren't protecting you, and I'd want
 * to know.
 * 
 * If something fails
 * Symptom Likely cause
 * Everything fails in setup with Status expected:<200> but was:<401> The token
 * login failed: check the JWT properties in application.properties
 * (demo.app.jwtSecret, demo.app.jwtExpirationMs)
 * Communications link failure at startup MySQL is not running
 * Tests start but nothing runs, or the classes aren't found A file is in
 * src/main/java, or the class name doesn't end in Test
 * Red import on MockitoBean, AutoConfigureMockMvc Wrong package: use
 * org.springframework.test.context.bean.override.mockito.MockitoBean and
 * org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
 * A USER write -> 403 test gets 400 The request went without a body: always
 * send one (the helper does it by default)
 * UnsatisfiedDependencyException or NoSuchBeanDefinition at startup A class
 * from a previous step has a wrong package or is missing a @Component/@Service
 * 
 * Send me the first error line if one doesn't fit the table.
 * 
 * You've finished: how each requirement is covered
 * # Requirement Implemented in Verified by
 * 1 JWT security by roles; only ADMIN creates, updates, deletes
 * WebSecurityConfig, AuthTokenFilter, JwtUtils, @PreAuthorize The access
 * matrices in TutorialControllerTest and TagControllerTest
 * 2 Login with email instead of username LoginRequest,
 * UserDetailsServiceImpl.findByEmail, JwtUtils subject
 * testSigninWithUsernameInsteadOfEmail, UserRepositoryTest
 * 3 Validation of signup and signin, returning all errors @Valid +
 * BindingResult, HttpMessageNotReadableException handler
 * testSignupReportsAllValidationErrors, the malformed-JSON tests
 * 4 Repository tests 10B TutorialRepositoryTest, TagRepositoryTest,
 * UserRepositoryTest
 * 5 Service tests 10A (new layer) and 10C TutorialServiceImplTest,
 * TagServiceImplTest
 * 6 Controller integration tests with security 10D The three controller test
 * classes
 * 
 * Before you hand it in, a short checklist:
 * 
 * Run the full ./mvnw test once more from a clean state: ./mvnw clean test.
 * Don't commit real secrets. Your jwtSecret and database password are in
 * application.properties. For the exercise it's fine, but check what your
 * teacher expects before pushing to GitHub.
 * Remember the two deliberately insecure things we kept to match the example:
 * signup lets anyone choose the admin role, and the sample users share one
 * fixed password. Both are fine for a class exercise and wrong for a real
 * application.
 * If your teacher compares with the example, the differences are small and
 * deliberate: email login, the extra SignatureException catch, the empty-role
 * fix, the /error rule, the 400/401/403 handlers, and the service layer.
 * They're all documented in the "Differences with the example" sections of each
 * step.
 * 
 * If you want, tell me what your teacher asked you to hand in, and I can help
 * you review the project as a whole before you deliver it.
 * 
 * I'm sorry, but testing after 10B, I have this:
 * 
 * [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.386
 * s -- in com.example.spring_security_jwt.repository.UserRepositoryTest
 * [INFO]
 * [INFO] Results:
 * [INFO]
 * [ERROR] Errors:
 * [ERROR] UserRepositoryTest » NoClassDefFound UserRepository
 * [INFO]
 * [ERROR] Tests run: 14, Failures: 0, Errors: 1, Skipped: 0
 * [INFO]
 * [INFO]
 * ------------------------------------------------------------------------
 * [INFO] BUILD FAILURE
 * [INFO]
 * ------------------------------------------------------------------------
 * [INFO] Total time: 22.944 s
 * [INFO] Finished at: 2026-10-07T10:24:04+02:00
 * [INFO]
 * ------------------------------------------------------------------------
 * [ERROR] Failed to execute goal
 * org.apache.maven.plugins:maven-surefire-plugin:3.5.6:test (default-test) on
 * project rest-api-many-to-many-example:
 * [ERROR]
 * [ERROR] See
 * /home/usuario/Documentos/vscode_proyects/REST_API/rest-api-many-to-many-
 * example/target/surefire-reports for the individual test results.
 * [ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and
 * [date].dumpstream.
 * [ERROR] -> [Help 1]
 * [ERROR]
 * [ERROR] To see the full stack trace of the errors, re-run Maven with the -e
 * switch.
 * [ERROR] Re-run Maven using the -X switch to enable full debug logging.
 * [ERROR]
 * [ERROR] For more information about the errors and possible solutions, please
 * read the following articles:
 * [ERROR] [Help 1]
 * http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
 * 
 * I see no error in code, seem like some error in maven plugins, application
 * was down and docker my-sql server up.
 * 
 * You're right that this doesn't look like a code error, and the output
 * supports that:
 * 
 * UserRepositoryTest reports 5 tests run, 0 failures, 0 errors, so its
 * assertions and your mappings are fine.
 * The only error is a NoClassDefFoundError, which means
 * "the JVM needed this class and couldn't load it". It isn't an assertion
 * failing, and it's reported at class level, after the tests ran. So it's a
 * build or classloading problem, not a test problem.
 * The surefire plugin is just the messenger. It's reporting what the test JVM
 * threw.
 * Your setup (app stopped, MySQL in Docker up) is correct.
 * 
 * I can't tell the exact cause from this output alone. The most common one for
 * this error is an inconsistent target/ folder: the VS Code Java extension
 * compiles into the same target/classes folder that Maven uses, and you've just
 * moved test files between folders and fixed the red imports, so VS Code has
 * been rebuilding a lot. If it rewrites class files while Maven's test JVM is
 * running, or leaves stale ones behind, you get exactly this. Treat that as a
 * hypothesis to test, not a diagnosis.
 * 
 * 1. The cheap fix
 * 
 * Wait until the VS Code status bar shows no background Java build, or close VS
 * Code entirely, then in a plain terminal:
 * 
 * ./mvnw clean test
 * 
 * clean deletes target/ so everything is compiled from scratch.
 * 
 * 2. If it fails again, get the evidence
 * 
 * Capture the full output and look at the stack trace:
 * 
 * ./mvnw clean test 2>&1 | tee test-output.log
 * grep -n -B5 -A25 "NoClassDefFoundError" test-output.log | head -80
 * 
 * The surefire report for the class also has it:
 * 
 * sed -n '1,60p'
 * target/surefire-reports/com.example.spring_security_jwt.repository.
 * UserRepositoryTest.txt
 * ls target/surefire-reports | grep -i dump
 * 
 * The Caused by: lines will show when the class was needed (during startup, in
 * the middle of a test, or at shutdown), which tells us whether to look at the
 * build or at the Spring context. Send me what you find.
 * 
 * 3. To narrow it down, run the class alone
 * ./mvnw clean test -Dtest=UserRepositoryTest
 * If it passes alone, the problem only shows up when the classes run together
 * (for example, something at the end of the whole run).
 * If it fails alone, it's specific to that class or to your environment.
 * 
 * Tell me what clean test gives you. If it comes back green, the target/ theory
 * was right, and the practical rule is: don't run Maven while the Java
 * extension is still building, and use clean after moving files around. Then we
 * can continue with 10C.
 * 
 * the clean test worked. thank you. No need for 10C you already gave it to me
 * :D
 * 
 * Glad that did it. And you're right about 10C: you already have it, and 10D
 * too. Your 14 tests (13 repository tests plus contextLoads) are exactly the
 * number I predicted for that stage, so 10B is done.
 * 
 * That result also supports the target/ theory: the same code went from an
 * error to green just by rebuilding from scratch. If it ever happens again
 * after moving files around or fixing imports, run ./mvnw clean test with VS
 * Code idle, before digging into anything else.
 * 
 * You haven't run the other two blocks yet, so the next step is the full suite
 * from a clean state:
 * 
 * ./mvnw clean test
 * 
 * The expected total is 113 tests: 13 repository, 20 service, 79 controller,
 * plus contextLoads. I wrote the 10C and 10D tests without running them against
 * your project, so if any fail, send me the first error line (the Caused by
 * part is usually the useful one) and we'll sort it out. If you want to isolate
 * a failing block, run -Dtest="TutorialServiceImplTest,TagServiceImplTest" for
 * 10C or the three *ControllerTest classes for 10D.
 * 
 * When it's all green, the checklist at the end of my last message covers what
 * to review before handing it in.
 */