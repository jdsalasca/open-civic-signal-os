import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { toast } from "react-hot-toast";
import apiClient from "../../api/axios";
import { CivicBadge } from "../ui/CivicBadge";
import { CivicButton } from "../ui/CivicButton";
import { CivicCard } from "../ui/CivicCard";
import type { HelpCenterResponse, HelpCenterStateResponse, HelpGuide, HelpSurface } from "../../types";

type Props = {
  surface: HelpSurface;
  dataTestId?: string;
  className?: string;
};

export function ContextualHelpPanel({ surface, dataTestId, className }: Props) {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const [payload, setPayload] = useState<HelpCenterResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [updatingGuideId, setUpdatingGuideId] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      try {
        setLoading(true);
        const response = await apiClient.get<HelpCenterResponse>("help-center", {
          params: { surface, lang: i18n.language }
        });
        if (!mounted) {
          return;
        }
        setPayload(response.data);
      } catch {
        if (mounted) {
          setPayload(null);
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    load();
    return () => {
      mounted = false;
    };
  }, [i18n.language, surface]);

  const visibleGuides = useMemo(
    () => (payload?.guides ?? []).filter((guide) => guide.kind === "CONTEXTUAL" && !guide.dismissed).slice(0, 2),
    [payload]
  );

  const updateState = async (nextDismissedGuideKeys: string[]) => {
    if (!payload) {
      return;
    }
    const response = await apiClient.put<HelpCenterStateResponse>("help-center/state", {
      completedStepKeys: payload.completedStepKeys,
      dismissedGuideKeys: nextDismissedGuideKeys
    });
    setPayload((current) =>
      current
        ? {
            ...current,
            dismissedGuideKeys: response.data.dismissedGuideKeys,
            guides: current.guides.map((guide) => ({
              ...guide,
              dismissed: response.data.dismissedGuideKeys.includes(guide.id)
            })),
            onboardingSteps: current.onboardingSteps.map((step) => ({
              ...step,
              dismissed: response.data.dismissedGuideKeys.includes(step.key)
            }))
          }
        : current
    );
  };

  const handleDismiss = async (guide: HelpGuide) => {
    if (!payload) {
      return;
    }
    try {
      setUpdatingGuideId(guide.id);
      const nextDismissed = Array.from(new Set([...payload.dismissedGuideKeys, guide.id])).sort();
      await updateState(nextDismissed);
      toast.success(t("help_center.dismissed_toast"));
    } catch {
      toast.error(t("help_center.dismiss_error"));
    } finally {
      setUpdatingGuideId(null);
    }
  };

  if (loading || visibleGuides.length === 0) {
    return null;
  }

  return (
    <CivicCard
      title={t("help_center.contextual_title")}
      className={className}
      data-testid={dataTestId}
    >
      <div className="flex flex-column gap-4">
        <p className="text-sm text-secondary m-0">{t("help_center.contextual_desc")}</p>
        {visibleGuides.map((guide) => (
          <div key={guide.id} className="border-round-xl border-1 border-surface-soft bg-surface-soft p-4">
            <div className="flex justify-content-between gap-3 flex-wrap align-items-start">
              <div className="flex-1 min-w-0">
                <div className="font-black text-main">{guide.title}</div>
                <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{guide.summary}</p>
              </div>
              <CivicBadge label={t(`help_center.surfaces.${guide.surface}`)} severity="neutral" />
            </div>
            <div className="mt-3 flex gap-2 flex-wrap">
              <CivicButton
                type="button"
                label={t("help_center.open_full_guide")}
                icon="pi pi-book"
                variant="secondary"
                size="small"
                onClick={() => navigate(`/help?surface=${guide.surface}&guide=${guide.id}`)}
              />
              {guide.dismissible && (
                <CivicButton
                  type="button"
                  label={t("help_center.dismiss_guide")}
                  icon="pi pi-times"
                  variant="ghost"
                  size="small"
                  loading={updatingGuideId === guide.id}
                  onClick={() => handleDismiss(guide)}
                />
              )}
            </div>
          </div>
        ))}
      </div>
    </CivicCard>
  );
}
