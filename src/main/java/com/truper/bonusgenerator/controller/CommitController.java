package com.truper.bonusgenerator.controller;

import com.truper.bonusgenerator.model.dto.CommitDto;
import com.truper.bonusgenerator.service.commit.CommitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/insert-commit")
    @Operation(summary = "Crea un commit", description = "Recibe un DTO y lo guarda en base de datos")
    public ResponseEntity<CommitDto> createCommit(@RequestBody CommitDto commitRequest) {
        CommitDto response = commitService.createCommit(commitRequest);
        return ResponseEntity.ok(response);
    }




}
