package com.example.service;

import java.util.List;

import com.example.entities.Tag;
import com.example.entities.Tutorial;

public interface TagService {

    List<Tag> findAll();

    // Throws ResourceNotFoundException if the tutorial does not exist
    List<Tag> findByTutorialId(long tutorialId);

    // Throws ResourceNotFoundException if the tag does not exist
    List<Tutorial> findTutorialsByTagId(long tagId);

    // If the tag has an id, links that existing tag to the tutorial.
    // If it has no id, creates it and links it.
    Tag addTagToTutorial(long tutorialId, Tag tagRequest);

    // Throws ResourceNotFoundException if the tag does not exist
    Tag update(long id, Tag tagRequest);

    // Throws ResourceNotFoundException if the tutorial does not exist
    void removeTagFromTutorial(long tutorialId, long tagId);

    void deleteById(long id);
}

/*
 * Explanation
 * Two dependencies. Tags and tutorials are related, so this service needs both
 * repositories, just as TagController did.
 * addTagToTutorial is the interesting one. It's your controller's addTag logic,
 * unchanged, with three branches that 10C will test one by one: tutorial not
 * found (404), tag id given and found (link it), tag id given but not found
 * (404), and no id (create it). tagId != 0L works because Tag.id is a primitive
 * long: a missing id in the JSON arrives as 0.
 * Why @Transactional matters here. tutorial.addTag(...) touches the lazy tags
 * collection and then the code saves. Inside one transaction, loading and
 * saving share one persistence context and either both happen or neither does.
 * getTutorialOrThrow is a private helper, so the "find or 404" for tutorials is
 * written once for the two methods that need it.
 * One message fixed. Your updateTag said "TagId " + id + "not found" (without a
 * space, giving TagId 5not found). It now says Not found Tag with id = 5,
 * consistent with the others.
 */
