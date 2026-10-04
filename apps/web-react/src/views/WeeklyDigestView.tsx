import { useCallback, useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { InputText } from "primereact/inputtext";
import { Layout } from "../components/Layout";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicField } from "../components/ui/CivicField";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicSkeleton } from "../components/ui/CivicSkeleton";
import { useCommunityStore } from "../store/useCommunityStore";
import type { DigestScheduleRun, WeeklyDigest } from "../types";

type ApiError = Error & { friendlyMessage?: string };

/**
 * The weekly digest, as a bulletin rather than a widget.
 *
 * The dashboard sidebar of the same name sorts the signals already on screen and shows the top
 * three. That is a dashboard widget, not the digest: it does not know the week, it does not carry
 * the counts, and it cannot be published.
 *
 * Two things this screen must make obvious:
 *
 *   - **Whether the week has been published.** A prepared digest that nobody published is the
 *     failure mode the scheduler deliberately leaves to a person, and it should be visible rather
 *     than discovered on Friday.
 *   - **What the scheduler did.** The history shows prepared, skipped and failed runs, because a
 *     failed run means a community gets no bulletin that week unless someone looks.
 */
export function WeeklyDigestView() {
  const { t } = useTranslation();
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership =
    memberships.find((membership) => membership.communityId === activeCommunityId) ??
    memberships[0] ??
    null;
  const selectedCommunityId = activeCommunityId ?? activeMembership?.communityId ?? null;

  const [week, setWeek] = useState("");
  const [digest, setDigest] = useState<WeeklyDigest | null>(null);
  const [history, setHistory] = useState<DigestScheduleRun[]>([]);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const [publishing, setPublishing] = useState(false);

  const load = useCallback(async () => {
    if (!selectedCommunityId) {
      setDigest(null);
      setHistory([]);
      return;
    }
    setLoading(true);
    setFailed(false);
    try {
      const weekParam = week.trim() ? `&week=${encodeURIComponent(week.trim())}` : "";
      const [digestRes, historyRes] = await Promise.all([
        apiClient.get<WeeklyDigest>(
          `community/weekly-digest?communityId=${selectedCommunityId}${weekParam}`,
        ),
        apiClient.get<DigestScheduleRun[]>(
          `community/weekly-digest/schedule-history?communityId=${selectedCommunityId}`,
        ),
      ]);
      setDigest(digestRes.data);
      setHistory(historyRes.data ?? []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("weekly_digest.load_error"));
      setFailed(true);
      setDigest(null);
    } finally {
      setLoading(false);
    }
  }, [selectedCommunityId, week, t]);

  useEffect(() => {
    load();
  }, [load]);

  const publish = async () => {
    if (!selectedCommunityId || !digest) {
      return;
    }
    setPublishing(true);
    try {
      await apiClient.post(
        `community/weekly-digest/publish?communityId=${selectedCommunityId}&week=${digest.week.key}`,
      );
      toast.success(t("weekly_digest.published_toast"));
      await load();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("weekly_digest.publish_error"));
    } finally {
      setPublishing(false);
    }
  };

  return (
    <Layout>
      <div className="weekly-digest-shell p-3 md:p-5" data-testid="weekly-digest-page">
        <CivicPageHeader
          title={t("weekly_digest.title")}
          description={t("weekly_digest.subtitle")}
          // Not the default "what needs attention today": this screen is the record of a week that
          // already closed, and an eyebrow promising something urgent misdescribes it.
          eyebrow={t("weekly_digest.eyebrow")}
        />

        <CivicField label={t("weekly_digest.week_label")} helpText={t("weekly_digest.week_help")}>
          {/* InputText like every other field in the app. A raw <input> rendered as an unstyled box. */}
          <InputText
            className="w-full"
            type="text"
            placeholder="2026-W13"
            value={week}
            onChange={(event) => setWeek(event.target.value)}
            data-testid="weekly-digest-week-input"
          />
        </CivicField>

        {loading ? (
          <CivicSkeleton type="table-row" count={6} />
        ) : failed ? (
          <CivicEmptyState
            icon="pi pi-exclamation-triangle"
            title={t("weekly_digest.unavailable_title")}
            description={t("weekly_digest.unavailable_desc")}
            data-testid="weekly-digest-unavailable"
          />
        ) : !digest ? (
          <CivicEmptyState
            icon="pi pi-inbox"
            title={t("weekly_digest.empty_title")}
            description={t("weekly_digest.empty_desc")}
            data-testid="weekly-digest-empty"
          />
        ) : (
          <>
            <div className="flex flex-wrap align-items-center gap-2 mb-4">
              <CivicBadge
                label={t("weekly_digest.week_badge", { week: digest.week.key })}
                severity="neutral"
                data-testid="weekly-digest-week-badge"
              />
              {/*
                Whether the week is published is the first thing a reader needs. A prepared digest
                nobody published is the failure mode the scheduler leaves to a person.
              */}
              <CivicBadge
                label={
                  digest.published
                    ? t("weekly_digest.published_badge")
                    : t("weekly_digest.unpublished_badge")
                }
                severity={digest.published ? "resolved" : "progress"}
                data-testid="weekly-digest-published-badge"
              />
              {digest.published && (
                <CivicBadge
                  label={t("weekly_digest.delivered_badge", { count: digest.deliveredToChannels })}
                  severity="neutral"
                  data-testid="weekly-digest-delivered-badge"
                />
              )}
            </div>

            <div className="grid mb-4">
              <div className="col-12 md:col-3">
                <CivicCard padding="md" data-testid="weekly-digest-reported">
                  <div className="text-2xl font-bold">{digest.reportedThisWeek}</div>
                  <div className="text-xs text-secondary">{t("weekly_digest.reported")}</div>
                </CivicCard>
              </div>
              <div className="col-12 md:col-3">
                <CivicCard padding="md" data-testid="weekly-digest-resolved">
                  <div className="text-2xl font-bold">{digest.resolvedThisWeek}</div>
                  <div className="text-xs text-secondary">{t("weekly_digest.resolved")}</div>
                </CivicCard>
              </div>
              <div className="col-12 md:col-3">
                <CivicCard padding="md" data-testid="weekly-digest-rejected">
                  <div className="text-2xl font-bold">{digest.rejectedThisWeek}</div>
                  <div className="text-xs text-secondary">{t("weekly_digest.rejected")}</div>
                </CivicCard>
              </div>
              <div className="col-12 md:col-3">
                <CivicCard padding="md" data-testid="weekly-digest-open">
                  <div className="text-2xl font-bold">{digest.stillOpenTotal}</div>
                  <div className="text-xs text-secondary">{t("weekly_digest.still_open")}</div>
                </CivicCard>
              </div>
            </div>

            <CivicCard
              title={t("weekly_digest.body_title")}
              className="mb-4"
              data-testid="weekly-digest-body-card"
            >
              {/* The rendered body, exactly as a channel would receive it. Width capped: this is
                  markdown meant to be read, and at full app width the measure runs past what a line
                  of text can carry. */}
              <pre
                className="text-sm white-space-pre-wrap m-0 max-w-3xl"
                data-testid="weekly-digest-body"
              >
                {digest.body}
              </pre>
              <div className="text-xs text-secondary mt-3" data-testid="weekly-digest-hash">
                {t("weekly_digest.hash_note", { hash: digest.contentHash.slice(0, 16) })}
              </div>
            </CivicCard>

            {!digest.published && (
              <div className="mb-4">
                <CivicButton
                  label={t("weekly_digest.publish")}
                  onClick={publish}
                  disabled={publishing}
                  data-testid="weekly-digest-publish"
                />
                {/* Stated on the control, because publishing reaches residents. */}
                <div className="text-xs text-secondary mt-2">
                  {t("weekly_digest.publish_warning")}
                </div>
              </div>
            )}

            <CivicCard title={t("weekly_digest.schedule_title")} data-testid="weekly-digest-schedule">
              {history.length === 0 ? (
                <div className="text-sm text-secondary" data-testid="weekly-digest-schedule-empty">
                  {t("weekly_digest.schedule_empty")}
                </div>
              ) : (
                <ul className="list-none p-0 m-0" data-testid="weekly-digest-schedule-list">
                  {history.map((run) => (
                    <li
                      key={`${run.weekKey}-${run.ranAt}`}
                      className="py-2 border-top-1 border-surface-soft text-sm"
                      data-testid={`weekly-digest-run-${run.weekKey}`}
                    >
                      <span className="font-semibold">{run.weekKey}</span> ·{" "}
                      {t(`weekly_digest.outcome_${run.outcome}`)}
                      {run.detail ? ` · ${run.detail}` : ""}
                    </li>
                  ))}
                </ul>
              )}
            </CivicCard>
          </>
        )}
      </div>
    </Layout>
  );
}