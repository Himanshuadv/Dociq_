package dociq_backend.api.controller;

import dociq_backend.application.dto.response.DocumentResponse;
import dociq_backend.application.service.DocumentService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/document")
public class DocumentController {

    // Dummy DTO to represent a Document in the API responses
    private final DocumentService documentService;
    public record DocumentDto(String id, String title, String content, String status, LocalDateTime createdAt) {}

    // 1. GET ALL
    @GetMapping
    public ResponseEntity<List<DocumentDto>> getAllDocuments() {
        List<DocumentDto> dummyDocs = List.of(
                new DocumentDto(UUID.randomUUID().toString(), "Q1 Financials", "Revenue report...", "APPROVED", LocalDateTime.now()),
                new DocumentDto(UUID.randomUUID().toString(), "Architecture Diagram", "System flow...", "DRAFT", LocalDateTime.now())
        );
        return ResponseEntity.ok(dummyDocs);
    }

    // 2. GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> getDocumentById(@PathVariable String id) {
        DocumentDto dummyDoc = new DocumentDto(id, "Sample Document", "This is dummy content for id: " + id, "DRAFT", LocalDateTime.now());
        return ResponseEntity.ok(dummyDoc);
    }


    // 4. UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<DocumentDto> updateDocument(@PathVariable String id, @RequestBody DocumentDto request) {
        // Pretend we found it and updated it
        DocumentDto updatedDoc = new DocumentDto(
                id,
                request.title() + " (Updated)",
                request.content(),
                "UPDATED",
                LocalDateTime.now()
        );
        return ResponseEntity.ok(updatedDoc);
    }

    // 5. DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
        // Pretend we deleted it
        return ResponseEntity.noContent().build();
    }
    // 6. Upload
    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(@RequestPart("file") MultipartFile file,
                                                           @RequestPart("title") @NotBlank String title, Authentication auth){
        String userEmail = auth.getName();
        DocumentResponse documentResponse =  documentService.uploadDocument(file, title, userEmail);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(documentResponse);
    }
}