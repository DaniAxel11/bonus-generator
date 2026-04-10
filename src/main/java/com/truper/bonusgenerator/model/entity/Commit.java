package com.truper.bonusgenerator.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;

import java.util.Date;

@Getter
@Setter
@ToString
@Entity
@Table(name = "commits")
public class Commit {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo", length = Integer.MAX_VALUE)
    private String repo;

    @Column(name = "branch", length = Integer.MAX_VALUE)
    private String branch;

    @Column(name = "hash", length = Integer.MAX_VALUE)
    private String hash;

    @Column(name = "author", length = Integer.MAX_VALUE)
    private String author;

    @Column(name = "message", length = Integer.MAX_VALUE)
    private String message;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Date createdAt;


}