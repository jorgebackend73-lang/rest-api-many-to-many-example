package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import com.example.entities.Tag;
import com.example.entities.Tutorial;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class TutorialRepositoryTest {

    @Autowired
    private TutorialRepository tutorialRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Tutorial springTutorial;

    private Tutorial hibernateTutorial;

    @BeforeEach
    void setUp() {

        springTutorial = Tutorial.builder()
                .title("Spring Boot basics")
                .description("Intro to Spring Boot")
                .published(true)
                .build();

        hibernateTutorial = Tutorial.builder()
                .title("Hibernate in depth")
                .description("JPA and Hibernate")
                .published(false)
                .build();
    }

    @Test
    @DisplayName("Saving a tutorial generates its id")
    void testSaveTutorial() {

        // when
        Tutorial saved = tutorialRepository.save(springTutorial);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0);
    }

    @Test
    @DisplayName("A saved tutorial is read back from the database with all its fields")
    void testFindById() {

        // given
        Tutorial saved = tutorialRepository.save(springTutorial);

        // Write to the database and empty the cache, so the read below really queries
        // MySQL
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Tutorial> found = tutorialRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Spring Boot basics");
        assertThat(found.get().getDescription()).isEqualTo("Intro to Spring Boot");
        assertThat(found.get().isPublished()).isTrue();
    }

    @Test
    @DisplayName("findByPublished returns only the tutorials with that published value")
    void testFindByPublished() {

        // given
        tutorialRepository.save(springTutorial);
        tutorialRepository.save(hibernateTutorial);

        // when
        List<Tutorial> published = tutorialRepository.findByPublished(true);
        List<Tutorial> notPublished = tutorialRepository.findByPublished(false);

        // then
        assertThat(published).hasSize(1);
        assertThat(published.get(0).getTitle()).isEqualTo("Spring Boot basics");

        assertThat(notPublished).hasSize(1);
        assertThat(notPublished.get(0).getTitle()).isEqualTo("Hibernate in depth");
    }

    @Test
    @DisplayName("findByTitleContaining returns the tutorials whose title contains the text")
    void testFindByTitleContaining() {

        // given
        tutorialRepository.save(springTutorial);
        tutorialRepository.save(hibernateTutorial);

        // when
        List<Tutorial> result = tutorialRepository.findByTitleContaining("Spring");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Spring Boot basics");

        assertThat(tutorialRepository.findByTitleContaining("Kotlin")).isEmpty();
    }

    @Test
    @DisplayName("findTutorialsByTagsId returns only the tutorials linked to that tag")
    void testFindTutorialsByTagsId() {

        // given: springTutorial is linked to the tag, hibernateTutorial is not
        Tag javaTag = Tag.builder().name("java").build();
        springTutorial.addTag(javaTag);

        tutorialRepository.save(springTutorial); // the cascade also persists javaTag
        tutorialRepository.save(hibernateTutorial);

        // The join table rows are written when Hibernate flushes
        entityManager.flush();

        // when
        List<Tutorial> result = tutorialRepository.findTutorialsByTagsId(javaTag.getId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Spring Boot basics");
    }
}

/*
 * Key concepts
 * 
 * @DataJpaTest is a slice test: it loads only the persistence part of Spring
 * (entities, repositories, the DataSource, the EntityManager) and nothing else.
 * No controllers, no security, no services. It's fast, and it's why this counts
 * as a "unit" test of the repository layer. It also does two things for you:
 * 
 * It wraps each test in a transaction that is rolled back at the end, so tests
 * don't leave data behind. (The example's comment attributes this
 * to @AutoConfigureTestDatabase, but the rollback comes from @DataJpaTest.)
 * It ignores your @Configuration classes, so CreatesSamplesData and
 * WebSecurityConfig are not loaded.
 * 
 * @AutoConfigureTestDatabase(replace = Replace.NONE). By default @DataJpaTest
 * swaps your database for an in-memory one such as H2, and it fails if none is
 * on the classpath. Replace.NONE means
 * "use the DataSource from application.properties", your MySQL.
 * 
 * Boot 4 imports. The test annotations moved to new packages in Spring Boot 4,
 * so tutorials written for Boot 3 won't compile. These are the right ones:
 * 
 * org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
 * org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
 * (and its nested Replace)
 * 
 * A test that never touches the database proves nothing. Hibernate keeps a
 * first-level cache (the persistence context). If you save an entity and then
 * findById it in the same transaction, Hibernate hands back the same object
 * from memory, so even a broken mapping would pass. To make a test really hit
 * the database:
 * 
 * entityManager.flush() sends all pending SQL to MySQL (for example the join
 * table rows of a many-to-many).
 * entityManager.clear() empties the cache, so the next read has to query the
 * database.
 * 
 * (Queries like findByPublished always run SQL, so they don't need the
 * clear().)
 * 
 * Given / When / Then is the structure from the example: given = prepare the
 * data, when = run the thing being tested, then = check the result.
 * 
 * 
 *
 * Explanation
 * 
 * @Autowired and @PersistenceContext. In a @DataJpaTest, Spring injects the
 * repository (the real one, with the real SQL). @PersistenceContext is the
 * standard JPA annotation to get the EntityManager, which we use for flush and
 * clear.
 * 
 * @BeforeEach builds fresh, unsaved objects before every test, so tests don't
 * depend on each other. Nothing is saved there: each test saves what it needs.
 * testSaveTutorial doesn't need flush. Since the id is IDENTITY
 * (auto-increment), Hibernate must run the INSERT immediately to learn the id,
 * so save already hits the database.
 * testFindById is the mapping test: if a field weren't mapped correctly, the
 * values read back after clear() would differ.
 * testFindByPublished and testFindByTitleContaining test your query methods:
 * Spring Data generates the SQL from the names, so these tests prove that the
 * names mean what you think. The tables start empty in every test (transactions
 * are rolled back), which is why asserting hasSize(1) is safe.
 * testFindTutorialsByTagsId tests the many-to-many join. addTag links both
 * sides, and since Tutorial.tags has cascade = PERSIST, saving the tutorial
 * also inserts the tag. javaTag.getId() is available afterwards because the
 * insert already happened.
 */
