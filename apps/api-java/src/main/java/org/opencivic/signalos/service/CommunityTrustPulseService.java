package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityTrustPulseResponse;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.CommunityTrustPulseResponseRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The community trust pulse: what residents say about the platform, aggregated per closed month.
 *
 * <p>Deliberately separate from {@link CommunityTrustMetricsService}. Those cards measure what the
 * platform did — closure rate, participation coverage, median resolution. This measures what
 * residents think of it. Merging them into one "trust score" would let the platform grade its own
 * homework, which is the opposite of what a trust metric is for.
 *
 * <p>Three rules, each of which exists because the alternative is a number that misleads:
 *
 * <ul>
 *   <li><b>No average below a minimum sample.</b> Three responses averaging 4.3 is not a community
 *       verdict, and publishing it as one invites a decision on noise. Below the floor the aggregate
 *       reports the count and refuses to report a mean.
 *   <li><b>One response per person per month.</b> Enforced by a unique index. A pulse that counts
 *       the loudest respondent twice is not a measure of the community.
 *   <li><b>Comments are never published verbatim.</b> They are a prompt for a human to read. A
 *       resident writing "the man at number 12 never fixes anything" did not consent to that being
 *       a public dataset row.
 * </ul>
 */
@Service
public class CommunityTrustPulseService {

    public static final String VERSION = "v1";

    /** Below this many responses, no average is reported. */
    static final int MIN_RESPONSES_FOR_AVERAGE = 5;

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;
    private static final int MAX_COMMENT_LENGTH = 1000;

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final CommunityTrustPulseResponseRepository pulseRepository;

    public CommunityTrustPulseService(
        CommunityRepository communityRepository,
        UserRepository userRepository,
        CommunityTrustPulseResponseRepository pulseRepository
    ) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.pulseRepository = pulseRepository;
    }

    public record PulseSubmission(
        UUID communityId,
        String period,
        int trustScore,
        int responsivenessScore,
        int transparencyScore,
        String comment
    ) {}

    /** One dimension's aggregate, with the sample it came from. */
    public record PulseDimension(
        String key,
        String label,
        Double average,
        int responses,
        String note
    ) {}

    public record PulseAggregate(
        String version,
        UUID communityId,
        String communityName,
        String periodKey,
        int responses,
        boolean enoughForAverage,
        List<PulseDimension> dimensions,
        String interpretation,
        LocalDateTime generatedAt
    ) {}

    public record PulseSubmissionResult(
        UUID responseId,
        String periodKey,
        boolean replacedPrevious,
        String message
    ) {}

    /**
     * Records or replaces one resident's answer for a closed month.
     *
     * <p>Replacing rather than appending: a resident changing their mind should update their answer,
     * not stack a second one. The unique index enforces it, and this reports which happened so the
     * caller is not left guessing.
     */
    @Transactional
    public PulseSubmissionResult submit(PulseSubmission submission, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        Community community = communityRepository.findById(submission.communityId())
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + submission.communityId()));

        String periodKey = resolvePeriod(submission.period());
        validateScore("trustScore", submission.trustScore());
        validateScore("responsivenessScore", submission.responsivenessScore());
        validateScore("transparencyScore", submission.transparencyScore());

        String comment = submission.comment() == null ? null : submission.comment().trim();
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException(
                "comment must be at most " + MAX_COMMENT_LENGTH + " characters, but was: " + comment.length());
        }
        if (comment != null && comment.isBlank()) {
            comment = null;
        }

        var existing = pulseRepository.findByCommunityIdAndRespondentIdAndPeriodKey(
            community.getId(), user.getId(), periodKey);

        CommunityTrustPulseResponse response = existing.orElseGet(CommunityTrustPulseResponse::new);
        boolean replaced = existing.isPresent();
        if (!replaced) {
            response.setId(UUID.randomUUID());
            response.setCommunityId(community.getId());
            response.setRespondentId(user.getId());
            response.setPeriodKey(periodKey);
        }
        response.setTrustScore(submission.trustScore());
        response.setResponsivenessScore(submission.responsivenessScore());
        response.setTransparencyScore(submission.transparencyScore());
        response.setComment(comment);
        response.setSubmittedAt(LocalDateTime.now());
        response = pulseRepository.save(response);

        return new PulseSubmissionResult(
            response.getId(),
            periodKey,
            replaced,
            replaced
                ? "Your previous answer for " + periodKey + " was replaced."
                : "Recorded for " + periodKey + "."
        );
    }

    /**
     * The aggregate for one month. Readable by any member, because a community's own opinion of its
     * platform is not privileged information.
     */
    @Transactional(readOnly = true)
    public PulseAggregate aggregate(UUID communityId, String period, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        String periodKey = resolvePeriod(period);
        List<CommunityTrustPulseResponse> responses =
            pulseRepository.findByCommunityIdAndPeriodKey(communityId, periodKey);

        boolean enough = responses.size() >= MIN_RESPONSES_FOR_AVERAGE;

        List<PulseDimension> dimensions = List.of(
            dimension("trust", "Overall trust", responses, CommunityTrustPulseResponse::getTrustScore, enough),
            dimension("responsiveness", "Responsiveness", responses,
                CommunityTrustPulseResponse::getResponsivenessScore, enough),
            dimension("transparency", "Transparency", responses,
                CommunityTrustPulseResponse::getTransparencyScore, enough)
        );

        return new PulseAggregate(
            VERSION,
            communityId,
            community.getName(),
            periodKey,
            responses.size(),
            enough,
            dimensions,
            interpretation(responses.size(), enough),
            LocalDateTime.now()
        );
    }

    /** Every month with responses, newest first, so a trend is visible without a second call. */
    @Transactional(readOnly = true)
    public List<PulseAggregate> history(UUID communityId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        List<String> periods = pulseRepository.findByCommunityIdOrderByPeriodKeyDesc(communityId).stream()
            .map(CommunityTrustPulseResponse::getPeriodKey)
            .distinct()
            .sorted(Comparator.reverseOrder())
            .toList();
        List<PulseAggregate> aggregates = new ArrayList<>();
        for (String period : periods) {
            aggregates.add(aggregate(communityId, period, username));
        }
        return aggregates;
    }

    private PulseDimension dimension(
        String key,
        String label,
        List<CommunityTrustPulseResponse> responses,
        java.util.function.ToIntFunction<CommunityTrustPulseResponse> extractor,
        boolean enough
    ) {
        if (!enough) {
            return new PulseDimension(key, label, null, responses.size(),
                "Not enough responses to report an average. " + responses.size() + " of "
                    + MIN_RESPONSES_FOR_AVERAGE + " needed.");
        }
        double average = responses.stream().mapToInt(extractor).average().orElse(0);
        return new PulseDimension(key, label, round1(average), responses.size(), null);
    }

    /**
     * Says what this is and is not, on every aggregate.
     *
     * <p>A number labelled "trust" invites a reader to treat it as a performance measure. It is a
     * perception measure from a self-selected sample, and the difference matters when someone
     * proposes acting on it.
     */
    private String interpretation(int responses, boolean enough) {
        StringBuilder text = new StringBuilder();
        text.append("This is what residents who chose to answer said about the platform, not a measure ")
            .append("of what the platform did. The computed trust metrics cover that separately. ")
            .append("The sample is self-selected: people with a strong view answer more often, so a low ")
            .append("score may mean dissatisfaction or may mean the people who are content did not reply.");
        if (!enough) {
            text.append(" With ").append(responses).append(" response(s), no average is reported at all: ")
                .append("a mean over a handful of answers is noise, and publishing it as a community ")
                .append("verdict would invite a decision on that noise.");
        }
        text.append(" Comments are never published verbatim; they are read by a person.");
        return text.toString();
    }

    /**
     * Accepts YYYY-MM, or defaults to the previous completed month.
     *
     * <p>Previous rather than current, for the same reason the digest and the transparency report do:
     * on the 3rd of the month the current one is three days of answers.
     */
    private String resolvePeriod(String period) {
        String trimmed = period == null ? "" : period.trim();
        if (trimmed.isEmpty()) {
            return YearMonth.now().minusMonths(1).toString();
        }
        try {
            return YearMonth.parse(trimmed).toString();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                "period must be formatted YYYY-MM, for example 2026-03, but was: " + trimmed);
        }
    }

    private void validateScore(String field, int value) {
        if (value < MIN_SCORE || value > MAX_SCORE) {
            throw new IllegalArgumentException(
                field + " must be between " + MIN_SCORE + " and " + MAX_SCORE + ", but was: " + value);
        }
    }

    private double round1(double value) {
        return java.math.BigDecimal.valueOf(value)
            .setScale(1, java.math.RoundingMode.HALF_UP)
            .doubleValue();
    }

    /** Exposed for the response so a caller can see the floor without reading the source. */
    public int minimumResponsesForAverage() {
        return MIN_RESPONSES_FOR_AVERAGE;
    }
}