package com.truper.bonusgenerator.service.commit;

import com.truper.bonusgenerator.model.dto.CommitDto;

import java.util.List;

public interface CommitService {

    CommitDto createCommit(CommitDto commit);

    List<CommitDto> getCommitsByRangeDate();
}
