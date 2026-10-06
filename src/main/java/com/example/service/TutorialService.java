package com.example.service;

import java.util.List;

import com.example.entities.Tutorial;

public interface TutorialService {

    // If title is null, returns all tutorials; otherwise, those whose title
    // contains it
    List<Tutorial> findAll(String title);

    // Throws ResourceNotFoundException if the tutorial does not exist
    Tutorial findById(long id);

    List<Tutorial> findByPublished(boolean published);

    Tutorial create(Tutorial tutorial);

    // Throws ResourceNotFoundException if the tutorial does not exist
    Tutorial update(long id, Tutorial tutorial);

    void deleteById(long id);

    void deleteAll();
}

/*
 * Requirements 4, 5 and 6 are a lot of material, so I'm splitting step 10 into
 * four parts and we'll go through them one at a time:
 * 
 * Part What Requirement
 * 10A Create the service layer and refactor the controllers to use it (now)
 * prerequisite
 * 10B Repository tests 4
 * 10C Service tests with Mockito 5
 * 10D Controller integration tests with JWT 6
 * 
 * We start with the service layer because your controllers currently call the
 * repositories directly, so requirement 5 has nothing to test, and requirement
 * 6 (like the example's ProductControllerTest) mocks the service layer under
 * the controller. 10A is a pure refactor: the behavior of the API must not
 * change.
 * 
 * The layers
 * Layer Responsibility Examples in your project
 * Controller The HTTP side: routes, parameters, status codes, @PreAuthorize 204
 * when a list is empty, 201 when something is created
 * Service Business rules "A missing tutorial is a 404",
 * "new tutorials are created published",
 * "adding a tag either creates it or links an existing one"
 * Repository Database access findByPublished, findTagsByTutorialsId
 * 
 * Until now the controllers did all three jobs. Moving the rules into services
 * also gives you something real to test in isolation in 10C.
 * 
 * Create the folder src/main/java/com/example/service/ (singular, like your
 * controller and repository packages; the example uses services).
 * 
 * 
 * 
 * Explanation
 * 
 * Interface plus implementation. TutorialService is the contract: what the
 * service can do. TutorialServiceImpl is how it does it. Spring injects the
 * single implementation wherever the interface is requested. This split matters
 * for testing: in 10D we'll replace the service with a mock using @MockitoBean
 * TutorialService, and the controller won't notice.
 * 
 * @Service and @RequiredArgsConstructor work exactly as in
 * UserDetailsServiceImpl (step 3): a bean, with the final repository injected
 * through the constructor.
 * 
 * @Transactional on the class (Spring's version, as in step 3). Every public
 * method runs inside a transaction: all its database changes succeed together
 * or are rolled back together, and a RuntimeException (like our
 * ResourceNotFoundException) triggers the rollback. It matters most in
 * TagServiceImpl, where one operation changes several entities. In unit tests
 * with Mockito it is simply ignored, since no Spring is running.
 * 
 * findById throws instead of returning an Optional. The rule
 * "a missing tutorial is a 404" now lives in one place, and update reuses it.
 * Before, the same orElseThrow was copied in every controller method.
 * 
 * Where does the exception go? A ResourceNotFoundException thrown in the
 * service propagates through the controller, and your
 * ControllerExceptionHandler catches it exactly as before. The advice handles
 * any exception that escapes a controller method, wherever it was thrown.
 * 
 * create ignores some request fields on purpose. It's what your controller
 * already did: id is generated and published is forced to true. The rule is the
 * same, just moved to a layer where it can be tested.
 */