package dociq_backend.infrastructure.worker;

import dociq_backend.application.port.DocumentAnalyzer;
import dociq_backend.domain.model.Document;
import dociq_backend.domain.model.Job;
import dociq_backend.infrastructure.storage.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Pure pipeline orchestrator.
 * Runs on a worker thread (called by AsyncDocumentProcessor).
 *
 * ZERO @Transactional here — intentional by design.
 * ALL DB operations are delegated to JobPersistenceService (external calls ✅).
 *
 * This class only owns:
 * - File I/O (Tika text extraction)
 * - AI/LLM calls (DocumentAnalyzer)
 * - Pipeline coordination (call persistence service at right moments)
 *
 * Self-invocation is IMPOSSIBLE here because there are NO @Transactional
 * or @Async methods in this class to accidentally bypass.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final JobPersistenceService jobPersistenceService; // ALL DB ops go here
    private final LocalFileStorageService storageService;
    private final DocumentAnalyzer documentAnalyzer;


    // FIX 1: Set a generous but explicit character limit instead of relying
    // on Tika's silent default truncation of 100,000 chars.
    // 1_000_000 chars ≈ ~700KB of text — enough for large documents.
    private static final int MAX_TEXT_LENGTH = 1_000_000;
    private static final Tika TIKA = new Tika(); // thread-safe, reuse across threads

    static {
        TIKA.setMaxStringLength(MAX_TEXT_LENGTH);
    }

    /**
     * Main pipeline. Called by AsyncDocumentProcessor on a worker thread.
     *
     * Flow:
     *   loadJob (short tx, ~5ms)
     *   → extractText (file I/O, 1-3s, NO transaction)
     *   → analyze (LLM API, 10-30s, NO transaction)
     *   → persistResults (short tx, ~50ms)
     *
     * DB connection is NEVER held during extractText or analyze.
     * Total DB connection time: ~55ms out of potentially 33 seconds of work.
     */
//    public void process(UUID jobId) {
//
//        // ── Step 1: Load job ─────────────────────────────────────────────
//        // External call to JobPersistenceService → @Transactional(readOnly) works ✅
//        Job job = jobPersistenceService.loadJobWithDocument(jobId);
//
//        if (job == null) {
//            return; // logged inside loadJobWithDocument()
//        }
//
//
//        // FIX 2: Null-check the document before using it.
//        // A missing FK relationship or corrupted record should fail gracefully.
//        Document document = job.getDocument();
//
//        if (document == null) {
//            log.error("Pipeline aborted: job={} reason=document_is_null", jobId);
//            jobPersistenceService.markFailedOrRetry(jobId, "Associated document is null");
//            return;
//        }
//
//
//        log.info("Pipeline started: job={} file={} domain={}",
//                jobId, document.getOriginalFilename(), document.getDomain());
//
//        try {
//            // ── Step 2: Extract text ─────────────────────────────────────
//            // Pure file I/O — no DB connection, no transaction needed
//            String extractedText = extractText(document);
//            int wordCount = countWords(extractedText);
//            log.info("Text extracted: job={} wordCount={}", jobId, wordCount);
//
//            // ── Step 3: AI analysis ──────────────────────────────────────
//            // Pure external API call — no DB connection, no transaction needed
//            log.info("AI analysis starting: job={}", jobId);
//            dociq_backend.application.dto.analysis.AnalysisResult result =
//                    documentAnalyzer.analyze(
//                            extractedText,
//                            document.getMimeType(),
//                            document.getDomain()
//                    );
//            log.info("AI analysis done: job={} riskFlags={}",
//                    jobId, result.riskFlags().size());
//
//            // ── Step 4: Persist results ───────────────────────────────────
//            // External call → @Transactional works ✅
//            // analysis_results INSERT + job UPDATE in one atomic transaction
//            jobPersistenceService.persistResultsAndComplete(job, document, result);
//
//        } catch (InterruptedException ex) {
//            // FIX 3: Never swallow InterruptedException.
//            // Restore the interrupted flag so the executor/thread pool
//            // can perform a clean shutdown.
//            Thread.currentThread().interrupt();
//            log.error("Pipeline interrupted: job={}", jobId, ex);
//            jobPersistenceService.markFailedOrRetry(jobId, "Worker thread interrupted");
//
//        } catch (Exception ex) {
//            log.error("Pipeline exception: job={} error={}", jobId, ex.getMessage(), ex);
//            jobPersistenceService.markFailedOrRetry(jobId, ex.getMessage());
//        }
//    }


    // ───────────────────────── PRIVATE HELPERS ───────────────────────────────
    // These are plain Java methods — no Spring annotations.
    // No proxy needed. No risk of self-invocation issues.


    public void process(UUID jobId) {

        Job job = jobPersistenceService.loadJobWithDocument(jobId);
        if (job == null) return;

        Document document = job.getDocument();
        if (document == null) {
            log.error("Pipeline aborted: job={} reason=document_is_null", jobId);
            jobPersistenceService.markFailedOrRetry(jobId, "Associated document is null");
            return;
        }

        // ✅ Check interrupt flag BEFORE long-running I/O and LLM calls
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Pipeline skipped (thread interrupted): job={}", jobId);
            jobPersistenceService.markFailedOrRetry(jobId, "Worker thread interrupted before start");
            return;
        }

        log.info("Pipeline started: job={} file={} domain={}",
                jobId, document.getOriginalFilename(), document.getDomain());

        try {
            // ── Step 2: Extract text ─────────────────────────────────────
            String extractedText = extractText(document);
            int wordCount = countWords(extractedText);
            log.info("Text extracted: job={} wordCount={}", jobId, wordCount);

            // ✅ Check again before the slowest step (LLM call can be 10-30s)
            if (Thread.currentThread().isInterrupted()) {
                log.warn("Pipeline aborted after extraction (thread interrupted): job={}", jobId);
                jobPersistenceService.markFailedOrRetry(jobId, "Worker thread interrupted before LLM call");
                return;
            }

            // ── Step 3: AI analysis ──────────────────────────────────────
            log.info("AI analysis starting: job={}", jobId);
            dociq_backend.application.dto.analysis.AnalysisResult result =
                    documentAnalyzer.analyze(
                            extractedText,
                            document.getMimeType(),
                            document.getDomain()
                    );
            log.info("AI analysis done: job={} riskFlags={}",
                    jobId, result.riskFlags().size());

            // ── Step 4: Persist results ───────────────────────────────────
            jobPersistenceService.persistResultsAndComplete(job, document, result);

        } catch (Exception ex) {
            log.error("Pipeline exception: job={} error={}", jobId, ex.getMessage(), ex);
            jobPersistenceService.markFailedOrRetry(jobId, ex.getMessage());
        }
    }


    private String extractText(Document document) throws IOException {
        // FIX 4: Tika.parseToString() can also throw TikaException on corrupt files.
        // Catch it here and wrap as IOException so the caller's catch handles it uniformly,
        // OR rethrow as a checked exception your pipeline already handles.
        try (InputStream is = storageService.retrieve(
                document.getId(), document.getMimeType())) {

            String text = TIKA.parseToString(is);
            return text;
        } catch (TikaException e) {
            throw new IOException("Tika failed to parse document id="
                    + document.getId(), e);
        }
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }
}