package com.truper.bonusgenerator.controller;

import com.truper.bonusgenerator.model.dto.CommitDto;
import com.truper.bonusgenerator.model.dto.request.CommitAnalysisRequest;
import com.truper.bonusgenerator.model.dto.response.CommitAnalysisManualResponse;
import com.truper.bonusgenerator.model.dto.response.CommitAnalysisResponse;
import com.truper.bonusgenerator.model.dto.response.CommitMonthWeeksResponse;
import com.truper.bonusgenerator.service.analysis.CommitAnalysisService;
import com.truper.bonusgenerator.service.commit.CommitService;
import com.truper.bonusgenerator.service.email.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/report")
@RequiredArgsConstructor
@Tag(name = "CommitController", description = "Endpoints for managing commits")
public class CommitController {

    private final CommitService commitService;
    private final CommitAnalysisService commitAnalysisService;
    private final EmailService emailService;

    @PostMapping("/commits/insert-commit")
    @Operation(
            summary = "Crea un commit",
            description = "Recibe un DTO y lo guarda en base de datos"
    )
    public ResponseEntity<CommitDto> createCommit(@RequestBody CommitDto commitRequest) {
        CommitDto response = commitService.createCommit(commitRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/commits/current-month/weeks")
    @Operation(
            summary = "Consulta commits de la ultima semana completa",
            description = "Consulta la ultima semana completa domingo-sabado anterior a la semana en curso"
    )
    public ResponseEntity<CommitMonthWeeksResponse> getCurrentMonthCommitsByWeek() {
        return ResponseEntity.ok(commitService.getCurrentMonthCommitsByWeek());
    }

    @PostMapping("/commits/analysis/manual")
    @Operation(
            summary = "Genera reporte manual de commits",
            description = "Recibe un rango de fechas, consulta commits, genera analisis con IA y envia el resultado por correo"
    )
    public ResponseEntity<CommitAnalysisManualResponse> generateManualCommitAnalysis(
            @RequestBody CommitAnalysisRequest request
    ) {
        CommitAnalysisResponse response = commitAnalysisService.analyzeByDateRange(
                request.getStartDate(),
                request.getEndDate()
        );

        return ResponseEntity.ok(new CommitAnalysisManualResponse(
                response.getAnalysis(),
                response.isEmailSent()
        ));
    }

    @PostMapping("/email/test")
    @Operation(
            summary = "Envia correo de prueba",
            description = "Valida la configuracion SMTP enviando un correo simple al destinatario configurado"
    )
    public ResponseEntity<String> sendTestEmail() {
        emailService.sendTestEmail();
        return ResponseEntity.ok("Correo de prueba enviado correctamente");
    }
}
