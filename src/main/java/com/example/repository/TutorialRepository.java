package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entities.Tutorial;

public interface TutorialRepository extends JpaRepository<Tutorial, Long> {

    // Métodos personalizados:

    List<Tutorial> findByPublished(boolean published);

    List<Tutorial> findByTitleContaining(String title);
    
    // Buscar tutoriales por el id de una etiqueta (many-to-many)
    List<Tutorial> findTutorialsByTagsId(Long tagId);
}

