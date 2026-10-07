package com.example.spring_security_jwt.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.spring_security_jwt.model.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User ana;

    @BeforeEach
    void setUp() {

        // The password is not a real hash: the repository doesn't care, it only has to
        // fit the column
        ana = User.builder()
                .username("ana")
                .email("ana@example.com")
                .password("not-a-real-hash-but-valid-for-the-column")
                .build();
    }

    @Test
    @DisplayName("findByEmail returns the user registered with that email")
    void testFindByEmail() {

        // given
        userRepository.save(ana);

        // when
        Optional<User> found = userRepository.findByEmail("ana@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("ana");
    }

    @Test
    @DisplayName("findByEmail returns empty if the email is unknown, and does not match usernames")
    void testFindByEmailNotFound() {

        // given
        userRepository.save(ana);

        // when and then
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();

        // The login is by email: the username value must not find the user
        assertThat(userRepository.findByEmail("ana")).isEmpty();
    }

    @Test
    @DisplayName("existsByUsername and existsByEmail tell whether the value is already taken")
    void testExistsByUsernameAndEmail() {

        // given
        userRepository.save(ana);

        // then
        assertThat(userRepository.existsByUsername("ana")).isTrue();
        assertThat(userRepository.existsByUsername("nobody")).isFalse();

        assertThat(userRepository.existsByEmail("ana@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    @DisplayName("The database rejects two users with the same email")
    void testDuplicateEmailIsRejected() {

        // given
        userRepository.save(ana);

        User sameEmail = User.builder()
                .username("other")
                .email("ana@example.com")
                .password("not-a-real-hash-but-valid-for-the-column")
                .build();

        // when and then
        assertThatThrownBy(() -> userRepository.saveAndFlush(sameEmail))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("The database rejects two users with the same username")
    void testDuplicateUsernameIsRejected() {

        // given
        userRepository.save(ana);

        User sameUsername = User.builder()
                .username("ana")
                .email("other@example.com")
                .password("not-a-real-hash-but-valid-for-the-column")
                .build();

        // when and then
        assertThatThrownBy(() -> userRepository.saveAndFlush(sameUsername))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

/*
 * UserRepositoryTest.java
 * 
 * Create it in src/test/java/com/example/spring_security_jwt/repository/. This
 * one covers the repository behind the email login (requirement 2).
 * 
 * Explanation
 * Import trap again. User must be com.example.spring_security_jwt.model.User,
 * not Spring Security's User.
 * testFindByEmailNotFound has a second assertion that directly checks
 * requirement 2: searching with the username value finds nothing, so the email
 * is the only way in.
 * The two duplicate tests verify the @UniqueConstraints you declared in step 2.
 * They use saveAndFlush to force the INSERT immediately. MySQL rejects it,
 * Hibernate reports a constraint violation, and Spring translates it to
 * DataIntegrityViolationException. assertThatThrownBy runs the lambda and
 * checks the exception type.
 * Why "ana" and not "admin1". In a full test run, other test classes that start
 * the whole application insert admin1 and user1 through CreatesSamplesData.
 * Using unrelated values means these tests pass regardless of what ran before.
 * There is no RoleRepository test on purpose. Roles are reference data created
 * by the seed, and the roles.name column has no unique constraint, so a test
 * inserting ROLE_ADMIN could end up with two rows and fail depending on test
 * order. Role lookup is exercised indirectly in 10D, through the signup.
 */

/*
 * What we deliberately don't test
 * 
 * Methods like save, findAll or deleteById are Spring Data's own code, and
 * testing them is testing the framework. The tests above focus on what is
 * yours: the custom queries, the field mapping, the many-to-many join and the
 * unique constraints. That's also why there's no delete test: deleting a
 * tutorial that still has linked tags needs extra care in Hibernate (both sides
 * of the relation must be unlinked first), and that belongs to a service-level
 * behavior, not to a repository query.
 * 
 * Running the tests
 * 
 * With MySQL on and the application stopped, in the project folder:
 * 
 * ./mvnw test
 * -Dtest="TutorialRepositoryTest,TagRepositoryTest,UserRepositoryTest"
 * 
 * You can also use the Testing panel of VS Code (the beaker icon), where each
 * test gets its own play button. You'll see a lot of SQL in the console,
 * because of show-sql=true. The expected result is:
 * 
 * Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
 * BUILD SUCCESS
 * 
 * (If you run plain ./mvnw test, it's 14, because your existing contextLoads
 * test runs too.)
 * 
 * A sanity check worth doing once: a test that can't fail is useless, so break
 * something on purpose. For example, in TutorialRepositoryTest.setUp change
 * .published(true) to .published(false) and run it: testFindByPublished and
 * testFindById should fail with a readable message (that's where
 * the @ToString.Exclude fix pays off). Then undo the change.
 * 
 * If something fails, send me the first error in the console (the part after
 * Caused by is usually the useful one).
 * 
 * When the 13 tests are green, we continue with 10C: unit tests of
 * TutorialServiceImpl and TagServiceImpl using Mockito, where we'll also fix a
 * subtle mistake that the example's ProductServiceImplTest has in the way it
 * stubs a method.
 */
