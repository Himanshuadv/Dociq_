package dociq_backend.api.controller;


import dociq_backend.application.dto.response.*;
import dociq_backend.application.service.AnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents/{id}/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    // GET /api/v1/documents/{id}/analysis
    @GetMapping
    public ResponseEntity<AnalysisResponse> getFullAnalysis(@PathVariable("id") UUID documentId) {

        return ResponseEntity.ok(analysisService.getFullAnalysis(documentId));
    }

    // GET /api/v1/documents/{id}/analysis/summary
    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummary(@PathVariable("id") UUID documentId) {
        return ResponseEntity.ok(analysisService.getSummary(documentId));
    }

    // GET /api/v1/documents/{id}/analysis/risk-flags
    @GetMapping("/risk-flags")
    public ResponseEntity<RiskFlagsResponse> getRiskFlags(@PathVariable("id") UUID documentId) {
        return ResponseEntity.ok(analysisService.getRiskFlags(documentId));
    }

    // GET /api/v1/documents/{id}/analysis/key-points
    @GetMapping("/key-points")
    public ResponseEntity<KeyPointsResponse> getKeyPoints(@PathVariable("id") UUID documentId) {
        return ResponseEntity.ok(analysisService.getKeyPoints(documentId));
    }
}