package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.entities.Tag;
import com.example.entities.Tutorial;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.TagRepository;
import com.example.repository.TutorialRepository;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TutorialRepository tutorialRepository;

    @InjectMocks
    private TagServiceImpl tagServiceImpl;

    private Tutorial tutorial;

    @BeforeEach
    void setUp() {

        tutorial = Tutorial.builder()
                .id(1L)
                .title("Spring Boot basics")
                .description("Intro to Spring Boot")
                .published(true)
                .build();
    }

    // ---------- findByTutorialId ----------

    @Test
    @DisplayName("findByTutorialId returns the tags of an existing tutorial")
    void testFindByTutorialId() {

        // given
        Tag javaTag = Tag.builder().id(5L).name("java").build();

        given(tutorialRepository.existsById(1L)).willReturn(true);
        given(tagRepository.findTagsByTutorialsId(1L)).willReturn(List.of(javaTag));

        // when
        List<Tag> result = tagServiceImpl.findByTutorialId(1L);

        // then
        assertThat(result).containsExactly(javaTag);
    }

    @Test
    @DisplayName("findByTutorialId throws ResourceNotFoundException when the tutorial does not exist")
    void testFindByTutorialIdNotFound() {

        // given
        given(tutorialRepository.existsById(7L)).willReturn(false);

        // when and then
        assertThatThrownBy(() -> tagServiceImpl.findByTutorialId(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not found Tutorial with id = 7");

        then(tagRepository).shouldHaveNoInteractions();
    }

    // ---------- findTutorialsByTagId ----------

    @Test
    @DisplayName("findTutorialsByTagId returns the tutorials of an existing tag")
    void testFindTutorialsByTagId() {

        // given
        given(tagRepository.existsById(5L)).willReturn(true);
        given(tutorialRepository.findTutorialsByTagsId(5L)).willReturn(List.of(tutorial));

        // when
        List<Tutorial> result = tagServiceImpl.findTutorialsByTagId(5L);

        // then
        assertThat(result).containsExactly(tutorial);
    }

    @Test
    @DisplayName("findTutorialsByTagId throws ResourceNotFoundException when the tag does not exist")
    void testFindTutorialsByTagIdNotFound() {

        // given
        given(tagRepository.existsById(9L)).willReturn(false);

        // when and then
        assertThatThrownBy(() -> tagServiceImpl.findTutorialsByTagId(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not found Tag with id = 9");

        then(tutorialRepository).shouldHaveNoInteractions();
    }

    // ---------- addTagToTutorial ----------

    @Test
    @DisplayName("addTagToTutorial throws ResourceNotFoundException when the tutorial does not exist")
    void testAddTagTutorialNotFound() {

        // given
        given(tutorialRepository.findById(99L)).willReturn(Optional.empty());

        Tag request = Tag.builder().name("java").build();

        // when and then
        assertThatThrownBy(() -> tagServiceImpl.addTagToTutorial(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not found Tutorial with id = 99");

        then(tagRepository).shouldHaveNoInteractions();
        then(tutorialRepository).should(never()).save(any(Tutorial.class));
    }

    @Test
    @DisplayName("addTagToTutorial links an existing tag (request carries its id) to the tutorial")
    void testAddExistingTag() {

        // given: the request only carries the id of a tag that already exists
        Tag existingTag = Tag.builder().id(5L).name("java").build();
        Tag request = Tag.builder().id(5L).build();

        given(tutorialRepository.findById(1L)).willReturn(Optional.of(tutorial));
        given(tagRepository.findById(5L)).willReturn(Optional.of(existingTag));

        // when
        Tag result = tagServiceImpl.addTagToTutorial(1L, request);

        // then: the stored tag is returned and both sides of the relation are linked
        assertThat(result).isSameAs(existingTag);
        assertThat(tutorial.getTags()).containsOnly(existingTag);
        assertThat(existingTag.getTutorials()).containsOnly(tutorial);

        then(tutorialRepository).should().save(tutorial);
        then(tagRepository).should(never()).save(any(Tag.class));
    }

    @Test
    @DisplayName("addTagToTutorial throws ResourceNotFoundException when the tag id does not exist")
    void testAddExistingTagNotFound() {

        // given
        Tag request = Tag.builder().id(5L).build();

        given(tutorialRepository.findById(1L)).willReturn(Optional.of(tutorial));
        given(tagRepository.findById(5L)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> tagServiceImpl.addTagToTutorial(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not found Tag with id = 5");

        then(tutorialRepository).should(never()).save(any(Tutorial.class));
    }

    @Test
    @DisplayName("addTagToTutorial creates a new tag (no id in the request) and links it to the tutorial")
    void testAddNewTag() {

        // given: id 0 means "new tag"
        Tag newTag = Tag.builder().name("spring").build();
        Tag savedTag = Tag.builder().id(10L).name("spring").build();

        given(tutorialRepository.findById(1L)).willReturn(Optional.of(tutorial));
        given(tagRepository.save(newTag)).willReturn(savedTag);

        // when
        Tag result = tagServiceImpl.addTagToTutorial(1L, newTag);

        // then
        assertThat(result).isSameAs(savedTag);
        assertThat(tutorial.getTags()).containsOnly(newTag);

        then(tagRepository).should().save(newTag);
        then(tutorialRepository).should(never()).save(any(Tutorial.class));
    }

    // ---------- update ----------

    @Test
    @DisplayName("update changes the name of an existing tag")
    void testUpdate() {

        // given
        Tag existingTag = Tag.builder().id(5L).name("java").build();
        Tag changes = Tag.builder().name("java21").build();

        given(tagRepository.findById(5L)).willReturn(Optional.of(existingTag));
        given(tagRepository.save(existingTag)).willReturn(existingTag);

        // when
        Tag result = tagServiceImpl.update(5L, changes);

        // then
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getName()).isEqualTo("java21");
        then(tagRepository).should().save(existingTag);
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException and saves nothing when the tag does not exist")
    void testUpdateNotFound() {

        // given
        given(tagRepository.findById(99L)).willReturn(Optional.empty());

        Tag changes = Tag.builder().name("java21").build();

        // when and then
        assertThatThrownBy(() -> tagServiceImpl.update(99L, changes))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not found Tag with id = 99");

        then(tagRepository).should(never()).save(any(Tag.class));
    }

    // ---------- removeTagFromTutorial ----------

    @Test
    @DisplayName("removeTagFromTutorial unlinks the tag from both sides and saves the tutorial")
    void testRemoveTagFromTutorial() {

        // given: the tag is already linked to the tutorial
        Tag javaTag = Tag.builder().id(5L).name("java").build();
        tutorial.addTag(javaTag);

        given(tutorialRepository.findById(1L)).willReturn(Optional.of(tutorial));

        // when
        tagServiceImpl.removeTagFromTutorial(1L, 5L);

        // then
        assertThat(tutorial.getTags()).isEmpty();
        assertThat(javaTag.getTutorials()).isEmpty();
        then(tutorialRepository).should().save(tutorial);
    }

    @Test
    @DisplayName("removeTagFromTutorial throws ResourceNotFoundException when the tutorial does not exist")
    void testRemoveTagTutorialNotFound() {

        // given
        given(tutorialRepository.findById(99L)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> tagServiceImpl.removeTagFromTutorial(99L, 5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not found Tutorial with id = 99");

        then(tutorialRepository).should(never()).save(any(Tutorial.class));
    }
}

/*
 * Explanation
 * Two @Mocks, one @InjectMocks. TagServiceImpl has a constructor with both
 * repositories, and Mockito matches each mock to its parameter by type.
 * A fresh tutorial before every test (@BeforeEach). Some tests modify it
 * (linking tags), so sharing one instance across tests would make them depend
 * on each other.
 * shouldHaveNoInteractions() proves the guard clause works: when the tutorial
 * doesn't exist, the service must stop before touching the other repository.
 * It's the unit-level version of "the 404 happens first".
 * The four addTagToTutorial tests map one-to-one to the branches of the method
 * in TagServiceImpl: tutorial missing, existing tag found, existing tag not
 * found, new tag. This is the real business logic of the layer, and now each
 * path has a test.
 * Linking is checked on both sides. containsOnly(existingTag) on the tutorial
 * and containsOnly(tutorial) on the tag prove that addTag keeps the
 * bidirectional relation consistent. containsOnly fits because a Set has no
 * guaranteed order.
 * Mocks don't track identity automatically. given(tagRepository.save(newTag))
 * matches because your entities have no equals, so the match is by the same
 * object. That's why the test passes the very same newTag instance to the
 * service.
 * Which assertions depend on @ToString.Exclude. None of them call toString, but
 * if one failed, AssertJ would print the entities. Thanks to the fix from 10B,
 * you'd see a readable message and not a StackOverflowError.
 * Running the tests
 * 
 * Make sure both files are under src/test/java/com/example/service/, then:
 * 
 * ./mvnw test -Dtest="TutorialServiceImplTest,TagServiceImplTest"
 * 
 * Expected result: 8 + 12 = 20 tests, no failures, and it finishes in a couple
 * of seconds, with no SQL in the console because there's no database involved.
 * 
 * Two sanity checks worth doing once (undo each afterward):
 * 
 * In TutorialServiceImpl.create, change .published(true) to
 * .published(tutorial.isPublished()).
 * testCreateForcesPublishedAndIgnoresRequestId must fail.
 * In TagServiceImpl.findByTutorialId, delete the existsById check.
 * testFindByTutorialIdNotFound must fail.
 * 
 * If a test fails or Mockito complains (for example
 * UnnecessaryStubbingException or PotentialStubbingProblem), send me the
 * message: those two errors mean the stubbed arguments don't match what the
 * service really calls.
 * 
 * When the 20 tests are green, we move to 10D, the last block: integration
 * tests for the controllers with Spring Security and JWT (requirement 6). It's
 * where everything we've built comes together: a real login to get a token,
 * then the 401, 403 and success cases for each endpoint.
 */
