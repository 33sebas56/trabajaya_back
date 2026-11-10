package com.trabajaya.shared;
import java.util.*;
import org.slf4j.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
@RestControllerAdvice
public class ApiErrors {
    private static final Logger log=LoggerFactory.getLogger(ApiErrors.class);
    @ExceptionHandler(ApiException.class) ResponseEntity<?> domain(ApiException e){
        return response(e.status,e.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){
        var fields=new LinkedHashMap<String,String>();
        e.getBindingResult().getFieldErrors().forEach(f->fields.putIfAbsent(f.getField(),f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message","Revisa los campos del formulario.","fields",fields));
    }
    @ExceptionHandler({
        HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    }
    )
    ResponseEntity<?> format(Exception e){
        return response(HttpStatus.BAD_REQUEST,"El formato de los datos no es valido.");
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> size(Exception e){
        return response(HttpStatus.PAYLOAD_TOO_LARGE,"La imagen no debe superar 5 MB.");
    }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> duplicate(Exception e){
        log.debug("Conflicto de integridad",e);
        return response(HttpStatus.CONFLICT,"El registro ya existe o entra en conflicto con los datos actuales.");
    }
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class) ResponseEntity<?> denied(Exception e){
        return response(HttpStatus.FORBIDDEN,"No tienes permiso para esta accion.");
    }
    @ExceptionHandler(Exception.class) ResponseEntity<?> other(Exception e){
        String code=UUID.randomUUID().toString().substring(0,8);
        log.error("Error {}",code,e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,"No fue posible completar la solicitud. Referencia: "+code);
    }
    private ResponseEntity<?> response(HttpStatus status,String message){
        return ResponseEntity.status(status).body(Map.of("message",message,"status",status.value()));
    }
}
