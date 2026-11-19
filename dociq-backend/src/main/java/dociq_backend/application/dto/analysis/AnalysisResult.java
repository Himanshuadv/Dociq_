// application/dto/analysis/AnalysisResult.java

package dociq_backend.application.dto.analysis;

import lombok.Builder;
import java.util.List;

@Builder
public record AnalysisResult(
        String summary,
        List<String> keyPoints,
        List<String> riskFlags,
        String domain
) {}