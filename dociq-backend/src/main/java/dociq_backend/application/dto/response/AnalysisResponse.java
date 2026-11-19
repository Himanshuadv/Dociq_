package dociq_backend.application.dto.response;


import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// 1. Full Analysis Response
public record AnalysisResponse(
        UUID id,
        UUID documentId,
        String summary,
        List<String> keyPoints,
        List<String> riskFlags,
        String domain,
        BigDecimal confidenceScore,
        Integer wordCount
) {}


