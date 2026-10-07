package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

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
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TutorialRepository tutorialRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("Saving a tag generates its id and keeps its name")
    void testSaveTag() {

        // given
        Tag javaTag = Tag.builder().name("java").build();

        // when
        Tag saved = tagRepository.save(javaTag);

        entityManager.flush();
        entityManager.clear();

        Optional<Tag> found = tagRepository.findById(saved.getId());

        // then
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("java");
    }

    @Test
    @DisplayName("findTagsByTutorialsId returns only the tags linked to that tutorial")
    void testFindTagsByTutorialsId() {

        // given: two tags linked to the tutorial and one tag linked to nothing
        Tag javaTag = Tag.builder().name("java").build();
        Tag springTag = Tag.builder().name("spring").build();
        Tag sqlTag = Tag.builder().name("sql").build();

        Tutorial tutorial = Tutorial.builder()
                .title("Spring Boot basics")
                .description("Intro to Spring Boot")
                .published(true)
                .build();

        tutorial.addTag(javaTag);
        tutorial.addTag(springTag);

        tutorialRepository.save(tutorial); // the cascade also persists javaTag and springTag
        tagRepository.save(sqlTag); // exists, but it is not linked

        entityManager.flush();

        // when
        List<Tag> tags = tagRepository.findTagsByTutorialsId(tutorial.getId());

        // then
        assertThat(tags).extracting(Tag::getName)
                .containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    @DisplayName("findTagsByTutorialsId returns an empty list if the tutorial has no tags or does not exist")
    void testFindTagsByTutorialsIdWithoutTags() {

        // given
        Tutorial tutorialWithoutTags = tutorialRepository.save(Tutorial.builder()
                .title("Hibernate in depth")
                .description("JPA and Hibernate")
                .published(false)
                .build());

        entityManager.flush();

        // when and then
        assertThat(tagRepository.findTagsByTutorialsId(tutorialWithoutTags.getId())).isEmpty();

        // A tutorial id that does not exist is not an error at this level: just an
        // empty list
        // (turning it into a 404 is the job of the service layer)
        assertThat(tagRepository.findTagsByTutorialsId(999999L)).isEmpty();
    }
}

/*
 * Explanation
 * extracting(Tag::getName) is AssertJ for "take the name of each element", and
 * containsExactlyInAnyOrder checks the exact content without caring about the
 * order (a Set coming from a join has no guaranteed order).
 * The last test documents a design decision: the repository returns an empty
 * list for an unknown id. In TagServiceImpl we added the existsById check
 * precisely because the repository doesn't give a 404 by itself. In 10C we'll
 * test that check.
 */
