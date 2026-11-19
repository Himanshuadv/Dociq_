// infrastructure/ai/MockDocumentAnalyzer.java

package dociq_backend.infrastructure.ai.mock;

import dociq_backend.application.dto.analysis.AnalysisResult;
import dociq_backend.application.port.DocumentAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Profile("dev") // only active in dev — swap to real implementation in prod
public class MockDocumentAnalyzer implements DocumentAnalyzer {

    @Override
    public AnalysisResult analyze(String extractedText, String mimeType, String domain) {

        log.info("MockDocumentAnalyzer: simulating 2s analysis for domain={}", domain);

        simulateProcessingDelay();

        // Test hook: force a failure by including this phrase in the document
        if (extractedText != null && extractedText.contains("FORCE_FAILURE")) {
            throw new RuntimeException("Simulated analysis failure (FORCE_FAILURE detected in text)");
        }

        String preview = extractedText != null
                ? extractedText.substring(0, Math.min(80, extractedText.length()))
                : "(empty)";

        return AnalysisResult.builder()
                .summary("Mock summary: This document covers — " + preview + "...")
                .keyPoints(List.of(
                        "Key point 1: Important clause identified in section 2",
                        "Key point 2: Payment terms specified as net-30",
                        "Key point 3: Termination with 30-day notice required"
                ))
                .riskFlags(List.of(
                        "⚠️ Clause 4.2: Unlimited liability exposure identified",
                        "⚠️ Clause 7.1: Non-compete duration exceeds 2 years"
                ))
                .domain(domain)
                .build();
    }

    private void simulateProcessingDelay() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // always restore interrupt flag
        }
    }
}