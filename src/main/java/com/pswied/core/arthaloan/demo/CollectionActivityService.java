package com.pswied.core.arthaloan.demo;

import com.pswied.core.arthaloan.demo.CollectionActivityRequest.ActivityOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class CollectionActivityService {

    private static final String DEMO_ACTOR = "demo-user";
    private static final int MAX_NOTES_LENGTH = 500;
    private final ConcurrentMap<String, List<CollectionActivity>> activitiesByLoan = new ConcurrentHashMap<>();
    private final LoanInvestigationService loanInvestigationService;

    public CollectionActivityService(LoanInvestigationService loanInvestigationService) {
        this.loanInvestigationService = loanInvestigationService;
    }

    public List<CollectionActivity> activities(String loanId) {
        loanInvestigationService.loan(loanId);
        return activitiesByLoan.getOrDefault(loanId, List.of());
    }

    public CollectionActivity record(String loanId, CollectionActivityRequest request) {
        LoanDetail loan = loanInvestigationService.loan(loanId);
        if (loan.dpd() <= 0 || loan.overdueAmount().signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Collection activities can only be recorded for overdue loans.");
        }
        validate(request);

        CollectionActivity activity = new CollectionActivity(
                UUID.randomUUID().toString(),
                loanId,
                request.activityType(),
                request.outcome(),
                Instant.now(),
                DEMO_ACTOR,
                request.followUpDate(),
                request.promisedAmount(),
                normalizeNotes(request.notes()));

        activitiesByLoan.compute(loanId, (ignored, existing) -> {
            List<CollectionActivity> updated = new ArrayList<>(existing == null ? List.of() : existing);
            updated.add(activity);
            updated.sort(Comparator.comparing(CollectionActivity::recordedAt).reversed()
                    .thenComparing(CollectionActivity::activityId));
            return List.copyOf(updated);
        });
        return activity;
    }

    private void validate(CollectionActivityRequest request) {
        if (request == null || request.activityType() == null || request.outcome() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "activityType and outcome are required.");
        }
        if (request.notes() != null && request.notes().length() > MAX_NOTES_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "notes must not exceed %d characters.".formatted(MAX_NOTES_LENGTH));
        }
        if (request.promisedAmount() != null && request.promisedAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "promisedAmount must be greater than zero.");
        }
        if (request.outcome() == ActivityOutcome.PROMISE_TO_PAY) {
            if (request.promisedAmount() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "promisedAmount is required for PROMISE_TO_PAY.");
            }
            if (request.followUpDate() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "followUpDate is required for PROMISE_TO_PAY.");
            }
        }
        if (request.outcome() == ActivityOutcome.FOLLOW_UP_REQUIRED && request.followUpDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "followUpDate is required for FOLLOW_UP_REQUIRED.");
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return notes.strip();
    }
}
