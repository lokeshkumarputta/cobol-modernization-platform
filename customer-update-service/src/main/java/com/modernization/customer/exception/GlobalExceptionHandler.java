package com.modernization.customer.exception;

import com.modernization.customer.domain.exception.BusinessRuleViolationException;
import com.modernization.customer.domain.model.BusinessRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Maps CUSTREJ reason codes (business rule violations) and ABENDs to RFC 7807 problem responses. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        BusinessRule rule = ex.getRule();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusFor(rule), rule.message());
        problem.setTitle("Business rule violation");
        problem.setProperty("ruleId", rule.name());
        problem.setProperty("errorCode", rule.errorCode());
        return problem;
    }

    /** BR020: unexpected failures roll back the transaction (former U999 ABEND). */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled error - transaction rolled back", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error - the update was rolled back");
        problem.setTitle("Internal error");
        problem.setProperty("errorCode", "U999");
        return problem;
    }

    static HttpStatus statusFor(BusinessRule rule) {
        return switch (rule) {
            case BR001, BR002 -> HttpStatus.BAD_REQUEST;
            case BR003 -> HttpStatus.NOT_FOUND;
            case BR004 -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }
}
