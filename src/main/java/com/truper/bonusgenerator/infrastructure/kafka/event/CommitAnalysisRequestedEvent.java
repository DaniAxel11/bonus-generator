package com.truper.bonusgenerator.infrastructure.kafka.event;

import java.time.LocalDate;

public record CommitAnalysisRequestedEvent(
        LocalDate startDate,
        LocalDate endDate,
        String source
) {
}
