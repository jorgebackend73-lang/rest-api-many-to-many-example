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

import com.example.entities.Tag;
import com.example.entities.Tutorial;
import com.example.exception.ResourceNotFoundException;

class TagControllerTest extends AbstractControllerTest {

    private Tag javaTag;

    private Tag springTag;

    @BeforeEach
    void setUp() {

        javaTag = Tag.builder().id(5L).name("java").build();

        springTag = Tag.builder().id(6L).name("spring").build();
    }

    // ================= Access control matrix (requirement 1) =================

    @ParameterizedTest(name = "{0} {1} without token -> 401")
    @CsvSource({
            "GET,    /api/tags",
            "GET,    /api/tutorials/1/tags",
            "GET,    /api/tags/1/tutorials",
            "POST,   /api/tutorials/1/tags",
            "PUT,    /api/tags/1",
            "DELETE, /api/tutorials/1/tags/5",
            "DELETE, /api/tags/1"
    })
    void testWithoutTokenIsUnauthorized(String method, String url) throws Exception {

        perform(method, url, null)
                .andExpect(status().isUnauthorized());

        then(tagService).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "USER {0} {1} -> allowed")
    @CsvSource({
            "GET, /api/tags",
            "GET, /api/tutorials/1/tags",
            "GET, /api/tags/1/tutorials"
    })
    void testUserCanRead(String method, String url) throws Exception {

        perform(method, url, userToken)
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest(name = "USER {0} {1} -> 403")
    @CsvSource({
            "POST,   /api/tutorials/1/tags",
            "PUT,    /api/tags/1",
            "DELETE, /api/tutorials/1/tags/5",
            "DELETE, /api/tags/1"
    })
    void testUserCannotWrite(String method, String url) throws Exception {

        perform(method, url, userToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode", is(403)));

        then(tagService).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "ADMIN {0} {1} -> allowed")
    @CsvSource({
            "GET,    /api/tags",
            "GET,    /api/tutorials/1/tags",
            "GET,    /api/tags/1/tutorials",
            "POST,   /api/tutorials/1/tags",
            "PUT,    /api/tags/1",
            "DELETE, /api/tutorials/1/tags/5",
            "DELETE, /api/tags/1"
    })
    void testAdminCanDoEverything(String method, String url) throws Exception {

        perform(method, url, adminToken)
                .andExpect(status().is2xxSuccessful());
    }

    // ================= Behavior of each endpoint =================

    @Test
    @DisplayName("GET /api/tags returns the list of tags")
    void testGetAllTags() throws Exception {

        // given
        given(tagService.findAll()).willReturn(List.of(javaTag, springTag));

        // when and then
        perform("GET", "/api/tags", userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("java")))
                .andExpect(jsonPath("$[1].name", is("spring")));
    }

    @Test
    @DisplayName("GET /api/tags answers 204 when there are no tags")
    void testGetAllTagsEmpty() throws Exception {

        // given
        given(tagService.findAll()).willReturn(List.of());

        // when and then
        perform("GET", "/api/tags", userToken)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/tutorials/{id}/tags answers 404 when the tutorial does not exist")
    void testGetTagsByTutorialIdNotFound() throws Exception {

        // given
        given(tagService.findByTutorialId(99L))
                .willThrow(new ResourceNotFoundException("Not found Tutorial with id = 99"));

        // when and then
        perform("GET", "/api/tutorials/99/tags", userToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not found Tutorial with id = 99")));
    }

    @Test
    @DisplayName("GET /api/tags/{id}/tutorials returns the tutorials of the tag")
    void testGetTutorialsByTagId() throws Exception {

        // given
        Tutorial tutorial = Tutorial.builder()
                .id(1L)
                .title("Spring Boot basics")
                .description("Intro to Spring Boot")
                .published(true)
                .build();

        given(tagService.findTutorialsByTagId(5L)).willReturn(List.of(tutorial));

        // when and then
        perform("GET", "/api/tags/5/tutorials", userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Spring Boot basics")));
    }

    @Test
    @DisplayName("POST /api/tutorials/{id}/tags as ADMIN creates a new tag and answers 201")
    void testAddNewTag() throws Exception {

        // given
        given(tagService.addTagToTutorial(eq(1L), any(Tag.class))).willReturn(javaTag);

        // when and then
        perform("POST", "/api/tutorials/1/tags", adminToken, "{\"name\": \"java\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("java")));

        then(tagService).should()
                .addTagToTutorial(eq(1L), argThat(tag -> "java".equals(tag.getName())));
    }

    @Test
    @DisplayName("POST /api/tutorials/{id}/tags with the id of an existing tag passes that id to the service")
    void testAddExistingTag() throws Exception {

        // given
        given(tagService.addTagToTutorial(eq(1L), any(Tag.class))).willReturn(javaTag);

        // when and then
        perform("POST", "/api/tutorials/1/tags", adminToken, "{\"id\": 5}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(5)));

        then(tagService).should()
                .addTagToTutorial(eq(1L), argThat(tag -> tag.getId() == 5L));
    }

    @Test
    @DisplayName("POST /api/tutorials/{id}/tags answers 404 when the tutorial does not exist")
    void testAddTagTutorialNotFound() throws Exception {

        // given
        given(tagService.addTagToTutorial(eq(99L), any(Tag.class)))
                .willThrow(new ResourceNotFoundException("Not found Tutorial with id = 99"));

        // when and then
        perform("POST", "/api/tutorials/99/tags", adminToken, "{\"name\": \"java\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not found Tutorial with id = 99")));
    }

    @Test
    @DisplayName("PUT /api/tags/{id} as ADMIN updates the tag")
    void testUpdateTag() throws Exception {

        // given
        Tag updated = Tag.builder().id(5L).name("java21").build();

        given(tagService.update(eq(5L), any(Tag.class))).willReturn(updated);

        // when and then
        perform("PUT", "/api/tags/5", adminToken, "{\"name\": \"java21\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("java21")));
    }

    @Test
    @DisplayName("PUT /api/tags/{id} answers 404 when the tag does not exist")
    void testUpdateTagNotFound() throws Exception {

        // given
        given(tagService.update(eq(99L), any(Tag.class)))
                .willThrow(new ResourceNotFoundException("Not found Tag with id = 99"));

        // when and then
        perform("PUT", "/api/tags/99", adminToken, "{\"name\": \"x\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Not found Tag with id = 99")));
    }

    @Test
    @DisplayName("DELETE /api/tutorials/{tutorialId}/tags/{tagId} as ADMIN unlinks the tag and answers 204")
    void testRemoveTagFromTutorial() throws Exception {

        // when and then
        perform("DELETE", "/api/tutorials/1/tags/5", adminToken)
                .andExpect(status().isNoContent());

        then(tagService).should().removeTagFromTutorial(1L, 5L);
    }

    @Test
    @DisplayName("DELETE /api/tags/{id} as ADMIN answers 204 and calls the service")
    void testDeleteTag() throws Exception {

        // when and then
        perform("DELETE", "/api/tags/5", adminToken)
                .andExpect(status().isNoContent());

        then(tagService).should().deleteById(5L);
    }
}

/*
 * Explanation
 * It follows the same structure as TutorialControllerTest, so you can read it
 * by comparison.
 * testAddNewTag vs testAddExistingTag check how the JSON reaches the service.
 * {"name": "java"} arrives with id = 0 ("new tag"), and {"id": 5} arrives with
 * id = 5 ("link existing"). That 0 versus non-zero difference is exactly what
 * TagServiceImpl decides on, so the controller must not lose it on the way.
 * jsonPath("$[0].title") on the tutorials list works because the mocked
 * tutorials are plain objects, with no database or lazy collections involved.
 */
