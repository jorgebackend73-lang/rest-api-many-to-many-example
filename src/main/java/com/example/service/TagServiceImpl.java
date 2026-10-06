package com.example.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entities.Tag;
import com.example.entities.Tutorial;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.TagRepository;
import com.example.repository.TutorialRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    private final TutorialRepository tutorialRepository;

    @Override
    public List<Tag> findAll() {

        return tagRepository.findAll();
    }

    @Override
    public List<Tag> findByTutorialId(long tutorialId) {

        if (!tutorialRepository.existsById(tutorialId)) {
            throw new ResourceNotFoundException
                    ("Not found Tutorial with id = " + tutorialId);
        }

        return tagRepository.findTagsByTutorialsId(tutorialId);
    }

    @Override
    public List<Tutorial> findTutorialsByTagId(long tagId) {

        if (!tagRepository.existsById(tagId)) {
            throw new ResourceNotFoundException
                    ("Not found Tag with id = " + tagId);
        }

        return tutorialRepository.findTutorialsByTagsId(tagId);
    }

    @Override
    public Tag addTagToTutorial(long tutorialId, Tag tagRequest) {

        Tutorial tutorial = getTutorialOrThrow(tutorialId);

        long tagId = tagRequest.getId();

        // The tag already exists: link it to the tutorial
        if (tagId != 0L) {

            Tag existingTag = tagRepository.findById(tagId)
                    .orElseThrow(() -> new ResourceNotFoundException
                            ("Not found Tag with id = " + tagId));

            tutorial.addTag(existingTag);
            tutorialRepository.save(tutorial);

            return existingTag;
        }

        // The tag is new: link it to the tutorial and create it
        tutorial.addTag(tagRequest);

        return tagRepository.save(tagRequest);
    }

    @Override
    public Tag update(long id, Tag tagRequest) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException
                        ("Not found Tag with id = " + id));

        tag.setName(tagRequest.getName());

        return tagRepository.save(tag);
    }

    @Override
    public void removeTagFromTutorial(long tutorialId, long tagId) {

        Tutorial tutorial = getTutorialOrThrow(tutorialId);

        tutorial.removeTag(tagId);

        tutorialRepository.save(tutorial);
    }

    @Override
    public void deleteById(long id) {

        tagRepository.deleteById(id);
    }

    private Tutorial getTutorialOrThrow(long tutorialId) {

        return tutorialRepository.findById(tutorialId)
                .orElseThrow(() -> new ResourceNotFoundException
                        ("Not found Tutorial with id = " + tutorialId));
    }
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
