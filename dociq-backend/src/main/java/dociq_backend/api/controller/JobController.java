// api/controller/JobController.java

package dociq_backend.api.controller;

import dociq_backend.application.dto.response.JobStatusResponse;
import dociq_backend.domain.model.Job;
import dociq_backend.domain.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;

    @GetMapping("/{jobId}/status")
    public ResponseEntity<JobStatusResponse> getStatus(
            @PathVariable UUID jobId,
            Authentication auth) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new dociq_backend.api.exception.ResourceNotFoundException("Job not found: " + jobId));

        return ResponseEntity.ok(JobStatusResponse.from(job));
    }
}