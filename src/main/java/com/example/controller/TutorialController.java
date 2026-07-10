package com.example.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.entities.Tutorial;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.TutorialRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TutorialController {

    private final TutorialRepository tutorialRepository;


    @GetMapping("/tutorials")
    public ResponseEntity<List<Tutorial>> getAllTutorials(@RequestParam(required = false) String title) {
        
        // Creamos lista de tutoriales
        List<Tutorial> tutorials = new ArrayList<Tutorial>();

        // Si no me pasan un titulo paso todos los tutoriales. Como al final hay un forEach
        // no hace falta un stream. Paso todos los tutoriales y los añado a la lista:
        if (title == null)
            tutorialRepository.findAll().forEach(tutorials::add);
        // en caso de tener un titulo buscamos por titulo y son los tutoriales que devolvemos:
        else
            tutorialRepository.findByTitleContaining(title).forEach(tutorials::add);
        // si no viene nada de nada, se lo indicamos:
        if (tutorials.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(tutorials, HttpStatus.OK);
    }

    @GetMapping("/tutorials/{id}")
    public ResponseEntity<Tutorial> getTutorialById(@PathVariable("id") long id) {
        Tutorial tutorial = tutorialRepository.findById(id)
                // con esto mandamos nuestro mensaje personalizado que empezamos a preparar desde ResourceNotFoundException
                .orElseThrow(() -> new ResourceNotFoundException("Not found Tutorial with id = " + id));

        // HttpStatus.OK es el que te da el estado 200 del servidor, como vemos en postman.
        return new ResponseEntity<>(tutorial, HttpStatus.OK);
    }

    // Este método recoge un tutorial y usa un constructor para crearlo
    @PostMapping("/tutorials")
    public ResponseEntity<Tutorial> createTutorial(@RequestBody Tutorial tutorial) {
        Tutorial _tutorial = tutorialRepository
                .save(
                    Tutorial.builder()
                    .title(tutorial.getTitle())
                    .description(tutorial.getDescription())
                    .published(true)
                    .build());
        return new ResponseEntity<>(_tutorial, HttpStatus.CREATED);
    }

}
