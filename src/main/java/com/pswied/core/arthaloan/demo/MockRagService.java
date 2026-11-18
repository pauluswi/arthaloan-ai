package com.pswied.core.arthaloan.demo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MockRagService {

    private static final String RETRIEVAL_METHOD = "Local keyword-overlap retrieval (mock; no embeddings).";
    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_SOURCES = 3;
    private static final int MIN_MATCHED_TERMS = 2;
    private static final Set<String> SINGLE_TERM_TOPICS = Set.of(
            "collection", "delinquency", "dpd", "par", "repayment", "risk", "synthetic", "worklist");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "can", "do", "does", "for", "from",
            "how", "i", "in", "is", "it", "me", "of", "on", "or", "our", "please", "tell", "that",
            "the", "this", "to", "was", "what", "when", "where", "which", "who", "why", "with", "you");
    private static final List<KnowledgeDocument> DOCUMENTS = List.of(
            new KnowledgeDocument(
                    "demo-metric-par",
                    "Portfolio metric definitions",
                    "Portfolio at risk (PAR)",
                    "PAR 1, 7, 30, or 90 (including PAR 30) is the outstanding principal for loans whose days past due (DPD) "
                            + "is greater than or equal to the threshold, divided by total outstanding principal "
                            + "as of the reporting date. The threshold is inclusive. If total portfolio principal "
                            + "is zero, the ratio is unavailable rather than zero percent."),
            new KnowledgeDocument(
                    "demo-metric-repayment",
                    "Portfolio metric definitions",
                    "Repayment rate",
                    "Repayment rate is period-posted allocations to installments due in the selected period, "
                            + "net of reversals, divided by scheduled principal, interest, and fees due in that "
                            + "period. If no amount was scheduled, the rate is unavailable."),
            new KnowledgeDocument(
                    "demo-metric-dpd",
                    "Portfolio metric definitions",
                    "Days past due (DPD)",
                    "DPD is the calendar-day difference between the as-of date and the oldest installment with "
                            + "an unpaid balance and a due date on or before the as-of date. There is no grace "
                            + "period; an installment due on the as-of date has zero DPD."),
            new KnowledgeDocument(
                    "demo-collection-policy",
                    "Demo collection policy",
                    "Worklist prioritization",
                    "The demo-collection-v1 worklist ranks overdue active loans using delinquency (one point per "
                            + "DPD, capped at 40), overdue exposure (5 to 20 points by amount band), and 40% of "
                            + "the illustrative 30-day demo risk score, rounded to the nearest point. Priority "
                            + "bands are HIGH at 60 or more, PRIORITY at 35 to 59, and STANDARD below 35. "
                            + "The ranking is advisory for human review; no borrower is contacted and no action "
                            + "is taken automatically."),
            new KnowledgeDocument(
                    "demo-risk-score",
                    "Demo risk-score notes",
                    "Illustrative 30-day loan score",
                    "The demo-rules-v1 loan score is a deterministic rule score, not a trained model or calibrated "
                            + "probability. It uses current DPD, unpaid balances in the three most recent due "
                            + "installments, and recent payment reversals. It has no measured predictive "
                            + "performance and must not be used for lending or collection decisions."),
            new KnowledgeDocument(
                    "demo-data-limitations",
                    "Synthetic demo data guide",
                    "Data, intended use, and limitations",
                    "All showcase records are deterministic synthetic data. They are not real customers, a financial "
                            + "simulation, or evidence of real-world lending or repayment outcomes. The demo uses "
                            + "a single as-of snapshot and cannot establish historical portfolio changes. Use the "
                            + "dashboard for decision-support demonstrations only; do not enter real or confidential "
                            + "bank data."));

    public RagAnswer answer(RagQuestion request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required.");
        }
        String question = request.question().strip();
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "question must not exceed " + MAX_QUESTION_LENGTH + " characters.");
        }

        Set<String> queryTerms = terms(question);
        List<ScoredDocument> matches = DOCUMENTS.stream()
                .map(document -> score(document, queryTerms))
                .filter(match -> match.matchedTerms() >= MIN_MATCHED_TERMS
                        || (match.matchedTerms() == 1
                        && queryTerms.stream().anyMatch(SINGLE_TERM_TOPICS::contains)
                        && queryTerms.stream().filter(SINGLE_TERM_TOPICS::contains)
                        .anyMatch(terms(match.document().title() + " "
                                + match.document().section() + " "
                                + match.document().content())::contains)))
                .sorted(Comparator.comparingInt(ScoredDocument::matchedTerms).reversed()
                        .thenComparing(match -> match.document().documentId()))
                .limit(MAX_SOURCES)
                .toList();

        if (matches.isEmpty()) {
            return new RagAnswer(
                    "INSUFFICIENT_KNOWLEDGE",
                    question,
                    "I could not find a relevant approved demo knowledge source for that question. "
                            + "Try asking about PAR, repayment rate, DPD, collection prioritization, the demo risk score, "
                            + "or synthetic-data limitations.",
                    "MOCK_RAG",
                    RETRIEVAL_METHOD,
                    OffsetDateTime.now(),
                    List.of());
        }

        List<RagAnswer.Source> sources = matches.stream()
                .map(match -> new RagAnswer.Source(
                        match.document().documentId(),
                        match.document().title(),
                        match.document().section(),
                        match.document().content(),
                        match.matchedTerms()))
                .toList();
        String answer = "Based on the retrieved approved demo knowledge: "
                + String.join(" ", sources.stream()
                        .map(source -> source.section() + ": " + source.excerpt())
                        .toList());
        return new RagAnswer(
                "ANSWERED",
                question,
                answer,
                "MOCK_RAG",
                RETRIEVAL_METHOD,
                OffsetDateTime.now(),
                sources);
    }

    public List<KnowledgeSummary> knowledgeSources() {
        return DOCUMENTS.stream()
                .map(document -> new KnowledgeSummary(
                        document.documentId(), document.title(), document.section()))
                .toList();
    }

    private ScoredDocument score(KnowledgeDocument document, Set<String> queryTerms) {
        Set<String> documentTerms = terms(
                document.title() + " " + document.section() + " " + document.content());
        int matches = (int) queryTerms.stream().filter(documentTerms::contains).count();
        return new ScoredDocument(document, matches);
    }

    private Set<String> terms(String text) {
        Set<String> terms = new LinkedHashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (token.length() >= 2 && !STOP_WORDS.contains(token)) {
                terms.add(token);
            }
        }
        return terms;
    }

    public record KnowledgeSummary(String documentId, String title, String section) {
    }

    private record KnowledgeDocument(String documentId, String title, String section, String content) {
    }

    private record ScoredDocument(KnowledgeDocument document, int matchedTerms) {
    }
}
