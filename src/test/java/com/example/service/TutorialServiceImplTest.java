package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.entities.Tutorial;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.TutorialRepository;

@ExtendWith(MockitoExtension.class)
class TutorialServiceImplTest {

    @Mock
    private TutorialRepository tutorialRepository;

    @InjectMocks
    private TutorialServiceImpl tutorialServiceImpl;

    @Captor
    private ArgumentCaptor<Tutorial> tutorialCaptor;

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
                .published(false)
                .build();
    }

    @Test
    @DisplayName("findAll without title returns all the tutorials")
    void testFindAllWithoutTitle() {

        // given
        given(tutorialRepository.findAll()).willReturn(List.of(springTutorial, hibernateTutorial));

        // when
        List<Tutorial> result = tutorialServiceImpl.findAll(null);

        // then
        assertThat(result).containsExactly(springTutorial, hibernateTutorial);
        then(tutorialRepository).should(never()).findByTitleContaining(anyString());
    }

    @Test
    @DisplayName("findAll with a title searches by title and does not load everything")
    void testFindAllWithTitle() {

        // given
        given(tutorialRepository.findByTitleContaining("Spring")).willReturn(List.of(springTutorial));

        // when
        List<Tutorial> result = tutorialServiceImpl.findAll("Spring");

        // then
        assertThat(result).containsExactly(springTutorial);
        then(tutorialRepository).should(never()).findAll();
    }

    @Test
    @DisplayName("findById returns the tutorial when it exists")
    void testFindById() {

        // given
        given(tutorialRepository.findById(1L)).willReturn(Optional.of(springTutorial));

        // when
        Tutorial result = tutorialServiceImpl.findById(1L);

        // then
        assertThat(result).isSameAs(springTutorial);
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the tutorial does not exist")
    void testFindByIdNotFound() {

        // given
        given(tutorialRepository.findById(99L)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> tutorialServiceImpl.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not found Tutorial with id = 99");
    }

    @Test
    @DisplayName("create always saves the tutorial as published and ignores the id sent by the client")
    void testCreateForcesPublishedAndIgnoresRequestId() {

        // given: a request that tries to choose its id and to be unpublished
        Tutorial request = Tutorial.builder()
                .id(55L)
                .title("New tutorial")
                .description("New description")
                .published(false)
                .build();

        given(tutorialRepository.save(any(Tutorial.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Tutorial result = tutorialServiceImpl.create(request);

        // then: look at what was really passed to the repository
        then(tutorialRepository).should().save(tutorialCaptor.capture());
        Tutorial saved = tutorialCaptor.getValue();

        assertThat(saved.getTitle()).isEqualTo("New tutorial");
        assertThat(saved.getDescription()).isEqualTo("New description");
        assertThat(saved.isPublished()).isTrue();
        assertThat(saved.getId()).isNotEqualTo(55L);
        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("update changes title, description and published of an existing tutorial")
    void testUpdate() {

        // given
        Tutorial changes = Tutorial.builder()
                .title("Hibernate updated")
                .description("Updated description")
                .published(true)
                .build();

        given(tutorialRepository.findById(2L)).willReturn(Optional.of(hibernateTutorial));
        given(tutorialRepository.save(hibernateTutorial)).willReturn(hibernateTutorial);

        // when
        Tutorial result = tutorialServiceImpl.update(2L, changes);

        // then
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getTitle()).isEqualTo("Hibernate updated");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.isPublished()).isTrue();
        then(tutorialRepository).should().save(hibernateTutorial);
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException and saves nothing when the tutorial does not exist")
    void testUpdateNotFound() {

        // given
        given(tutorialRepository.findById(99L)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> tutorialServiceImpl.update(99L, springTutorial))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not found Tutorial with id = 99");

        then(tutorialRepository).should(never()).save(any(Tutorial.class));
    }

    @Test
    @DisplayName("deleteById asks the repository to delete that id")
    void testDeleteById() {

        // when
        tutorialServiceImpl.deleteById(1L);

        // then
        then(tutorialRepository).should().deleteById(1L);
    }
}

/*
 * Good catch on the folder. 10C is about requirement 5: unit tests of
 * TutorialServiceImpl and TagServiceImpl, using Mockito.
 * 
 * Key concepts
 * 
 * This is the first true unit test. No Spring, no database, no MySQL:
 * 
 * Test type What runs Speed
 * 10B, @DataJpaTest Spring's persistence slice + MySQL seconds
 * 10C, Mockito Only your service class milliseconds
 * 10D, @SpringBootTest Everything slow
 * 
 * So for this step you don't need MySQL running, and the app can stay on.
 * 
 * Mocks. A mock is a fake object that Mockito creates, with the same methods as
 * the real one, but which only does what you tell it to. Here the repositories
 * are mocked, so the test reads:
 * "given the repository says X, when the service runs, then it must do Y". That
 * isolates the logic we wrote in 10A. The repositories' own behavior was
 * already tested in 10B.
 * 
 * The Mockito tools used below:
 * 
 * Tool What it does
 * 
 * @ExtendWith(MockitoExtension.class) Activates Mockito for the test class
 * (JUnit 5 extension)
 * 
 * @Mock Creates a fake of that type
 * 
 * @InjectMocks Creates the real class under test and injects the mocks into its
 * constructor (the one Lombok generated)
 * given(...).willReturn(...) Stubbing:
 * "when this method is called with these arguments, return this"
 * then(mock).should().method(...) Verification: "this call must have happened"
 * should(never()) "This call must NOT have happened"
 * 
 * @Captor / ArgumentCaptor Grabs the object that was passed to a mocked method,
 * so you can inspect it
 * 
 * Unstubbed calls return harmless defaults: empty lists, Optional.empty(),
 * false, null.
 * 
 * Strict stubs. MockitoExtension fails a test with UnnecessaryStubbingException
 * if you stub something the code never calls. It's useful: it exposes setup
 * that has become dead.
 * 
 * A mistake in the example's service test
 * 
 * In ProductServiceImplTest, one test does this:
 * 
 * java
 * when(productServiceImpl.findAll()).thenReturn(productsList);
 * 
 * productServiceImpl is the real object under test, not a mock. It works only
 * by accident: when() doesn't look at its argument, it grabs the last call made
 * on any mock, which happens to be the productDao.findAll() executed inside the
 * service. The correct rule is: stub the mocks (the collaborators), then call
 * the object under test.
 * 
 * java
 * given(productDao.findAll()).willReturn(productsList); // stub the mock
 * List<Product> result = productServiceImpl.findAll(); // call the real object
 * 
 * Our tests follow the correct pattern.
 * 
 * All files go under src/test/java/com/example/service/
 */

/*
 * Explanation
 * 
 * @InjectMocks builds a real TutorialServiceImpl, calling the constructor
 * that @RequiredArgsConstructor generated and passing the @Mock repository to
 * it. That's why the field in the service is final and constructor-injected: it
 * makes it trivial to test.
 * testFindAllWithoutTitle and testFindAllWithTitle check the branching rule of
 * findAll, and the should(never()) line proves the other branch wasn't taken.
 * assertThatThrownBy(...) runs the lambda, expects it to throw, and lets you
 * check the exception type and the exact message.
 * testCreateForcesPublishedAndIgnoresRequestId is the most instructive test.
 * create builds a new Tutorial internally, so the test never has a reference to
 * it. The ArgumentCaptor grabs it at the moment it's passed to save, so we can
 * inspect it. The willAnswer(invocation -> invocation.getArgument(0)) stub
 * makes the fake save behave like the real one: it returns the object it
 * received.
 * testUpdate checks the changes on the very object returned, and that save was
 * called with it. testUpdateNotFound checks the error path and that nothing was
 * saved after the failure.
 * Why findByPublished and deleteAll have no test. They are pure delegation (one
 * line passing the call through). A test for them would only check Mockito, not
 * your logic. I kept testDeleteById as a minimal example of verifying a call on
 * a void method. The controller tests in 10D will cover these paths end to end
 * anyway.
 */