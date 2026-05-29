package com.truper.bonusgenerator.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitAnalysisResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private int totalCommits;
    private List<String> analysis;
    private AiUsageResponse usage;
    private boolean emailSent;
    private String emailMessage;
}
