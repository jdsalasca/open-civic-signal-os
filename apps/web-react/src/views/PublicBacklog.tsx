import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Layout } from "../components/Layout";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicSkeleton } from "../components/ui/CivicSkeleton";
import { PriorityScale, StatusChip } from "../components/signalPrimitives";
import apiClient from "../api/axios";
import type { PrioritizationFormula, Signal, SignalMeta } from "../types";
import { formatStamp } from "../utils/formatStamp";
import { useSignalStatusLabel } from "../utils/signalStatus";

/**
 * The backlog, readable without an account.
 *
 * AGENTS.md says inclusion is a default and dashboards must be readable in low-bandwidth
 * scenarios. Both point the same way here: the ranked backlog is already public at the API,
 * so requiring a login to *look at* it was the platform asking people to register before they
 * could see whether it was worth registering for.
 *
 * Two things this page must expose, because a public ranking without them is a black box:
 *   - the formula behind the order, and
 *   - when the data was last updated.
 *
 * It deliberately calls only the endpoints that are public. A page reachable without a session
 * that fired an authenticated request would render a login prompt to a stranger, which is the
 * opposite of the point.
 */
export function PublicBacklog() {
  const { t } = useTranslation();
  const statusLabel = useSignalStatusLabel();
  const [signals, setSignals] = useState<Signal[]>([]);
  const [meta, setMeta] = useState<SignalMeta | null>(null);
  const [formula, setFormula] = useState<PrioritizationFormula | null>(null);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      try {
        setLoading(true);
        setFailed(false);
        // Only permitAll endpoints. No auth header, no session, no redirect.
        const [signalsRes, metaRes, formulaRes] = await Promise.all([
          apiClient.get("signals/prioritized?page=0&size=20", { signal: controller.signal }),
          apiClient.get("signals/meta", { signal: controller.signal }),
          apiClient.get("signals/formula", { signal: controller.signal }),
        ]);
        setSignals(signalsRes.data?.content ?? []);
        setMeta(metaRes.data ?? null);
        setFormula(formulaRes.data ?? null);
      } catch {
        if (!controller.signal.aborted) {
          setFailed(true);
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    load();
    return () => controller.abort();
  }, []);

  const lastUpdated = formatStamp(meta?.lastUpdatedAt) ?? t('public_backlog.freshness_pending');

  return (
    <Layout authMode>
      <div className="public-backlog-shell p-3 md:p-5">
        <CivicPageHeader
          title={t('public_backlog.title')}
          description={t('public_backlog.subtitle')}
        />

        <div className="flex flex-wrap align-items-center gap-2 mb-4">
          <CivicBadge
            label={t('public_backlog.freshness', { when: lastUpdated })}
            severity="neutral"
            data-testid="public-backlog-freshness"
            title={t('public_backlog.freshness_hint')}
          />
          {meta && (
            <CivicBadge
              label={t('public_backlog.open_count', { count: meta.unresolvedSignals })}
              severity="progress"
              data-testid="public-backlog-open-count"
            />
          )}
        </div>

        {/*
          The formula is shown before the list on purpose. A resident deciding whether to take
          this ranking seriously should be able to read the rule before reading the result.
        */}
        {formula && (
          <CivicCard className="mb-4" data-testid="public-backlog-formula">
            <div className="text-sm">
              <div className="font-semibold mb-1">{t('public_backlog.formula_title')}</div>
              <code className="text-xs break-words">{formula.formula}</code>
              <div className="text-secondary mt-1 text-xs">
                {t('public_backlog.formula_version', { version: formula.version })}
              </div>
            </div>
          </CivicCard>
        )}

        {loading ? (
          <CivicSkeleton type="table-row" count={8} />
        ) : failed ? (
          <CivicEmptyState
            icon="pi pi-exclamation-triangle"
            title={t('public_backlog.unavailable_title')}
            description={t('public_backlog.unavailable_desc')}
            data-testid="public-backlog-unavailable"
          />
        ) : signals.length === 0 ? (
          <CivicEmptyState
            icon="pi pi-inbox"
            title={t('public_backlog.empty_title')}
            description={t('public_backlog.empty_desc')}
            data-testid="public-backlog-empty"
          />
        ) : (
          <ol className="list-none p-0 m-0 flex flex-column gap-3" data-testid="public-backlog-list">
            {signals.map((signal, index) => (
              <li key={signal.id}>
                <article className="signal-backlog-item" data-testid={`public-backlog-item-${index + 1}`}>
                  <div className="signal-backlog-item__rank" aria-hidden="true">
                    {index + 1}
                  </div>
                  <div className="signal-backlog-item__copy">
                    <div className="signal-backlog-item__title">{signal.title}</div>
                    <div className="signal-backlog-item__meta">
                      <span className="signal-status-chip" data-status={signal.category}>
                        <span className="signal-status-chip__dot" aria-hidden="true" style={{ background: "var(--chart-4)" }} />
                        <span>{t(`categories.${signal.category}`)}</span>
                      </span>
                      {signal.locationLabel && (
                        <span className="signal-status-chip">
                          <i className="pi pi-map-marker text-xs" aria-hidden="true"></i>
                          <span>{signal.locationLabel}</span>
                        </span>
                      )}
                      <StatusChip
                        status={signal.status}
                        label={t("public_backlog.status", { status: statusLabel(signal.status) })}
                      />
                    </div>
                    {/*
                      AGENTS.md: every list exposes why an item is ranked where it is. The terms
                      are the ones the published formula uses, so this is checkable rather than a
                      claim about the score.
                    */}
                    {signal.scoreBreakdown && (
                      <div
                        className="signal-backlog-item__why"
                        data-testid={`public-backlog-why-${index + 1}`}
                      >
                        {t('public_backlog.why', {
                          urgency: signal.scoreBreakdown.urgency,
                          impact: signal.scoreBreakdown.impact,
                          people: signal.scoreBreakdown.affectedPeople,
                          votes: signal.scoreBreakdown.communityVotes,
                        })}
                      </div>
                    )}
                    {signal.explainabilitySummary?.summary && (
                      <div className="signal-backlog-item__why">
                        {signal.explainabilitySummary.summary}
                      </div>
                    )}
                  </div>
                  <div className="signal-backlog-item__score">
                    <div
                      className="signal-backlog-item__score-value"
                      data-testid={`public-backlog-score-${index + 1}`}
                    >
                      {signal.priorityScore}
                    </div>
                    <div className="signal-backlog-item__score-label">
                      {t('public_backlog.score_label')}
                    </div>
                    <PriorityScale score={signal.priorityScore} compact showLabel={false} />
                  </div>
                </article>
              </li>
            ))}
          </ol>
        )}

        <div className="mt-5 text-center text-sm" data-testid="public-backlog-cta">
          {t('public_backlog.cta')}
        </div>
      </div>
    </Layout>
  );
}
