package com.example.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.entities.Tutorial;
import com.example.exception.ResourceNotFoundException;

class TutorialControllerTest extends AbstractControllerTest {

    private Tutorial springTutorial;

    private Tutorial hibernateTutorial;

    @BeforeEach
    void setUp() {

        springTutorial = Tutorial.builder()
                .id(1L)
                .title("Spring Boot basics")
                .description("Intro to Spring Boot")
                .published(true)
                .build();

        hibernateTutorial = Tutorial.builder()
                .id(2L)
                .title("Hibernate in depth")
                .description("JPA and Hibernate")
                .published(true)
                .build();
    }

    // ================= Access control matrix (requirement 1) =================

    @ParameterizedTest(name = "{0} {1} without token -> 401")
    @CsvSource({
            "GET,    /api/tutorials",
            "GET,    /api/tutorials/1",
            "GET,    /api/tutorials/published",
            "POST,   /api/tutorials",
            "PUT,    /api/tutorials/1",
            "DELETE, /api/tutorials/1",
            "DELETE, /api/tutorials"
    })
    void testWithoutTokenIsUnauthorized(String method, String url) throws Exception {

        perform(method, url, null)
                .andExpect(status().isUnauthorized());

        then(tutorialService).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "USER {0} {1} -> allowed")
    @CsvSource({
            "GET, /api/tutorials",
            "GET, /api/tutorials/1",
            "GET, /api/tutorials/published"
    })
    void testUserCanRead(String method, String url) throws Exception {

        perform(method, url, userToken)
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest(name = "USER {0} {1} -> 403")
    @CsvSource({
            "POST,   /api/tutorials",
            "PUT,    /api/tutorials/1",
            "DELETE, /api/tutorials/1",
            "DELETE, /api/tutorials"
    })
    void testUserCannotWrite(String method, String url) throws Exception {

        perform(method, url, userToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode", is(403)))
                .andExpect(jsonPath("$.message", is("You do not have permission to perform this action")));

        // The controller method never ran
        then(tutorialService).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "ADMIN {0} {1} -> allowed")
    @CsvSource({
            "GET,    /api/tutorials",
            "GET,    /api/tutorials/1",
            "GET,    /api/tutorials/published",
            "POST,   /api/tutorials",
            "PUT,    /api/tutorials/1",
            "DELETE, /api/tutorials/1",
            "DELETE, /api/tutorials"
    })
    void testAdminCanDoEverything(String method, String url) throws Exception {

        perform(method, url, adminToken)
                .andExpect(status().is2xxSuccessful());
    }

    // ================= Behavior of each endpoint =================

    @Test
    @DisplayName("GET /api/tutorials returns the list")
    void testGetAllTutorials() throws Exception {

        // given
        given(tutorialService.findAll(null)).willReturn(List.of(springTutorial, hibernateTutorial));

        // when and then
        perform("GET", "/api/tutorials", userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Spring Boot basics")))
                .andExpect(jsonPath("$[1].title", is("Hibernate in depth")));
    }

    @Test
    @DisplayName("GET /api/tutorials?title=... passes the title to the service")
    void testGetAllTutorialsFilteredByTitle() throws Exception {

        // given: only the call with the title "Spring" has an answer
        given(tutorialService.findAll("Spring")).willReturn(List.of(springTutorial));

        // when and then
        perform("GET", "/api/tutorials?title=Spring", userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Spring Boot basics")));
    }

    @Test
    @DisplayName("GET /api/tutorials answers 204 when there are no tutorials")
    void testGetAllTutorialsEmpty() throws Exception {

        // given
        given(tutorialService.findAll(null)).willReturn(List.of());

        // when and then
        perform("GET", "/api/tutorials", userToken)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/tutorials/published returns the published tutorials")
    void testFindByPublished() throws Exception {

        // given
        given(tutorialService.findByPublished(true)).willReturn(List.of(springTutorial));

        // when and then
        perform("GET", "/api/tutorials/published", userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/tutorials/{id} returns the tutorial")
    void testGetTutorialById() throws Exception {

        // given
        given(tutorialService.findById(1L)).willReturn(springTutorial);

        // when and then
        perform("GET", "/api/tutorials/1", userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Spring Boot basics")))
                .andExpect(jsonPath("$.published", is(true)));
    }

    @Test
    @DisplayName("GET /api/tutorials/{id} answers 404 with the ErrorMessage JSON when the service says not found")
    void testGetTutorialByIdNotFound() throws Exception {

        // given
        given(tutorialService.findById(99L))
                .willThrow(new ResourceNotFoundException("Not found Tutorial with id = 99"));

        // when and then
        perform("GET", "/api/tutorials/99", userToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode", is(404)))
                .andExpect(jsonPath("$.message", is("Not found Tutorial with id = 99")));
    }

    @Test
    @DisplayName("POST /api/tutorials as ADMIN creates the tutorial and answers 201")
    void testCreateTutorial() throws Exception {

        // given
        given(tutorialService.create(any(Tutorial.class))).willReturn(springTutorial);

        String json = """
                {"title": "Spring Boot basics", "description": "Intro to Spring Boot"}
                """;

        // when and then
        perform("POST", "/api/tutorials", adminToken, json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Spring Boot basics")));

        // The JSON of the request really reached the service as a Tutorial
        then(tutorialService).should()
                .create(argThat(tutorial -> "Spring Boot basics".equals(tutorial.getTitle())));
    }

    @Test
    @DisplayName("POST /api/tutorials with malformed JSON answers 400")
    void testCreateTutorialWithMalformedJson() throws Exception {

        // when and then: the JSON is cut off, so it cannot even be read
        perform("POST", "/api/tutorials", adminToken, "{\"title\": \"Broken\",")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));

        then(tutorialService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("PUT /api/tutorials/{id} as ADMIN updates the tutorial")
    void testUpdateTutorial() throws Exception {

        // given
        Tutorial updated = Tutorial.builder()
                .id(1L)
                .title("Spring Boot updated")
                .description("New description")
                .published(true)
                .build();

        given(tutorialService.update(eq(1L), any(Tutorial.class))).willReturn(updated);

        String json = """
                {"title": "Spring Boot updated", "description": "New description", "published": true}
                """;

        // when and then
        perform("PUT", "/api/tutorials/1", adminToken, json)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Spring Boot updated")));
    }

    @Test
    @DisplayName("PUT /api/tutorials/{id} answers 404 when the tutorial does not exist")
    void testUpdateTutorialNotFound() throws Exception {

        // given
        given(tutorialService.update(eq(99L), any(Tutorial.class)))
                .willThrow(new ResourceNotFoundException("Not found Tutorial with id = 99"));

        // when and then
        perform("PUT", "/api/tutorials/99", adminToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not found Tutorial with id = 99")));
    }

    @Test
    @DisplayName("DELETE /api/tutorials/{id} as ADMIN answers 204 and calls the service")
    void testDeleteTutorial() throws Exception {

        // when and then
        perform("DELETE", "/api/tutorials/1", adminToken)
                .andExpect(status().isNoContent());

        then(tutorialService).should().deleteById(1L);
    }

    @Test
    @DisplayName("DELETE /api/tutorials as ADMIN answers 204 and deletes everything")
    void testDeleteAllTutorials() throws Exception {

        // when and then
        perform("DELETE", "/api/tutorials", adminToken)
                .andExpect(status().isNoContent());

        then(tutorialService).should().deleteAll();
    }
}

/*
 * Explanation
 * The four parameterized tests are the whole role matrix of TutorialController.
 * In testUserCannotWrite, the extra shouldHaveNoInteractions() is the important
 * part: it proves the method body never ran, so a forbidden request can't have
 * changed anything.
 * The ADMIN and USER read matrices use is2xxSuccessful() because the mocked
 * service returns defaults (empty lists, null), which makes the controller
 * answer 200 or 204 depending on the endpoint. What matters in the matrix is
 * allowed or not. The exact responses are checked in the behavior tests below.
 * testGetAllTutorialsFilteredByTitle is a "negative-proof" test: only the call
 * with "Spring" has a stub. If the controller forgot to pass the parameter, the
 * mock would answer with an empty list, the status would be 204, and the test
 * would fail.
 * testGetTutorialByIdNotFound checks the integration of three things: the
 * service throws, the controller lets it go, and the real
 * ControllerExceptionHandler turns it into a 404 with your ErrorMessage JSON.
 * argThat(...) verifies that the JSON of the request was bound into a Tutorial
 * with the right title before reaching the service. In the same verification
 * you cannot mix argThat and raw values, which is why the update stub uses
 * eq(1L) together with any(...).
 * testCreateTutorialWithMalformedJson covers the "mal formado" part of
 * requirement 3 for a controller other than AuthController.
 */
