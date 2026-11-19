package dociq_backend.infrastructure.storage;


import java.util.Set;

public final class MimeTypeUtils {

    private MimeTypeUtils() {} // utility class — no instantiation

    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public static String resolveExtension(String mimeType) {
        return switch (mimeType) {
            case "application/pdf" -> ".pdf";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            default -> ".bin";
        };
    }
}