package com.truper.bonusgenerator.service.analysis;

import com.truper.bonusgenerator.model.dto.response.CommitAnalysisResponse;

import java.time.LocalDate;

public interface CommitAnalysisService {

    CommitAnalysisResponse analyzeLastCompleteWeek();

    CommitAnalysisResponse analyzeByDateRange(LocalDate startDate, LocalDate endDate);
}
