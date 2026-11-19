// application/port/DocumentAnalyzer.java

package dociq_backend.application.port;

import dociq_backend.application.dto.analysis.AnalysisResult;

/**
 * Port (interface) for document analysis.
 * Service layer depends on this interface only — never on AI libraries directly.
 * MockDocumentAnalyzer (dev) and RealDocumentAnalyzer (prod) both implement this.
 */
public interface DocumentAnalyzer {
    AnalysisResult analyze(String extractedText, String mimeType, String domain);
}