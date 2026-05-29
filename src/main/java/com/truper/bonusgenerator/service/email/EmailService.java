package com.truper.bonusgenerator.service.email;

import com.truper.bonusgenerator.infrastructure.client.AiClient.AiAnalysisResponse;

public interface EmailService {

    void sendCommitAnalysis(AiAnalysisResponse response);

    void sendTestEmail();
}
