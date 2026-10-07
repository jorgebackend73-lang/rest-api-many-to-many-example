package com.example.entities;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
@Table(name = "tags")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Tag implements Serializable {

    private final static long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;

    // @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE }, mappedBy = "tags")
    @JsonIgnore
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
    private final Set<Tutorial> tutorials = new HashSet<>(); // inicializamos y con final lombok no toca

}
