package com.truper.bonusgenerator.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitReportResponse {

    private String impactoPositivo;
    private String problemaDetectado;
    private String solucionPropuesta;

}
