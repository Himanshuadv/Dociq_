package dociq_backend.application.dto.response;


import java.util.List;

// 3. Risk Flags Response
public record RiskFlagsResponse(
        List<String> riskFlags,
        int count,
        boolean hasRisks
) {}
