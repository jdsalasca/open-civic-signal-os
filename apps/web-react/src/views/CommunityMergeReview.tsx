import { useCallback, useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { Layout } from "../components/Layout";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicField } from "../components/ui/CivicField";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicSkeleton } from "../components/ui/CivicSkeleton";
import { useCommunityStore } from "../store/useCommunityStore";
import type { SignalMergeReview, SignalMergeSuggestion } from "../types";

type ApiError = Error & { friendlyMessage?: string };

/**
 * Duplicate review: the algorithm suggests, a person decides.
 *
 * Two things this screen must not do, both taken from the API's own design:
 *
 *   - It must not merge anything on its own. The score and the candidate list are shown so a
 *     moderator can judge, not so the screen can act.
 *   - It must send back **exactly the candidates it displayed**. The backend stores that payload
 *     verbatim as the record of what was decided, so recomputing or trimming it here would make the
 *     audit trail describe something the reviewer never saw.
 */
export function CommunityMergeReview() {
  const { t } = useTranslation();
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership =
    memberships.find((membership) => membership.communityId === activeCommunityId) ??
    memberships[0] ??
    null;
  const selectedCommunityId = activeCommunityId ?? activeMembership?.communityId ?? null;

  const [suggestions, setSuggestions] = useState<SignalMergeSuggestion[]>([]);
  const [history, setHistory] = useState<SignalMergeReview[]>([]);
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [busyId, setBusyId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);

  const load = useCallback(async () => {
    if (!selectedCommunityId) {
      setSuggestions([]);
      setHistory([]);
      return;
    }
    setLoading(true);
    setFailed(false);
    try {
      const [suggestionsRes, historyRes] = await Promise.all([
        apiClient.get<SignalMergeSuggestion[]>(
          `signals/merge-review/suggestions?communityId=${selectedCommunityId}`,
        ),
        apiClient.get<SignalMergeReview[]>(
          `signals/merge-review/history?communityId=${selectedCommunityId}`,
        ),
      ]);
      setSuggestions(suggestionsRes.data ?? []);
      setHistory(historyRes.data ?? []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("merge_review.load_error"));
      setFailed(true);
      setSuggestions([]);
      setHistory([]);
    } finally {
      setLoading(false);
    }
  }, [selectedCommunityId, t]);

  useEffect(() => {
    load();
  }, [load]);

  const decide = async (suggestion: SignalMergeSuggestion, approve: boolean) => {
    if (!selectedCommunityId) {
      return;
    }
    setBusyId(suggestion.targetSignalId);
    try {
      await apiClient.post(
        `signals/merge-review/decisions?communityId=${selectedCommunityId}`,
        {
          targetSignalId: suggestion.targetSignalId,
          decision: approve ? "APPROVED" : "REJECTED",
          // The candidates exactly as displayed. The backend stores them verbatim as the record of
          // what the decision was based on.
          suggestedSimilarities: suggestion.candidates,
          threshold: suggestion.threshold,
          note: notes[suggestion.targetSignalId] ?? null,
        },
      );
      toast.success(
        approve ? t("merge_review.approved_toast") : t("merge_review.rejected_toast"),
      );
      setNotes((prev) => ({ ...prev, [suggestion.targetSignalId]: "" }));
      await load();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("merge_review.decide_error"));
    } finally {
      setBusyId(null);
    }
  };

  const formatSimilarity = (value: number | null) =>
    value === null ? t("merge_review.similarity_unknown") : value.toFixed(2);

  return (
    <Layout>
      <div className="merge-review-shell p-3 md:p-5" data-testid="merge-review-page">
        <CivicPageHeader
          title={t("merge_review.title")}
          description={t("merge_review.subtitle")}
        />

        {loading ? (
          <CivicSkeleton type="table-row" count={6} />
        ) : failed ? (
          <CivicEmptyState
            icon="pi pi-exclamation-triangle"
            title={t("merge_review.unavailable_title")}
            description={t("merge_review.unavailable_desc")}
            data-testid="merge-review-unavailable"
          />
        ) : suggestions.length === 0 ? (
          <CivicEmptyState
            icon="pi pi-check-circle"
            title={t("merge_review.empty_title")}
            description={t("merge_review.empty_desc")}
            data-testid="merge-review-empty"
          />
        ) : (
          <div className="flex flex-column gap-4" data-testid="merge-review-list">
            {suggestions.map((suggestion, index) => (
              <CivicCard
                key={suggestion.targetSignalId}
                padding="lg"
                data-testid={`merge-suggestion-${index}`}
              >
                <div className="flex justify-content-between align-items-start gap-3 mb-3">
                  <div>
                    <div className="font-semibold" data-testid={`merge-target-title-${index}`}>
                      {suggestion.targetTitle}
                    </div>
                    <div className="text-xs text-secondary">
                      {t("merge_review.target_meta", {
                        category: suggestion.targetCategory,
                        threshold: suggestion.threshold,
                      })}
                    </div>
                  </div>
                  {suggestion.latestDecision && (
                    /* A suggestion already answered stays visible, carrying its decision, rather
                       than vanishing and making the queue look permanently unfinished. */
                    <CivicBadge
                      label={t(`merge_review.decision_${suggestion.latestDecision}`)}
                      severity={suggestion.latestDecision === "APPROVED" ? "resolved" : "rejected"}
                      data-testid={`merge-latest-decision-${index}`}
                    />
                  )}
                </div>

                <ul className="list-none p-0 m-0 mb-3" data-testid={`merge-candidates-${index}`}>
                  {suggestion.candidates.map((candidate) => (
                    <li
                      key={candidate.signalId}
                      className="flex justify-content-between align-items-center py-2 border-top-1 border-surface-soft"
                      data-testid={`merge-candidate-${candidate.signalId}`}
                    >
                      <div>
                        <div className="text-sm">{candidate.title}</div>
                        <div className="text-xs text-secondary">
                          {candidate.category} · {candidate.status}
                        </div>
                      </div>
                      {/* The score travels with the suggestion: a reviewer deciding whether two
                          reports are the same complaint needs to see why the platform thinks so. */}
                      <CivicBadge
                        label={formatSimilarity(candidate.similarity)}
                        severity="neutral"
                        data-testid={`merge-similarity-${candidate.signalId}`}
                      />
                    </li>
                  ))}
                </ul>

                <CivicField
                  label={t("merge_review.note_label")}
                  helpText={t("merge_review.note_help")}
                >
                  <textarea
                    className="w-full"
                    rows={2}
                    value={notes[suggestion.targetSignalId] ?? ""}
                    onChange={(event) =>
                      setNotes((prev) => ({
                        ...prev,
                        [suggestion.targetSignalId]: event.target.value,
                      }))
                    }
                    data-testid={`merge-note-${index}`}
                  />
                </CivicField>

                <div className="flex gap-2 mt-3">
                  <CivicButton
                    label={t("merge_review.approve")}
                    onClick={() => decide(suggestion, true)}
                    disabled={busyId === suggestion.targetSignalId}
                    data-testid={`merge-approve-${index}`}
                  />
                  <CivicButton
                    label={t("merge_review.reject")}
                    variant="secondary"
                    onClick={() => decide(suggestion, false)}
                    disabled={busyId === suggestion.targetSignalId}
                    data-testid={`merge-reject-${index}`}
                  />
                </div>
                {/* Stated on the control itself, because approving is the destructive direction. */}
                <div className="text-xs text-secondary mt-2">
                  {t("merge_review.approve_warning")}
                </div>
              </CivicCard>
            ))}
          </div>
        )}

        <div className="mt-6">
          <h2 className="text-lg">{t("merge_review.history_title")}</h2>
          {history.length === 0 ? (
            <CivicEmptyState
              icon="pi pi-history"
              title={t("merge_review.history_empty_title")}
              description={t("merge_review.history_empty_desc")}
              data-testid="merge-review-history-empty"
            />
          ) : (
            <ul className="list-none p-0 m-0" data-testid="merge-review-history">
              {history.map((entry) => (
                <li
                  key={entry.decisionId}
                  className="py-2 border-top-1 border-surface-soft text-sm"
                  data-testid={`merge-history-${entry.decisionId}`}
                >
                  <span className="font-semibold">
                    {t(`merge_review.decision_${entry.decision}`)}
                  </span>{" "}
                  · {entry.suggestedCount} {t("merge_review.candidates_label")}
                  {entry.note ? ` · ${entry.note}` : ""}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </Layout>
  );
}