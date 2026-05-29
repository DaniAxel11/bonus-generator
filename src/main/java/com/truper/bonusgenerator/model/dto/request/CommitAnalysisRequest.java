package com.truper.bonusgenerator.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitAnalysisRequest {

    private LocalDate startDate;
    private LocalDate endDate;
}
