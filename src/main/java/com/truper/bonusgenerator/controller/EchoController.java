package com.truper.bonusgenerator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/echo")
@RequiredArgsConstructor
@Tag(name = "EchoController", description = "Endpoints for send a live test")
public class EchoController {

    @GetMapping("/sayHello")
    @Operation(summary = "Envia una prueba de vida", description = "Recibe una petición y responde con un mensaje de saludo y la hora actual")
    public ResponseEntity<String> createCommit() {
        return ResponseEntity.ok("Hello! The current time is: " + LocalDateTime.now());
    }

}
