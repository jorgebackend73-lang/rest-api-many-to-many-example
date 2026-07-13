package com.example.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.entities.Tag;
import com.example.entities.Tutorial;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.TagRepository;
import com.example.repository.TutorialRepository;

@RestController
@RequestMapping("/api")
public class TagController {

    private final TutorialRepository tutorialRepository;
    private final TagRepository tagRepository;

    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = new ArrayList<Tag>();

        tagRepository.findAll().forEach(tags::add);

        if (tags.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(tags, HttpStatus.OK);
    }

    @GetMapping("/tutorials/{tutorialId}/tags")
    public ResponseEntity<List<Tag>> getAllTagsByTutorialId(@PathVariable Long tutorialId) {
        if (!tutorialRepository.existsById(tutorialId)) {
            throw new ResourceNotFoundException("Not found Tutorial with id = " + tutorialId);
        }

        List<Tag> tags = tagRepository.findTagsByTutorialsId(tutorialId);
        return new ResponseEntity<>(tags, HttpStatus.OK);
    }

    @GetMapping("/tags/{tagId}/tutorials")
    public ResponseEntity<List<Tutorial>> getAllTutorialsByTagId(@PathVariable Long tagId) {
        if (!tagRepository.existsById(tagId)) {
            throw new ResourceNotFoundException("Not found Tag with id = " + tagId);
        }

        List<Tutorial> tutorials = tutorialRepository.findTutorialsByTagsId(tagId);
        return new ResponseEntity<>(tutorials, HttpStatus.OK);
    }
    
}
