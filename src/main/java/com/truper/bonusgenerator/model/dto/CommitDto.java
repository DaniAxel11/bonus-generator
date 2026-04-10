package com.truper.bonusgenerator.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for {@link com.truper.bonusgenerator.model.entity.Commit}
 */
@Data
public class CommitDto implements Serializable {
    Long id;
    String repo;
    String branch;
    String hash;
    String author;
    String message;
    Date createdAt;
}