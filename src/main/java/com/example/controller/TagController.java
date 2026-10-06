package com.example.controller;

import java.util.ArrayList;
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
import com.example.exception.ResourceNotFoundException;
import com.example.repository.TagRepository;
import com.example.repository.TutorialRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TagController {

    private final TutorialRepository tutorialRepository;
    private final TagRepository tagRepository;

    @GetMapping("/tags")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = new ArrayList<Tag>();

        tagRepository.findAll().forEach(tags::add);

        if (tags.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(tags, HttpStatus.OK);
    }

    @GetMapping("/tutorials/{tutorialId}/tags")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Tag>> getAllTagsByTutorialId(@PathVariable Long tutorialId) {
        if (!tutorialRepository.existsById(tutorialId)) {
            throw new ResourceNotFoundException("Not found Tutorial with id = " + tutorialId);
        }

        List<Tag> tags = tagRepository.findTagsByTutorialsId(tutorialId);
        return new ResponseEntity<>(tags, HttpStatus.OK);
    }

    @GetMapping("/tags/{tagId}/tutorials")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Tutorial>> getAllTutorialsByTagId(@PathVariable Long tagId) {
        if (!tagRepository.existsById(tagId)) {
            throw new ResourceNotFoundException("Not found Tag with id = " + tagId);
        }

        List<Tutorial> tutorials = tutorialRepository.findTutorialsByTagsId(tagId);
        return new ResponseEntity<>(tutorials, HttpStatus.OK);
    }

    @PostMapping("/tutorials/{tutorialId}/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tag> addTag(@PathVariable Long tutorialId,
            @RequestBody Tag tagRequest) {
        Tag tag = tutorialRepository.findById(tutorialId).map(tutorial -> {
            long tagId = tagRequest.getId();

            // tag is existed
            if (tagId != 0L) {
                Tag _tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Not found Tag with id = " + tagId));
                tutorial.addTag(_tag);
                tutorialRepository.save(tutorial);
                return _tag;
            }

            // add and create new Tag
            tutorial.addTag(tagRequest);
            return tagRepository.save(tagRequest);
        }).orElseThrow(() -> new ResourceNotFoundException("Not found Tutorial with id = " + tutorialId));

        return new ResponseEntity<>(tag, HttpStatus.CREATED);
    }

    @PutMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tag> updateTag(@PathVariable long id, @RequestBody Tag tagRequest) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TagId " + id + "not found"));

        tag.setName(tagRequest.getName());

        return new ResponseEntity<>(tagRepository.save(tag), HttpStatus.OK);
    }

    @DeleteMapping("/tutorials/{tutorialId}/tags/{tagId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HttpStatus> deleteTagFromTutorial(@PathVariable Long tutorialId,
            @PathVariable Long tagId) {
        Tutorial tutorial = tutorialRepository.findById(tutorialId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Tutorial with id = " + tutorialId));

        tutorial.removeTag(tagId);
        tutorialRepository.save(tutorial);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HttpStatus> deleteTag(@PathVariable long id) {
        tagRepository.deleteById(id);

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
