package dociq_backend.domain.enums;


public enum JobStatus {
    PENDING,    // waiting to be picked up by worker
    RUNNING,    // worker is currently processing
    COMPLETED,  // successfully processed, results saved
    FAILED      // failed, may be retried or permanently failed
}