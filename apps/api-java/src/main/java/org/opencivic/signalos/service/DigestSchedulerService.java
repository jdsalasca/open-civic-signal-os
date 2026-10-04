package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.DigestScheduleRun;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.DigestScheduleRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The weekly digest scheduler.
 *
 * <p>The decision this exists to make explicit: <b>the scheduler prepares, it does not publish.</b>
 *
 * <p>A scheduler that sent bulletins on its own would mean a wrong digest reaches residents with
 * nobody accountable for it. That is the question I recorded as the blocker when delivery shipped,
 * and this is the answer rather than a deferral: the job generates the digest for the previous
 * completed week, records that it is ready, and stops. Publishing remains a deliberate act by a
 * person who can be asked why.
 *
 * <p>Idempotent per community per week, enforced by a unique index. A scheduler that fires twice, or
 * a restart mid-week, must not produce two records for the same week and make it look like two
 * attempts happened.
 *
 * <p>Disabled by default. A job that starts generating digests the moment it is deployed would
 * surprise a community that has not decided to run a weekly bulletin yet.
 */
@Service
public class DigestSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(DigestSchedulerService.class);

    private final CommunityRepository communityRepository;
    private final WeeklyDigestService digestService;
    private final DigestScheduleRunRepository runRepository;

    @Value("${app.digest.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    public DigestSchedulerService(
        CommunityRepository communityRepository,
        WeeklyDigestService digestService,
        DigestScheduleRunRepository runRepository
    ) {
        this.communityRepository = communityRepository;
        this.digestService = digestService;
        this.runRepository = runRepository;
    }

    /**
     * Whether the weekly job is enabled.
     *
     * <p>Exposed so a test can assert on the value the application actually resolved, rather than on
     * a property read from a fresh environment that has none of the application's sources.
     */
    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public record ScheduleRunView(
        UUID communityId,
        String communityName,
        String weekKey,
        String outcome,
        String detail,
        LocalDateTime ranAt
    ) {}

    public record ScheduleReport(
        String version,
        String weekKey,
        int communitiesConsidered,
        int prepared,
        int skipped,
        int failed,
        List<ScheduleRunView> runs,
        String interpretation,
        LocalDateTime generatedAt
    ) {}

    /**
     * Runs weekly. Monday morning, for the week that just ended.
     *
     * <p>Monday rather than Sunday night: a digest generated at 23:59 on Sunday would race the last
     * reports of the week, and a report filed at 23:58 would land in the wrong bulletin.
     */
    @Scheduled(cron = "${app.digest.scheduler.cron:0 0 6 * * MON}")
    public void runWeekly() {
        if (!schedulerEnabled) {
            log.debug("Digest scheduler is disabled; skipping the weekly run.");
            return;
        }
        try {
            ScheduleReport report = prepareForAllCommunities(null);
            log.info("Digest scheduler prepared {} digest(s) for {}.",
                report.prepared(), report.weekKey());
        } catch (RuntimeException ex) {
            // A scheduler that throws into the void leaves no trace of why nothing happened.
            log.error("Digest scheduler run failed: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Prepares the digest for every community, or one when a community is given.
     *
     * <p>Exposed so an operator can run it deliberately, and so a test can exercise it without
     * waiting for a cron.
     */
    @Transactional
    public ScheduleReport prepareForAllCommunities(UUID onlyCommunityId) {
        return prepareForAllCommunities(onlyCommunityId, null);
    }

    /**
     * Prepares an explicit week, or the previous completed one when the week is null.
     *
     * <p>The explicit week exists because "the week that just ended" is only right for a weekly cron.
     * A community that closes its books late, or an operator preparing a week by hand, needs to name
     * the week rather than be given whatever the calendar currently says.
     */
    @Transactional
    public ScheduleReport prepareForAllCommunities(UUID onlyCommunityId, String requestedWeekKey) {
        String weekKey = digestService.resolveWeek(requestedWeekKey).key();
        List<Community> communities = onlyCommunityId == null
            ? communityRepository.findAll()
            : communityRepository.findById(onlyCommunityId).map(List::of).orElse(List.of());

        List<ScheduleRunView> runs = new ArrayList<>();
        int prepared = 0;
        int skipped = 0;
        int failed = 0;

        for (Community community : communities) {
            // One run per community per week. A restart mid-week must not look like two attempts.
            if (runRepository.findByCommunityIdAndWeekKey(community.getId(), weekKey).isPresent()) {
                runs.add(new ScheduleRunView(
                    community.getId(), community.getName(), weekKey, "SKIPPED",
                    "Already prepared for this week.", LocalDateTime.now()));
                skipped++;
                continue;
            }

            DigestScheduleRun run = new DigestScheduleRun();
            run.setId(UUID.randomUUID());
            run.setCommunityId(community.getId());
            run.setWeekKey(weekKey);
            run.setRanAt(LocalDateTime.now());

            try {
                // Generating is the whole job, and the generated digest is kept. It used to be
                // discarded here and recomposed at publish time, which meant a coordinator could
                // review one digest and residents received another: a rescored signal between the two
                // moved the score, the order and the body. "Prepared" has to mean the artifact exists.
                var digest = digestService.prepare(community.getId(), weekKey, null);
                run.setOutcome(DigestScheduleRun.Outcome.PREPARED);
                run.setDetail(digest.isPresent()
                    ? "Digest sealed and waiting for a person to publish: "
                        + digest.get().getItemCount() + " item(s), hash "
                        + digest.get().getContentHash().substring(0, 12)
                        + ". Publishing sends this exact artifact; it is not recomposed."
                    : "Already prepared for this week; the existing artifact was kept.");
                prepared++;
            } catch (RuntimeException ex) {
                run.setOutcome(DigestScheduleRun.Outcome.FAILED);
                run.setDetail(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                failed++;
            }

            runRepository.save(run);
            runs.add(new ScheduleRunView(
                community.getId(), community.getName(), weekKey,
                run.getOutcome().name(), run.getDetail(), run.getRanAt()));
        }

        return new ScheduleReport(
            "v1",
            weekKey,
            communities.size(),
            prepared,
            skipped,
            failed,
            runs,
            interpretation(prepared, skipped, failed),
            LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public List<ScheduleRunView> history(UUID communityId) {
        return runRepository.findByCommunityIdOrderByWeekKeyDesc(communityId).stream()
            .map(run -> new ScheduleRunView(
                run.getCommunityId(), null, run.getWeekKey(),
                run.getOutcome().name(), run.getDetail(), run.getRanAt()))
            .toList();
    }

    /**
     * Says what the scheduler did and, more importantly, what it did not do.
     *
     * <p>A reader seeing "PREPARED" could wrongly conclude a bulletin went out. It did not: it is
     * waiting for a person.
     */
    private String interpretation(int prepared, int skipped, int failed) {
        StringBuilder text = new StringBuilder();
        text.append("The scheduler PREPARES digests; it does not publish them. ")
            .append(prepared).append(" digest(s) are ready and waiting for a person to publish, ")
            .append(skipped).append(" were already prepared, and ").append(failed)
            .append(" could not be generated. ")
            .append("Nothing has reached residents. A scheduler that sent bulletins on its own would ")
            .append("mean a wrong digest reaches people with nobody accountable for it, so publishing ")
            .append("remains a deliberate act.");
        if (failed > 0) {
            text.append(" The failed runs carry their reason; a failure here means a community gets no ")
                .append("bulletin this week unless someone looks.");
        }
        return text.toString();
    }

    /**
     * The scheduler acts as the platform, not as a member.
     *
     * <p>It uses {@code buildDigestForScheduler}, which skips the membership check because there is
     * no user to check. The run record is the audit trail instead of a username.
     */
}