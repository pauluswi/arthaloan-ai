package com.pswied.core.arthaloan.demo;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionActivityServiceTest {

    private CollectionActivityService service() {
        return new CollectionActivityService(new LoanInvestigationService(
                new DemoPortfolioService(new SyntheticPortfolioGenerator(
                        20261117L, LocalDate.of(2026, 10, 31)))));
    }

    @Test
    void recordsAndReturnsAuditableSimulatedActivity() {
        CollectionActivityService service = service();
        CollectionActivityRequest request = new CollectionActivityRequest(
                CollectionActivityRequest.ActivityType.SIMULATED_CONTACT_ATTEMPT,
                CollectionActivityRequest.ActivityOutcome.PROMISE_TO_PAY,
                LocalDate.of(2026, 11, 15),
                new BigDecimal("125.00"),
                "Demo promise, no borrower contacted.");

        CollectionActivity recorded = service.record("LN-002", request);
        var history = service.activities("LN-002");

        assertEquals(1, history.size());
        assertEquals(recorded, history.getFirst());
        assertEquals("demo-user", recorded.recordedBy());
        assertEquals("LN-002", recorded.loanId());
        assertNotNull(recorded.recordedAt());
        assertNotNull(recorded.activityId());
    }

    @Test
    void validatesPromiseFieldsAndRestrictsRecordingToOverdueLoans() {
        CollectionActivityService service = service();

        ResponseStatusException missingAmount = assertThrows(ResponseStatusException.class,
                () -> service.record("LN-002", new CollectionActivityRequest(
                        CollectionActivityRequest.ActivityType.ACCOUNT_REVIEW,
                        CollectionActivityRequest.ActivityOutcome.PROMISE_TO_PAY,
                        LocalDate.of(2026, 11, 15),
                        null,
                        null)));
        assertEquals(400, missingAmount.getStatusCode().value());

        ResponseStatusException currentLoan = assertThrows(ResponseStatusException.class,
                () -> service.record("LN-001", new CollectionActivityRequest(
                        CollectionActivityRequest.ActivityType.ACCOUNT_REVIEW,
                        CollectionActivityRequest.ActivityOutcome.ACCOUNT_REVIEWED,
                        null,
                        null,
                        null)));
        assertEquals(409, currentLoan.getStatusCode().value());
        assertTrue(service.activities("LN-002").isEmpty());
    }

    @Test
    void rejectsOversizedNotesAndUnknownLoans() {
        CollectionActivityService service = service();
        ResponseStatusException oversizedNotes = assertThrows(ResponseStatusException.class,
                () -> service.record("LN-002", new CollectionActivityRequest(
                        CollectionActivityRequest.ActivityType.ACCOUNT_REVIEW,
                        CollectionActivityRequest.ActivityOutcome.ACCOUNT_REVIEWED,
                        null,
                        null,
                        "x".repeat(501))));
        assertEquals(400, oversizedNotes.getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.activities("UNKNOWN")).getStatusCode().value());
    }
}
