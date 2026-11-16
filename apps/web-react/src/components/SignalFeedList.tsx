import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { InputText } from "primereact/inputtext";
import { useTranslation } from "react-i18next";
import { Signal } from "../types";
import { PriorityScale, SignalEmpty, StatusChip } from "./signalPrimitives";
import { CivicButton } from "./ui/CivicButton";
import { LinkifiedText } from "./ui/LinkifiedText";
import { useSignalStatusLabel } from "../utils/signalStatus";
import { formatStamp } from "../utils/formatStamp";

type Props = {
  signals: Signal[];
  loading: boolean;
  totalRecords?: number;
  rows?: number;
  first?: number;
  onPage?: (event: { first: number; rows: number }) => void;
  criticalThreshold?: number;
  error?: boolean;
  onRetry?: () => void;
};

/**
 * The neighbourhood feed: one readable card per report instead of a wide data table.
 *
 * <p>Citizens read this on a phone, so the card carries the whole story in one column: what
 * happened, where, how serious it is and one link to the case. The DataTable stays where it belongs
 * (the staff list) and is no longer downloaded by every visitor.
 */
export function SignalFeedList({
  signals,
  loading,
  totalRecords,
  rows = 10,
  first = 0,
  onPage,
  criticalThreshold = 0,
  error = false,
  onRetry,
}: Props) {
  const { t } = useTranslation();
  const statusLabel = useSignalStatusLabel();
  const [query, setQuery] = useState("");

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return signals;
    return signals.filter((signal) =>
      [signal.title, signal.category, signal.status, signal.locationLabel]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(needle))
    );
  }, [signals, query]);

  const total = totalRecords ?? 0;
  const activeTotal = query.trim() ? filtered.length : total;
  const page = Math.floor(first / rows) + 1;
  const pageCount = Math.max(1, Math.ceil(total / rows));
  const hasPager = !query.trim() && total > rows && !loading;

  const preview = (signal: Signal) => {
    const factors = signal.explainabilitySummary?.topFactors ?? [];
    if (signal.explainabilitySummary?.summary) return signal.explainabilitySummary.summary;
    if (factors.length === 0) return t("signals.explanation_unavailable");
    if (factors.length > 1) {
      return t("signals.why_ranked_preview", {
        primary: t(`signals.factor_keys.${factors[0].key}`),
        secondary: t(`signals.factor_keys.${factors[1].key}`),
      });
    }
    if (factors.length === 1) {
      return t("signals.why_ranked_preview_single", {
        primary: t(`signals.factor_keys.${factors[0].key}`),
      });
    }
    return t("signals.why_ranked_preview_single", {
      primary: t("signals.factor_keys.urgency"),
    });
  };
  return (
    <section className="signal-feed" data-testid="signals-feed">
      <header className="signal-feed-header">
        <div className="signal-feed-header__copy">
          <span className="signal-feed-header__icon" aria-hidden="true">
            <i className="pi pi-list" />
          </span>
          <div className="flex flex-column gap-1 min-w-0">
            <span className="u-eyebrow m-0">{t("dashboard.feed_title")}</span>
            <span className="signal-feed-header__count">
              {t("signals.showing_count", { count: filtered.length, total: activeTotal })}
            </span>
            {query.trim() && (
              <span className="signal-feed-header__note">{t("signals.page_search_note")}</span>
            )}
          </div>
        </div>
        <div className="app-search-shell w-full md:w-auto" style={{ maxWidth: "20rem" }}>
          <i className="pi pi-search app-search-icon" aria-hidden="true" />
          <InputText
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={t("dashboard.search_placeholder")}
            className="app-search-input"
            aria-label={t("dashboard.search_placeholder")}
          />
        </div>
      </header>

      {error ? (
        <div className="signal-feed-error" role="alert">
          <i className="pi pi-exclamation-circle" aria-hidden="true" />
          <div className="signal-feed-error__copy">
            <strong>{t("dashboard.sync_error")}</strong>
            <span>{t("dashboard.sync_error_hint")}</span>
          </div>
          <CivicButton
            type="button"
            variant="secondary"
            size="small"
            label={t("common.retry")}
            onClick={onRetry}
            data-testid="signals-feed-retry"
          />
        </div>
      ) : loading ? (
        <div className="signal-feed-list" aria-hidden="true">
          {[0, 1, 2, 3].map((row) => (
            <div key={row} className="signal-feed-card is-skeleton" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="signal-feed-empty">
          <SignalEmpty
            icon="pi pi-inbox"
            title={t("signals.no_results_title")}
            description={t("signals.no_results_desc")}
            testId="signals-empty-state"
          />
        </div>
      ) : (
        <ul className="signal-feed-list">
          {filtered.map((signal) => (
            <li key={signal.id}>
              <Link
                to={`/signal/${signal.id}`}
                className="signal-feed-card"
                data-testid={`signal-feed-card-${signal.id}`}
              >
                <div className="signal-feed-card__top">
                  <h3 className="signal-feed-card__title">{signal.title}</h3>
                  <span className="signal-feed-card__cta">
                    {t("signals.view_details")}
                    <i className="pi pi-arrow-right" aria-hidden="true" />
                  </span>
                </div>
                <p className="signal-feed-card__summary">
                  <LinkifiedText text={preview(signal)} />
                </p>
                <div className="signal-feed-card__meta">
                  <span className="signal-status-chip" data-status={t(`categories.${signal.category}`)}>
                    <span className="signal-status-chip__dot" aria-hidden="true" style={{ background: "var(--brand-accent)" }} />
                    <span>{t(`categories.${signal.category}`)}</span>
                  </span>
                  <StatusChip status={signal.status} label={statusLabel(signal.status)} />
                  <PriorityScale
                    score={signal.priorityScore}
                    threshold={criticalThreshold}
                    compact
                    testId={`signal-explainability-${signal.id}`}
                  />
                  {signal.createdAt && (
                    <span className="signal-feed-card__date">{formatStamp(signal.createdAt)}</span>
                  )}
                  {signal.locationLabel && (
                    <span className="signal-feed-card__place">
                      <i className="pi pi-map-marker" aria-hidden="true" />
                      {signal.locationLabel}
                    </span>
                  )}
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {hasPager && (
        <nav className="signal-feed-pager" aria-label={t("signals.pagination_label")}>
          <CivicButton
            type="button"
            variant="ghost"
            size="small"
            icon="pi pi-arrow-left"
            label={t("signals.previous_page")}
            disabled={first === 0}
            onClick={() => onPage?.({ first: Math.max(0, first - rows), rows })}
          />
          <span className="signal-feed-pager__status">
            {t("signals.page_of", { page, total: pageCount })}
          </span>
          <CivicButton
            type="button"
            variant="ghost"
            size="small"
            icon="pi pi-arrow-right"
            iconPos="right"
            label={t("signals.next_page")}
            disabled={first + rows >= total}
            onClick={() => onPage?.({ first: first + rows, rows })}
          />
        </nav>
      )}
    </section>
  );
}
