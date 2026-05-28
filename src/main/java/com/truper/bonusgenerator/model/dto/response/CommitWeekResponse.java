package com.truper.bonusgenerator.model.dto.response;

import com.truper.bonusgenerator.model.dto.CommitDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitWeekResponse {

    private int weekNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalCommits;
    private List<CommitDto> commits;
}
