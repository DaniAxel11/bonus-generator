package com.truper.bonusgenerator.service.email;

import com.truper.bonusgenerator.infrastructure.client.AiClient.AiAnalysisResponse;

import java.time.LocalDate;

public interface EmailService {

    void sendCommitAnalysis(AiAnalysisResponse response, LocalDate startDate, LocalDate endDate);

    void sendTestEmail();
}
