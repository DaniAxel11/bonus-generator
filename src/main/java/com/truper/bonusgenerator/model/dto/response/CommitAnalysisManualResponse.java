package com.truper.bonusgenerator.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitAnalysisManualResponse {

    private List<String> analysis;
    private boolean emailSent;
}
