package dociq_backend.application.dto.response;


import java.util.List;

// 4. Key Points Response
public record KeyPointsResponse(
        List<String> keyPoints,
        int count
) {}