package com.example.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.entities.Tag;
import com.example.entities.Tutorial;
import com.example.service.TagService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/tags")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Tag>> getAllTags() {

        List<Tag> tags = tagService.findAll();

        if (tags.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(tags, HttpStatus.OK);
    }

    @GetMapping("/tutorials/{tutorialId}/tags")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Tag>> getAllTagsByTutorialId(@PathVariable Long tutorialId) {

        return new ResponseEntity<>(tagService.findByTutorialId(tutorialId), HttpStatus.OK);
    }

    @GetMapping("/tags/{tagId}/tutorials")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Tutorial>> getAllTutorialsByTagId(@PathVariable Long tagId) {

        return new ResponseEntity<>(tagService.findTutorialsByTagId(tagId), HttpStatus.OK);
    }

    @PostMapping("/tutorials/{tutorialId}/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tag> addTag(@PathVariable Long tutorialId,
            @RequestBody Tag tagRequest) {

        return new ResponseEntity<>(tagService.addTagToTutorial(tutorialId, tagRequest), HttpStatus.CREATED);
    }

    @PutMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tag> updateTag(@PathVariable long id, @RequestBody Tag tagRequest) {

        return new ResponseEntity<>(tagService.update(id, tagRequest), HttpStatus.OK);
    }

    @DeleteMapping("/tutorials/{tutorialId}/tags/{tagId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HttpStatus> deleteTagFromTutorial(@PathVariable Long tutorialId,
            @PathVariable Long tagId) {

        tagService.removeTagFromTutorial(tutorialId, tagId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HttpStatus> deleteTag(@PathVariable long id) {

        tagService.deleteById(id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}

/*
 * Part B: @PreAuthorize on the controllers
 * What @PreAuthorize does
 * 
 * It comes from org.springframework.security.access.prepost.PreAuthorize. It
 * evaluates an expression before the method runs. If it's false, the method
 * never executes and an AccessDeniedException is thrown. It works because step
 * 6's @EnableMethodSecurity makes Spring wrap your controllers in a proxy that
 * performs that check first. Without that annotation, @PreAuthorize would be
 * silently ignored.
 * 
 * The text inside is a SpEL expression (Spring Expression Language), evaluated
 * at runtime.
 * hasRole('ADMIN') checks whether the current user (the one stored in the
 * SecurityContext by AuthTokenFilter) has the authority ROLE_ADMIN. Spring adds
 * the ROLE_ prefix itself, which is why we named our roles that way in step 2.
 * hasRole('ADMIN') or hasRole('USER') combines two checks with or. It's the
 * same as hasAnyRole('ADMIN', 'USER').
 * Two layers of security
 * 
 * Your application now checks access twice:
 * 
 * Layer Where Question it answers Failure
 * URL rules filterChain (step 6) Is there a valid logged-in user? 401
 * Method rules @PreAuthorize Does this user have the right role? 403
 * 
 * The GET endpoints are already protected against anonymous users by
 * anyRequest().authenticated(). We still annotate them with hasRole('ADMIN') or
 * hasRole('USER') as the example does: it makes the intention explicit and
 * keeps them protected if someone later relaxes the URL rules.
 * 
 * Which role for which endpoint
 * 
 * Requirement 1 says only ADMIN can create, update and delete, so:
 * 
 * Endpoint Role
 * GET /api/tutorials, GET /api/tutorials/{id}, GET /api/tutorials/published
 * USER or ADMIN
 * GET /api/tags, GET /api/tutorials/{id}/tags, GET /api/tags/{id}/tutorials
 * USER or ADMIN
 * POST /api/tutorials, PUT /api/tutorials/{id} ADMIN
 * DELETE /api/tutorials/{id}, DELETE /api/tutorials ADMIN
 * POST /api/tutorials/{id}/tags, PUT /api/tags/{id} ADMIN
 * DELETE /api/tutorials/{tutorialId}/tags/{tagId}, DELETE /api/tags/{id} ADMIN
 * 
 * Note that POST /api/tutorials/{id}/tags counts as a creation (it can create a
 * new tag), and DELETE /api/tutorials deletes everything, so both are ADMIN
 * only.
 * 
 * TutorialController.java (complete file)
 * 
 * The only changes are the new import and the @PreAuthorize lines. Everything
 * else is your code.
 * 
 * 
 * If you'd rather not repeat the annotation on every GET, you can
 * put @PreAuthorize("hasRole('ADMIN') or hasRole('USER')") on the class and
 * only override the write methods with hasRole('ADMIN') (a method-level
 * annotation replaces the class-level one). I kept it per method, as in the
 * example.
 */

/*
 * What changed in the controllers
 * The field is now a service, not repositories, so the imports of the
 * repositories, ArrayList and ResourceNotFoundException disappear.
 * Each method is thinner: it receives the request, calls the service and
 * chooses the HTTP status (201, 204...). That is exactly the controller's job.
 * 
 * @PreAuthorize stays on the controllers, as in the example. The security rule
 * is about who may call the HTTP endpoint.
 * The routes, roles and status codes are identical.
 */