package com.truper.bonusgenerator.repository;

import com.truper.bonusgenerator.model.entity.Commit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface CommitRepository extends JpaRepository<Commit, Integer> {

    Commit save(Commit commit);

    List<Commit> findByCreatedAtBetween(Date createdAtAfter, Date createdAtBefore);
}
