import { useCallback, useEffect, useMemo, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { Navigate, useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { Checkbox, type CheckboxChangeEvent } from "primereact/checkbox";
import { InputText } from "primereact/inputtext";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { Layout } from "../components/Layout";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicField } from "../components/ui/CivicField";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicStatCard } from "../components/ui/CivicStatCard";
import { useAuthStore } from "../store/useAuthStore";
import { useCommunityStore } from "../store/useCommunityStore";
import type {
  CommunityOpenDataAccessLog,
  CommunityOpenDataCenter,
  CommunityOpenDataDataset,
  CommunityOpenDataFormat,
  CommunityOpenDataToken,
  CommunityOpenDataTokenScope,
  CommunityPermissionPolicy,
  CreateCommunityOpenDataTokenResponse,
} from "../types";

type ApiError = Error & { friendlyMessage?: string };

type TokenForm = {
  label: string;
  rateLimitPerHour: string;
  scopes: CommunityOpenDataTokenScope[];
};

const DEFAULT_SCOPE: CommunityOpenDataTokenScope = "EXPORT_SIGNALS";

const defaultValues: TokenForm = {
  label: "",
  rateLimitPerHour: "120",
  scopes: [DEFAULT_SCOPE],
};

export function CommunityOpenData() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const activeRole = useAuthStore((state) => state.activeRole);
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership = memberships.find((membership) => membership.communityId === activeCommunityId) ?? null;
  const [center, setCenter] = useState<CommunityOpenDataCenter | null>(null);
  const [permissionPolicies, setPermissionPolicies] = useState<CommunityPermissionPolicy[]>([]);
  const [loadingCenter, setLoadingCenter] = useState(false);
  const [loadingPolicies, setLoadingPolicies] = useState(false);
  const [downloadingKey, setDownloadingKey] = useState<string | null>(null);
  const [submittingToken, setSubmittingToken] = useState(false);
  const [revokingTokenId, setRevokingTokenId] = useState<string | null>(null);
  const [latestPlainToken, setLatestPlainToken] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<TokenForm>({
    mode: "onChange",
    defaultValues,
  });

  const watchedLabel = watch("label") ?? "";
  const openDataPolicy = useMemo(
    () => permissionPolicies.find((policy) => policy.scope === "MANAGE_OPEN_DATA_EXPORTS"),
    [permissionPolicies]
  );
  const canManageOpenData = Boolean(
    activeMembership && openDataPolicy?.allowedRoles.includes(activeMembership.role)
  );
  const datasets = center?.datasets ?? [];
  const scopeOptions = useMemo(
    () =>
      datasets.map((dataset) => ({
        value: `EXPORT_${dataset.resource}` as CommunityOpenDataTokenScope,
        label: t(`community_open_data.scopes.EXPORT_${dataset.resource}`),
        description: t(`community_open_data.resources.${dataset.resource}`),
      })),
    [datasets, t]
  );
  const activeTokenCount = center?.tokens.filter((token) => token.active).length ?? 0;

  const loadPolicies = useCallback(async () => {
    if (!activeCommunityId) {
      setPermissionPolicies([]);
      return;
    }
    setLoadingPolicies(true);
    try {
      const response = await apiClient.get<CommunityPermissionPolicy[]>(`communities/${activeCommunityId}/permissions`);
      setPermissionPolicies(response.data ?? []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_open_data.permissions_load_error"));
      setPermissionPolicies([]);
    } finally {
      setLoadingPolicies(false);
    }
  }, [activeCommunityId, t]);

  const loadCenter = useCallback(async () => {
    if (!activeCommunityId) {
      setCenter(null);
      return;
    }
    setLoadingCenter(true);
    try {
      const response = await apiClient.get<CommunityOpenDataCenter>(`community/exports/center?communityId=${activeCommunityId}`);
      setCenter(response.data);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_open_data.load_error"));
      setCenter(null);
    } finally {
      setLoadingCenter(false);
    }
  }, [activeCommunityId, t]);

  useEffect(() => {
    loadPolicies();
  }, [loadPolicies]);

  useEffect(() => {
    loadCenter();
  }, [loadCenter]);

  const formatDateTime = (value?: string | null) => {
    if (!value) {
      return t("community_open_data.not_available");
    }
    return new Date(value).toLocaleString();
  };

  const buildDownloadUrl = (resource: CommunityOpenDataDataset["resource"], format: CommunityOpenDataFormat) =>
    `community/exports/${resource.toLowerCase()}?communityId=${activeCommunityId}&format=${format}`;

  const buildPublicUrl = (dataset: CommunityOpenDataDataset) => {
    if (/^https?:\/\//i.test(dataset.externalPath)) {
      return dataset.externalPath;
    }
    return new URL(dataset.externalPath, window.location.origin).toString();
  };

  const resolveFilename = (headerValue: string | null, fallback: string) => {
    const match = headerValue?.match(/filename="?([^"]+)"?/i);
    return match?.[1] ?? fallback;
  };

  const triggerBrowserDownload = (blob: Blob, filename: string) => {
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = objectUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(objectUrl);
  };

  const copyText = async (value: string, successMessage: string) => {
    try {
      await navigator.clipboard.writeText(value);
      toast.success(successMessage);
    } catch {
      toast.error(t("community_open_data.copy_error"));
    }
  };

  const handleDatasetDownload = async (dataset: CommunityOpenDataDataset, format: CommunityOpenDataFormat) => {
    if (!activeCommunityId) {
      return;
    }
    const key = `${dataset.resource}-${format}`;
    setDownloadingKey(key);
    try {
      const response = await apiClient.get(buildDownloadUrl(dataset.resource, format), {
        responseType: "blob",
      });
      const filename = resolveFilename(
        response.headers["content-disposition"] ?? null,
        `community_${activeCommunityId}_${dataset.resource.toLowerCase()}.${format.toLowerCase()}`
      );
      triggerBrowserDownload(response.data, filename);
      await loadCenter();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_open_data.download_error"));
    } finally {
      setDownloadingKey(null);
    }
  };

  const onSubmitToken = async (values: TokenForm) => {
    if (!activeCommunityId) {
      return;
    }
    setSubmittingToken(true);
    try {
      const response = await apiClient.post<CreateCommunityOpenDataTokenResponse>("community/exports/tokens", {
        communityId: activeCommunityId,
        label: values.label.trim(),
        scopes: values.scopes,
        rateLimitPerHour: Number(values.rateLimitPerHour),
      });
      setLatestPlainToken(response.data.plainToken);
      toast.success(t("community_open_data.token_create_success"));
      reset({
        label: "",
        rateLimitPerHour: String(center?.defaultRateLimitPerHour ?? 120),
        scopes: values.scopes.length > 0 ? values.scopes : [DEFAULT_SCOPE],
      });
      await loadCenter();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_open_data.token_create_error"));
    } finally {
      setSubmittingToken(false);
    }
  };

  const handleRevokeToken = async (token: CommunityOpenDataToken) => {
    if (!activeCommunityId) {
      return;
    }
    setRevokingTokenId(token.id);
    try {
      await apiClient.delete(`community/exports/tokens/${token.id}?communityId=${activeCommunityId}`);
      toast.success(t("community_open_data.token_revoke_success"));
      await loadCenter();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_open_data.token_revoke_error"));
    } finally {
      setRevokingTokenId(null);
    }
  };

  if (!activeCommunityId || !activeMembership) {
    return (
      <Layout>
        <CivicCard>
          <CivicEmptyState
            icon="pi pi-database"
            title={t("community_open_data.no_context_title")}
            description={t("community_open_data.no_context_desc")}
            actionLabel={t("nav.communities")}
            onAction={() => navigate("/communities")}
          />
        </CivicCard>
      </Layout>
    );
  }

  if (!loadingPolicies && openDataPolicy && !canManageOpenData && activeRole !== "SUPER_ADMIN") {
    return <Navigate to="/unauthorized" replace />;
  }

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column xl:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("community_open_data.title")}
            description={t("community_open_data.desc", { community: activeMembership.communityName })}
            className="mb-0"
          />
          <CivicActionBar className="w-full xl:w-auto">
            <div className="community-home-action-copy">
              <div className="u-eyebrow">{t("community_open_data.kicker")}</div>
              <p className="u-section-copy text-sm m-0">{t("community_open_data.kicker_desc")}</p>
            </div>
            <div className="dashboard-action-cluster">
              <CivicButton type="button" icon="pi pi-refresh" label={t("community_open_data.refresh")} variant="secondary" onClick={loadCenter} />
              <CivicButton type="button" icon="pi pi-chart-line" label={t("nav.community_trust")} variant="ghost" onClick={() => navigate("/communities/trust")} />
              <CivicButton type="button" icon="pi pi-sitemap" label={t("nav.community_decisions")} variant="ghost" onClick={() => navigate("/communities/decisions")} />
            </div>
          </CivicActionBar>
        </div>

        {loadingPolicies || (loadingCenter && !center) ? (
          <CivicCard>
            <p className="text-secondary m-0">{t("common.loading")}</p>
          </CivicCard>
        ) : center ? (
          <div className="flex flex-column gap-6">
            <div className="civic-stat-grid civic-stat-grid-comfortable" data-testid="community-open-data-stats-grid">
              <CivicStatCard compact label={t("community_open_data.stats.datasets")} value={center.datasets.length} supportingText={t("community_open_data.stats.datasets_support")} />
              <CivicStatCard compact label={t("community_open_data.stats.active_tokens")} value={activeTokenCount} supportingText={t("community_open_data.stats.active_tokens_support")} />
              <CivicStatCard compact label={t("community_open_data.stats.recent_access")} value={center.recentAccessLogs.length} supportingText={t("community_open_data.stats.recent_access_support")} />
              <CivicStatCard compact label={t("community_open_data.stats.default_rate_limit")} value={center.defaultRateLimitPerHour} supportingText={t("community_open_data.stats.default_rate_limit_support")} />
            </div>

            <CivicCard title={t("community_open_data.datasets_title")} data-testid="community-open-data-datasets-card">
              <div className="flex flex-column gap-4">
                <p className="text-secondary text-sm m-0 line-height-3">{t("community_open_data.datasets_help")}</p>
                {center.datasets.map((dataset) => (
                  <div key={dataset.resource} className="border-round-xl border-1 border-surface-soft bg-surface-soft p-4" data-testid={`community-open-data-dataset-${dataset.resource.toLowerCase()}`}>
                    <div className="flex justify-content-between align-items-start gap-4 flex-wrap">
                      <div className="min-w-0 flex-1">
                        <div className="flex align-items-center gap-2 flex-wrap">
                          <span className="font-black text-main text-lg">{t(`community_open_data.resources.${dataset.resource}`)}</span>
                          {dataset.formats.map((format) => (
                            <CivicBadge key={`${dataset.resource}-${format}`} label={t(`community_open_data.formats.${format}`)} severity="neutral" />
                          ))}
                        </div>
                        <p className="text-sm text-secondary mt-3 mb-0 line-height-3">{dataset.description}</p>
                        <div className="u-surface-note mt-3">
                          <div className="u-eyebrow mb-2">{t("community_open_data.public_api_label")}</div>
                          <p className="text-sm text-secondary m-0 break-all">{buildPublicUrl(dataset)}</p>
                        </div>
                      </div>
                      <div className="flex flex-column gap-2">
                        {dataset.formats.map((format) => (
                          <CivicButton
                            key={`${dataset.resource}-${format}-download`}
                            type="button"
                            icon="pi pi-download"
                            label={t("community_open_data.download_format", { format: t(`community_open_data.formats.${format}`) })}
                            variant="secondary"
                            loading={downloadingKey === `${dataset.resource}-${format}`}
                            onClick={() => handleDatasetDownload(dataset, format)}
                            data-testid={`community-open-data-download-${dataset.resource.toLowerCase()}-${format.toLowerCase()}`}
                          />
                        ))}
                        <CivicButton
                          type="button"
                          icon="pi pi-copy"
                          label={t("community_open_data.copy_public_url")}
                          variant="ghost"
                          onClick={() => copyText(buildPublicUrl(dataset), t("community_open_data.copy_public_url_success"))}
                          data-testid={`community-open-data-copy-url-${dataset.resource.toLowerCase()}`}
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </CivicCard>

            <div className="grid">
              <div className="col-12 xl:col-5">
                <CivicCard title={t("community_open_data.token_create_title")} data-testid="community-open-data-create-token-card">
                  <form className="flex flex-column gap-2" onSubmit={handleSubmit(onSubmitToken)}>
                    <CivicField
                      label={t("community_open_data.token_label_label")}
                      helpText={t("community_open_data.token_label_help", { count: watchedLabel.length })}
                      error={errors.label?.message}
                    >
                      <Controller
                        name="label"
                        control={control}
                        rules={{
                          required: t("community_open_data.token_label_required"),
                          maxLength: {
                            value: 120,
                            message: t("community_open_data.token_label_too_long"),
                          },
                        }}
                        render={({ field }) => (
                          <InputText
                            {...field}
                            value={field.value ?? ""}
                            onChange={(e) => field.onChange(e.target.value)}
                            className="w-full"
                            placeholder={t("community_open_data.token_label_placeholder")}
                            data-testid="community-open-data-token-label"
                          />
                        )}
                      />
                    </CivicField>

                    <CivicField
                      label={t("community_open_data.rate_limit_label")}
                      helpText={t("community_open_data.rate_limit_help")}
                      error={errors.rateLimitPerHour?.message}
                    >
                      <Controller
                        name="rateLimitPerHour"
                        control={control}
                        rules={{
                          required: t("community_open_data.rate_limit_required"),
                          validate: (value) => {
                            const parsed = Number(value);
                            if (!Number.isInteger(parsed)) {
                              return t("community_open_data.rate_limit_invalid");
                            }
                            if (parsed < 1 || parsed > 1000) {
                              return t("community_open_data.rate_limit_range");
                            }
                            return true;
                          },
                        }}
                        render={({ field }) => (
                          <InputText
                            {...field}
                            value={field.value ?? ""}
                            onChange={(e) => field.onChange(e.target.value)}
                            className="w-full"
                            inputMode="numeric"
                            placeholder={String(center.defaultRateLimitPerHour)}
                            data-testid="community-open-data-rate-limit"
                          />
                        )}
                      />
                    </CivicField>

                    <CivicField
                      label={t("community_open_data.scopes_label")}
                      helpText={t("community_open_data.scopes_help")}
                      error={errors.scopes?.message}
                    >
                      <Controller
                        name="scopes"
                        control={control}
                        rules={{
                          validate: (value) => (value.length > 0 ? true : t("community_open_data.scopes_required")),
                        }}
                        render={({ field }) => (
                          <div className="flex flex-column gap-3" data-testid="community-open-data-scope-list">
                            {scopeOptions.map((option) => {
                              const checked = field.value.includes(option.value);
                              return (
                                <label key={option.value} className="border-round-xl border-1 border-surface-soft bg-surface-soft p-3 cursor-pointer">
                                  <div className="flex align-items-start gap-3">
                                    <Checkbox
                                      inputId={option.value}
                                      checked={checked}
                                      onChange={(e: CheckboxChangeEvent) => {
                                        const nextValues = e.checked
                                          ? [...field.value, option.value]
                                          : field.value.filter((value) => value !== option.value);
                                        field.onChange(nextValues);
                                      }}
                                      className="mt-1"
                                      data-testid={`community-open-data-scope-${option.value}`}
                                    />
                                    <div className="min-w-0">
                                      <div className="font-black text-main">{option.label}</div>
                                      <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{option.description}</p>
                                    </div>
                                  </div>
                                </label>
                              );
                            })}
                          </div>
                        )}
                      />
                    </CivicField>

                    <CivicActionBar>
                      <span className="text-xs text-muted font-medium">{t("community_open_data.token_security_note")}</span>
                      <CivicButton
                        type="submit"
                        icon="pi pi-key"
                        label={t("community_open_data.create_token")}
                        loading={submittingToken}
                        data-testid="community-open-data-create-token-button"
                      />
                    </CivicActionBar>
                  </form>

                  {latestPlainToken && (
                    <div className="u-surface-note mt-4" data-testid="community-open-data-plain-token-panel">
                      <div className="flex justify-content-between align-items-start gap-3 flex-wrap">
                        <div className="min-w-0 flex-1">
                          <div className="u-eyebrow mb-2">{t("community_open_data.plain_token_title")}</div>
                          <p className="text-sm text-secondary mt-0 mb-3 line-height-3">{t("community_open_data.plain_token_help")}</p>
                          <code className="block text-sm text-main line-height-3 break-all">{latestPlainToken}</code>
                        </div>
                        <CivicButton
                          type="button"
                          icon="pi pi-copy"
                          label={t("community_open_data.copy_token")}
                          variant="secondary"
                          onClick={() => copyText(latestPlainToken, t("community_open_data.copy_token_success"))}
                          data-testid="community-open-data-copy-token-button"
                        />
                      </div>
                    </div>
                  )}
                </CivicCard>
              </div>

              <div className="col-12 xl:col-7">
                <CivicCard title={t("community_open_data.tokens_title")} data-testid="community-open-data-tokens-card">
                  <div className="flex flex-column gap-4">
                    <p className="text-secondary text-sm m-0 line-height-3">{t("community_open_data.tokens_help")}</p>
                    {center.tokens.length === 0 ? (
                      <CivicEmptyState
                        icon="pi pi-key"
                        title={t("community_open_data.tokens_empty_title")}
                        description={t("community_open_data.tokens_empty_desc")}
                      />
                    ) : (
                      center.tokens.map((token) => (
                        <div key={token.id} className="border-round-xl border-1 border-surface-soft bg-surface-soft p-4" data-testid={`community-open-data-token-${token.id}`}>
                          <div className="flex justify-content-between gap-4 align-items-start flex-wrap">
                            <div className="min-w-0 flex-1">
                              <div className="flex align-items-center gap-2 flex-wrap">
                                <span className="font-black text-main text-lg">{token.label}</span>
                                <CivicBadge
                                  label={token.active ? t("community_open_data.token_status_active") : t("community_open_data.token_status_revoked")}
                                  severity={token.active ? "progress" : "rejected"}
                                />
                              </div>
                              <p className="text-sm text-secondary mt-2 mb-0 line-height-3">
                                {t("community_open_data.token_prefix_copy", { prefix: token.tokenPrefix })}
                              </p>
                            </div>
                            <CivicButton
                              type="button"
                              icon="pi pi-ban"
                              label={t("community_open_data.revoke_token")}
                              variant="ghost"
                              disabled={!token.active}
                              loading={revokingTokenId === token.id}
                              onClick={() => handleRevokeToken(token)}
                              data-testid={`community-open-data-revoke-token-${token.id}`}
                            />
                          </div>

                          <div className="grid mt-2">
                            <div className="col-12 md:col-4">
                              <div className="u-eyebrow mb-2">{t("community_open_data.rate_limit_label")}</div>
                              <p className="text-sm text-secondary m-0">{t("community_open_data.rate_limit_value", { count: token.rateLimitPerHour })}</p>
                            </div>
                            <div className="col-12 md:col-4">
                              <div className="u-eyebrow mb-2">{t("community_open_data.created_at_label")}</div>
                              <p className="text-sm text-secondary m-0">{formatDateTime(token.createdAt)}</p>
                            </div>
                            <div className="col-12 md:col-4">
                              <div className="u-eyebrow mb-2">{t("community_open_data.last_used_at_label")}</div>
                              <p className="text-sm text-secondary m-0">{formatDateTime(token.lastUsedAt)}</p>
                            </div>
                          </div>

                          <div className="flex flex-wrap gap-2 mt-3">
                            {token.scopes.map((scope) => (
                              <CivicBadge key={`${token.id}-${scope}`} label={t(`community_open_data.scopes.${scope}`)} severity="neutral" />
                            ))}
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </CivicCard>
              </div>
            </div>

            <CivicCard title={t("community_open_data.logs_title")} data-testid="community-open-data-logs-card">
              <div className="flex flex-column gap-4">
                <p className="text-secondary text-sm m-0 line-height-3">{t("community_open_data.logs_help")}</p>
                {center.recentAccessLogs.length === 0 ? (
                  <CivicEmptyState
                    icon="pi pi-history"
                    title={t("community_open_data.logs_empty_title")}
                    description={t("community_open_data.logs_empty_desc")}
                  />
                ) : (
                  center.recentAccessLogs.map((log: CommunityOpenDataAccessLog) => (
                    <div key={log.id} className="border-round-xl border-1 border-surface-soft bg-surface-soft p-4" data-testid={`community-open-data-log-${log.id}`}>
                      <div className="flex justify-content-between gap-3 flex-wrap align-items-start">
                        <div className="min-w-0 flex-1">
                          <div className="flex align-items-center gap-2 flex-wrap">
                            <span className="font-black text-main">{t(`community_open_data.access_channels.${log.accessChannel}`)}</span>
                            <CivicBadge label={t(`community_open_data.resources.${log.exportType}`)} severity="neutral" />
                            <CivicBadge label={t(`community_open_data.formats.${log.format}`)} severity="neutral" />
                          </div>
                          <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{log.note}</p>
                        </div>
                        <CivicBadge label={formatDateTime(log.createdAt)} severity="neutral" />
                      </div>
                      <div className="grid mt-2">
                        <div className="col-12 md:col-4">
                          <div className="u-eyebrow mb-2">{t("community_open_data.actor_label")}</div>
                          <p className="text-sm text-secondary m-0">{log.actorUsername ?? t("community_open_data.actor_system")}</p>
                        </div>
                        <div className="col-12 md:col-4">
                          <div className="u-eyebrow mb-2">{t("community_open_data.token_label_short")}</div>
                          <p className="text-sm text-secondary m-0">{log.tokenLabel ?? t("community_open_data.not_available")}</p>
                        </div>
                        <div className="col-12 md:col-4">
                          <div className="u-eyebrow mb-2">{t("community_open_data.public_api_label")}</div>
                          <p className="text-sm text-secondary m-0">
                            {t("community_open_data.access_path_value", {
                              path: datasets.find((dataset) => dataset.resource === log.exportType)?.externalPath ?? "/api/open-data",
                            })}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </CivicCard>
          </div>
        ) : (
          <CivicCard>
            <CivicEmptyState
              icon="pi pi-database"
              title={t("community_open_data.empty_title")}
              description={t("community_open_data.empty_desc")}
              actionLabel={t("community_open_data.refresh")}
              onAction={loadCenter}
            />
          </CivicCard>
        )}
      </div>
    </Layout>
  );
}
