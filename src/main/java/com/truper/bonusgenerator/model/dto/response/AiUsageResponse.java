package com.truper.bonusgenerator.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiUsageResponse {

    private int promptTokenCount;
    private int candidatesTokenCount;
    private int totalTokenCount;
    private long responseTimeMs;
}
