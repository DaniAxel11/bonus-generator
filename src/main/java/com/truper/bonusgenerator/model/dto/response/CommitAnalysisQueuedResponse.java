package com.truper.bonusgenerator.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitAnalysisQueuedResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private String topic;
    private String status;
}
