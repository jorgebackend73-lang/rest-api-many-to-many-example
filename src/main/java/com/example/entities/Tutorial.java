package com.example.entities;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "tutorials")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Tutorial implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String title;
    private String description;
    private boolean published;

    // @Builder.Default // para que el constructor no intente inicializarlo
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    // dar nombre y anotar la tabla intermedia
    // @JoinTable(name = "tutorial_tags",
    // joinColumns = {@JoinColumn(name = "tutorial_id")},
    // inverseJoinColumns = {@JoinColumn(name = "tag_id")})
    @ToString.Exclude
    /*
     * Preparation 1: a fix in the two entities
     * 
     * Both of your entities have @ToString, and each one prints the other:
     * Tutorial.toString() prints its tags, and each Tag prints its tutorials, which
     * print their tags, and so on. For two linked objects that is an infinite loop
     * (StackOverflowError). It only happens when toString() gets called, and our
     * tests will trigger it exactly when it hurts most: when an assertion fails,
     * because AssertJ prints the objects in its error message. You'd see a
     * confusing StackOverflowError instead of the real failure.
     */
    private final Set<Tag> tags = new HashSet<>(); // inicializamos y con final lombok no toca

    // crea un tag y a la vez asigna un tutorial
    public void addTag(Tag tag) {
        // this.tags.add(tag);
        // tag.getTutorials().add(this);
        // }

        // public void addTag(Tag tag) {

        // if (this.tags == null) {
        // this.tags = new HashSet<>();
        // }

        // if (tag.getTutorials() == null) {
        // tag.setTutorials(new HashSet<>());
        // }

        this.tags.add(tag);
        tag.getTutorials().add(this);
    }

    // explicado en el video del 09-07-2026, pero que nos lo explique la IA tb
    // este y el anterior método.
    public void removeTag(long tagId) {
        Tag tag = this.tags.stream().filter(t -> t.getId() == tagId).findFirst().orElse(null);
        if (tag != null) {
            this.tags.remove(tag);
            tag.getTutorials().remove(this);
        }
    }

}
