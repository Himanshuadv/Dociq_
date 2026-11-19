package dociq_backend.application.service;


import dociq_backend.api.exception.ResourceNotFoundException;
import dociq_backend.application.dto.response.*;
import dociq_backend.domain.model.AnalysisResult;
import dociq_backend.domain.repository.AnalysisResultRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional(readOnly = true) // Optimizes DB performance for read operations
public class AnalysisService {

    private final AnalysisResultRepository repository;

    public AnalysisService(AnalysisResultRepository repository) {
        this.repository = repository;
    }

    // should not we get this using job id; yes one document => one analysis result
    private AnalysisResult getAnalysisOrThrow(UUID documentId) {
        return repository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Analysis not found for document ID: " + documentId));
    }

    public AnalysisResponse getFullAnalysis(UUID documentId) {
        AnalysisResult result = getAnalysisOrThrow(documentId);
        return new AnalysisResponse(
                result.getId(),
                result.getDocument().getId(),
                result.getSummary(),
                result.getKeyPoints(),
                result.getRiskFlags(),
                result.getDomain(),
                result.getConfidenceScore(),
                result.getWordCount()
        );
    }

    public SummaryResponse getSummary(UUID documentId) {
        AnalysisResult result = getAnalysisOrThrow(documentId);
        return new SummaryResponse(result.getSummary());
    }

    public RiskFlagsResponse getRiskFlags(UUID documentId) {
        AnalysisResult result = getAnalysisOrThrow(documentId);
        return new RiskFlagsResponse(
                result.getRiskFlags(),
                result.riskFlagCount(),
                result.hasRisks()
        );
    }

    public KeyPointsResponse getKeyPoints(UUID documentId) {
        AnalysisResult result = getAnalysisOrThrow(documentId);
        return new KeyPointsResponse(
                result.getKeyPoints(),
                result.getKeyPoints().size()
        );
    }
}