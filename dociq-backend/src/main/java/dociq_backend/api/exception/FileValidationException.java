package dociq_backend.api.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class FileValidationException extends RuntimeException{
    public FileValidationException(String message) {
        super(message);
    }
}
