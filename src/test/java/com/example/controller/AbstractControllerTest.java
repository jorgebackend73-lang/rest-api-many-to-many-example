package com.example.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.service.TagService;
import com.example.service.TutorialService;
import com.example.spring_security_jwt.model.ERole;
import com.example.spring_security_jwt.model.Role;
import com.example.spring_security_jwt.model.User;
import com.example.spring_security_jwt.repository.RoleRepository;
import com.example.spring_security_jwt.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.NONE)
abstract class AbstractControllerTest {

    protected static final String ADMIN_EMAIL = "test-admin@example.com";

    protected static final String USER_EMAIL = "test-user@example.com";

    protected static final String PASSWORD = "Test2026$$##";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // The service layer is replaced by mocks: these tests are about HTTP and
    // security
    @MockitoBean
    protected TutorialService tutorialService;

    @MockitoBean
    protected TagService tagService;

    protected String adminToken;

    protected String userToken;

    @BeforeEach
    void prepareUsersAndTokens() throws Exception {

        createUserIfMissing("test-admin", ADMIN_EMAIL, ERole.ROLE_ADMIN);
        createUserIfMissing("test-user", USER_EMAIL, ERole.ROLE_USER);

        adminToken = login(ADMIN_EMAIL);
        userToken = login(USER_EMAIL);
    }

    // Sends a request with an empty JSON body ({}) and, if token is not null, the
    // Authorization header
    protected ResultActions perform(String method, String url, String token) throws Exception {

        return perform(method, url, token, "{}");
    }

    protected ResultActions perform(String method, String url, String token, String jsonBody)
            throws Exception {

        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody);

        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        return mockMvc.perform(builder);
    }

    // The tests create their own users instead of relying on the sample data of
    // CreatesSamplesData
    private void createUserIfMissing(String username, String email, ERole roleName) {

        if (userRepository.existsByEmail(email)) {
            return;
        }

        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));

        userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .roles(Set.of(role))
                .build());
    }

    // A real login against /api/auth/signin, exactly as Postman does it
    private String login(String email) throws Exception {

        String json = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, PASSWORD);

        String response = perform("POST", "/api/auth/signin", null, json)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.token");
    }
}

/*
 * 10D is requirement 6, and it ties together everything you've built: real JWT
 * tokens, the real security chain and the real ControllerExceptionHandler, with
 * only the service layer replaced by mocks. Put every file under
 * src/test/java/com/example/controller/ (the test folder, as in the last two
 * parts).
 * 
 * What is real and what is fake
 * Piece In these tests
 * SecurityFilterChain, AuthTokenFilter, JwtUtils, @PreAuthorize Real
 * AuthController, login, BCrypt, users and roles in MySQL Real
 * ControllerExceptionHandler (400, 401, 403, 404) Real
 * TutorialController, TagController Real
 * TutorialService, TagService Mocks (@MockitoBean), so the tests don't depend
 * on stored tutorials
 * 
 * This is why they're integration tests: several real layers working together.
 * The example's ProductControllerTest follows the same recipe.
 * 
 * Key concepts
 * 
 * @SpringBootTest loads the complete application context. The @WebMvcTest slice
 * is not enough here, because it doesn't load your security configuration (the
 * example's comments say the same).
 * 
 * @AutoConfigureMockMvc gives you a MockMvc: it simulates HTTP requests
 * in-process, with no real server or port, but they cross the real filter
 * chain, so JWT and @PreAuthorize apply. Its package changed in Spring Boot 4:
 * org.springframework.boot.webmvc.test.autoconfigure.
 * 
 * @MockitoBean replaces a bean in the context with a Mockito mock. It's the
 * Boot 4 replacement for @MockBean, which no longer exists, so tutorials
 * written for Boot 3 won't compile. The mocks are reset after every test
 * automatically.
 * Real tokens. Each test logs in through /api/auth/signin (like the
 * example's @BeforeEach) and sends Authorization: Bearer <token>. It's exactly
 * what you did by hand in Postman.
 * A parameterized role matrix. Instead of writing 28 near-identical
 * tests, @ParameterizedTest + @CsvSource runs one test body once per method,url
 * row. It automates the table from step 9.
 * A trap in that matrix: Spring reads the request body before
 * checking @PreAuthorize. A USER calling POST with no body would get 400, not
 * 403. That's why every request in the matrix carries a body ({}).
 * Context caching. Spring caches a context and reuses it for every test class
 * with the same configuration, including the same @MockitoBean set. If each
 * test class declared its own mocks, you'd pay a full application startup per
 * class. So the shared setup lives in an abstract base class, and all three
 * test classes share one context.
 * Prerequisites
 * 
 * MySQL running and the application stopped: the test context uses the same
 * schema and, with create-drop, it recreates it.
 */

/*
 * Explanation
 * abstract. It holds the shared setup and is never run by itself. Its name
 * doesn't end in a way that makes the test runner pick it up as a test of its
 * own, and abstract classes are skipped anyway.
 * 
 * @AutoConfigureTestDatabase(replace = NONE) is copied from the example: it
 * states explicitly that the real MySQL is used.
 * 
 * @MockitoBean in the base class. Subclasses inherit the two mocks, and this is
 * what keeps the context shared and cached.
 * createUserIfMissing: why the tests don't use admin1 and user1. Other test
 * classes (@DataJpaTest, contextLoads) start their own contexts, and with
 * create-drop each of them recreates the schema. If that happens while this
 * context is cached, the users created by CreatesSamplesData would silently
 * disappear, and your controller tests would fail depending on the order the
 * classes run. By creating their own users when missing (a handful of cheap
 * queries per test), the tests work in any order. They only reuse the roles
 * that exist, or create them if the schema was recreated.
 * login uses the real endpoint, so a bug in signin would show up here first.
 * JsonPath.read(response, "$.token") extracts the token without needing a JSON
 * library (JsonPath comes with spring-boot-starter-test). The text block
 * """...""" is a Java multi-line string, and .formatted(...) fills the %s.
 * perform builds the request. HttpMethod.valueOf("GET") turns the CSV text into
 * a method, and the body defaults to {} for the reason explained above.
 * The @BeforeEach method name is unique on purpose. If a subclass declared a
 * method with the same name, it would override this one and the login would
 * never run. Subclasses use setUp and the base uses prepareUsersAndTokens.
 */