import { useCallback, useEffect, useMemo, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { Navigate, useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { Checkbox, type CheckboxChangeEvent } from "primereact/checkbox";
import { InputText } from "primereact/inputtext";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { formatStamp } from "../utils/formatStamp";
import { Layout } from "../components/Layout";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicField } from "../components/ui/CivicField";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicSelect } from "../components/ui/CivicSelect";
import { CivicStatCard } from "../components/ui/CivicStatCard";
import { useAuthStore } from "../store/useAuthStore";
import { useCommunityStore } from "../store/useCommunityStore";
import type {
  CommunityIntegration,
  CommunityIntegrationCenter,
  CommunityIntegrationChannel,
  CommunityIntegrationDelivery,
  CommunityPermissionPolicy,
} from "../types";

type ApiError = Error & { friendlyMessage?: string };

type IntegrationForm = {
  channel: CommunityIntegrationChannel;
  name: string;
  targetUri: string;
  secret: string;
  autoRetry: boolean;
};

const defaultValues: IntegrationForm = {
  channel: "WEBHOOK",
  name: "",
  targetUri: "",
  secret: "",
  autoRetry: true,
};

const deliverySeverity = (status: CommunityIntegrationDelivery["status"]) => {
  if (status === "DELIVERED") return "resolved" as const;
  if (status === "FAILED") return "rejected" as const;
  return "progress" as const;
};

export function CommunityIntegrations() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const activeRole = useAuthStore((state) => state.activeRole);
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership = memberships.find((m) => m.communityId === activeCommunityId) ?? null;

  const [center, setCenter] = useState<CommunityIntegrationCenter | null>(null);
  const [policies, setPolicies] = useState<CommunityPermissionPolicy[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<IntegrationForm>({ mode: "onChange", defaultValues });

  const managePolicy = useMemo(
    () => policies.find((policy) => policy.scope === "MANAGE_INTEGRATIONS"),
    [policies]
  );
  const canManage = Boolean(
    activeMembership && managePolicy?.allowedRoles.includes(activeMembership.role)
  );

  const loadPolicies = useCallback(async () => {
    if (!activeCommunityId) {
      setPolicies([]);
      return;
    }
    try {
      const response = await apiClient.get<CommunityPermissionPolicy[]>(
        `communities/${activeCommunityId}/permissions`
      );
      setPolicies(response.data ?? []);
    } catch {
      setPolicies([]);
    }
  }, [activeCommunityId]);

  const loadCenter = useCallback(async () => {
    if (!activeCommunityId) {
      setCenter(null);
      return;
    }
    setLoading(true);
    try {
      const response = await apiClient.get<CommunityIntegrationCenter>(
        `community/integrations/center?communityId=${activeCommunityId}`
      );
      setCenter(response.data);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_integrations.load_error"));
      setCenter(null);
    } finally {
      setLoading(false);
    }
  }, [activeCommunityId, t]);

  useEffect(() => {
    loadPolicies();
  }, [loadPolicies]);

  useEffect(() => {
    loadCenter();
  }, [loadCenter]);

  const formatDateTime = (value?: string | null) =>
    formatStamp(value) ?? t("community_integrations.never");

  const onSubmit = async (values: IntegrationForm) => {
    if (!activeCommunityId) return;
    setSubmitting(true);
    try {
      await apiClient.post("community/integrations", {
        communityId: activeCommunityId,
        channel: values.channel,
        name: values.name.trim(),
        targetUri: values.targetUri.trim(),
        secret: values.secret,
        autoRetry: values.autoRetry,
      });
      toast.success(t("community_integrations.create_success"));
      reset(defaultValues);
      await loadCenter();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_integrations.create_error"));
    } finally {
      setSubmitting(false);
    }
  };

  const onToggle = async (integration: CommunityIntegration) => {
    if (!activeCommunityId) return;
    setBusyId(integration.id);
    try {
      await apiClient.patch(
        `community/integrations/${integration.id}?communityId=${activeCommunityId}&enabled=${!integration.enabled}`
      );
      await loadCenter();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_integrations.toggle_error"));
    } finally {
      setBusyId(null);
    }
  };

  const onRetry = async (delivery: CommunityIntegrationDelivery) => {
    if (!activeCommunityId) return;
    setBusyId(delivery.id);
    try {
      await apiClient.post(
        `community/integrations/deliveries/${delivery.id}/retry?communityId=${activeCommunityId}`
      );
      toast.success(t("community_integrations.retry_success"));
      await loadCenter();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_integrations.retry_error"));
    } finally {
      setBusyId(null);
    }
  };

  if (!activeCommunityId || !activeMembership) {
    return (
      <Layout>
        <CivicCard>
          <CivicEmptyState
            icon="pi pi-send"
            title={t("community_integrations.no_context_title")}
            description={t("community_integrations.no_context_desc")}
            actionLabel={t("nav.communities")}
            onAction={() => navigate("/communities")}
          />
        </CivicCard>
      </Layout>
    );
  }

  if (center && !canManage && activeRole !== "SUPER_ADMIN") {
    return <Navigate to="/unauthorized" replace />;
  }

  const integrations = center?.integrations ?? [];
  const deliveries = center?.recentDeliveries ?? [];

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column xl:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("community_integrations.title")}
            description={t("community_integrations.desc", { community: activeMembership.communityName })}
            className="mb-0"
          />
          <CivicActionBar className="w-full xl:w-auto">
            <div className="community-home-action-copy">
              <div className="u-eyebrow">{t("community_integrations.kicker")}</div>
              <p className="u-section-copy text-sm m-0">{t("community_integrations.kicker_desc")}</p>
            </div>
            <div className="dashboard-action-cluster">
              <CivicButton
                type="button"
                icon="pi pi-refresh"
                label={t("community_integrations.refresh")}
                variant="secondary"
                onClick={loadCenter}
                data-testid="community-integrations-refresh"
              />
              <CivicButton
                type="button"
                icon="pi pi-building"
                label={t("nav.community_resources")}
                variant="ghost"
                onClick={() => navigate("/communities/resources")}
              />
            </div>
          </CivicActionBar>
        </div>

        <div className="civic-stat-grid civic-stat-grid-comfortable" data-testid="community-integrations-stats-grid">
          <CivicStatCard compact label={t("community_integrations.integration_count")} value={center?.integrationCount ?? 0} supportingText={t("community_integrations.stats_support")} />
          <CivicStatCard compact label={t("community_integrations.pending_deliveries")} value={center?.pendingDeliveries ?? 0} supportingText={t("community_integrations.stats_support")} />
          <CivicStatCard compact label={t("community_integrations.failed_deliveries")} value={center?.failedDeliveries ?? 0} supportingText={t("community_integrations.stats_support")} />
        </div>

        <div className="grid mt-6">
          <div className="col-12 xl:col-7 flex flex-column gap-6">
            <CivicCard title={t("community_integrations.integrations_title")} data-testid="community-integrations-list-card">
              {loading && integrations.length === 0 ? (
                <p className="text-secondary m-0">{t("common.loading")}</p>
              ) : integrations.length === 0 ? (
                <CivicEmptyState
                  icon="pi pi-send"
                  title={t("community_integrations.empty_title")}
                  description={t("community_integrations.empty_desc")}
                />
              ) : (
                <div className="flex flex-column gap-4">
                  {integrations.map((integration) => (
                    <div
                      key={integration.id}
                      className="border-round-xl border-1 border-surface-soft p-4"
                      data-testid={`community-integration-${integration.id}`}
                    >
                      <div className="flex align-items-center gap-2 flex-wrap">
                        <span className="font-black text-main text-lg">{integration.name}</span>
                        <CivicBadge
                          label={t(`community_integrations.channels.${integration.channel}`)}
                          type="category"
                        />
                        <CivicBadge
                          label={integration.dispatchable ? t("community_integrations.dispatchable") : t("community_integrations.no_connector")}
                          severity={integration.dispatchable ? "resolved" : "neutral"}
                        />
                        {!integration.enabled && <CivicBadge label={t("community_integrations.disable")} severity="neutral" />}
                      </div>

                      <p className="text-sm text-secondary mt-2 mb-0 break-all">{integration.targetUri}</p>

                      <div className="flex align-items-center gap-4 flex-wrap mt-3">
                        <span className="text-xs text-muted">
                          {t("community_integrations.last_attempt")}: {formatDateTime(integration.lastAttemptAt)}
                        </span>
                        <span className="text-xs text-muted">
                          {t("community_integrations.last_success")}: {formatDateTime(integration.lastSuccessAt)}
                        </span>
                        {integration.consecutiveFailures > 0 && (
                          <span className="text-xs text-status-rejected">
                            {t("community_integrations.consecutive_failures", { count: integration.consecutiveFailures })}
                          </span>
                        )}
                      </div>

                      <CivicButton
                        type="button"
                        icon={integration.enabled ? "pi pi-pause" : "pi pi-play"}
                        label={integration.enabled ? t("community_integrations.disable") : t("community_integrations.enable")}
                        variant="ghost"
                        loading={busyId === integration.id}
                        onClick={() => onToggle(integration)}
                        data-testid={`community-integration-toggle-${integration.id}`}
                      />
                    </div>
                  ))}
                </div>
              )}
            </CivicCard>

            <CivicCard title={t("community_integrations.deliveries_title")} data-testid="community-integrations-deliveries">
              {deliveries.length === 0 ? (
                <p className="text-secondary m-0">{t("community_integrations.deliveries_empty")}</p>
              ) : (
                <div className="flex flex-column gap-3">
                  {deliveries.map((delivery) => (
                    <div
                      key={delivery.id}
                      className="border-round-xl border-1 border-surface-soft p-3"
                      data-testid={`community-integration-delivery-${delivery.id}`}
                    >
                      <div className="flex align-items-center gap-2 flex-wrap">
                        <span className="font-black text-main">{delivery.integrationName}</span>
                        <CivicBadge
                          label={t(`community_integrations.delivery_statuses.${delivery.status}`)}
                          severity={deliverySeverity(delivery.status)}
                        />
                        <span className="text-xs text-muted">{delivery.eventType}</span>
                        <span className="text-xs text-muted">
                          {t("community_integrations.attempts", { count: delivery.attempts })}
                        </span>
                      </div>
                      <small className="text-muted">{formatDateTime(delivery.createdAt)}</small>
                      {delivery.lastError && (
                        <p className="text-sm text-status-rejected mt-2 mb-0 break-all">
                          {delivery.lastError}
                        </p>
                      )}
                      {delivery.status === "FAILED" && (
                        <CivicButton
                          type="button"
                          icon="pi pi-refresh"
                          label={t("community_integrations.retry")}
                          variant="secondary"
                          loading={busyId === delivery.id}
                          onClick={() => onRetry(delivery)}
                          data-testid={`community-integration-retry-${delivery.id}`}
                        />
                      )}
                    </div>
                  ))}
                </div>
              )}
            </CivicCard>
          </div>

          <div className="col-12 xl:col-5">
            <CivicCard title={t("community_integrations.create_title")} data-testid="community-integrations-create-card">
              <form className="flex flex-column gap-2" onSubmit={handleSubmit(onSubmit)}>
                <CivicField label={t("community_integrations.create_channel_label")} error={errors.channel?.message}>
                  <Controller
                    name="channel"
                    control={control}
                    rules={{ required: t("community_integrations.channel_required") }}
                    render={({ field }) => (
                      <CivicSelect
                        value={field.value}
                        onChange={(e) => field.onChange(e.value)}
                        options={(center?.supportedChannels ?? []).map((channel) => ({
                          value: channel,
                          label: t(`community_integrations.channels.${channel}`),
                        }))}
                        placeholder={t("community_integrations.create_channel_label")}
                        inputId="community-integrations-channel"
                        data-testid="community-integrations-channel"
                      />
                    )}
                  />
                </CivicField>

                <CivicField label={t("community_integrations.create_name_label")} error={errors.name?.message}>
                  <Controller
                    name="name"
                    control={control}
                    rules={{
                      required: t("community_integrations.name_required"),
                      minLength: { value: 3, message: t("community_integrations.name_too_short") },
                    }}
                    render={({ field }) => (
                      <InputText
                        {...field}
                        value={field.value ?? ""}
                        onChange={(e) => field.onChange(e.target.value)}
                        className="w-full"
                        placeholder={t("community_integrations.create_name_placeholder")}
                        data-testid="community-integrations-name"
                      />
                    )}
                  />
                </CivicField>

                <CivicField
                  label={t("community_integrations.create_target_label")}
                  error={errors.targetUri?.message}
                >
                  <Controller
                    name="targetUri"
                    control={control}
                    rules={{
                      required: t("community_integrations.target_required"),
                      pattern: {
                        value: /^https?:\/\/.+/,
                        message: t("community_integrations.target_required"),
                      },
                    }}
                    render={({ field }) => (
                      <InputText
                        {...field}
                        value={field.value ?? ""}
                        onChange={(e) => field.onChange(e.target.value)}
                        className="w-full"
                        placeholder={t("community_integrations.create_target_placeholder")}
                        data-testid="community-integrations-target"
                      />
                    )}
                  />
                </CivicField>

                <CivicField
                  label={t("community_integrations.create_secret_label")}
                  helpText={t("community_integrations.create_secret_help")}
                  error={errors.secret?.message}
                >
                  <Controller
                    name="secret"
                    control={control}
                    rules={{
                      required: t("community_integrations.secret_required"),
                      minLength: { value: 8, message: t("community_integrations.secret_required") },
                    }}
                    render={({ field }) => (
                      <InputText
                        {...field}
                        type="password"
                        value={field.value ?? ""}
                        onChange={(e) => field.onChange(e.target.value)}
                        className="w-full"
                        data-testid="community-integrations-secret"
                      />
                    )}
                  />
                </CivicField>

                <div className="flex align-items-center gap-2 mb-4">
                  <Controller
                    name="autoRetry"
                    control={control}
                    render={({ field }) => (
                      <Checkbox
                        inputId="community-integrations-auto-retry"
                        checked={Boolean(field.value)}
                        onChange={(e: CheckboxChangeEvent) => field.onChange(e.value)}
                        data-testid="community-integrations-auto-retry"
                      />
                    )}
                  />
                  <label htmlFor="community-integrations-auto-retry" className="text-sm text-secondary">
                    {t("community_integrations.create_retry_label")}
                  </label>
                </div>

                <CivicButton
                  type="submit"
                  icon="pi pi-link"
                  label={t("community_integrations.create_submit")}
                  loading={submitting}
                  data-testid="community-integrations-create-submit"
                />
              </form>
            </CivicCard>
          </div>
        </div>
      </div>
    </Layout>
  );
}