import { useCallback, useEffect, useMemo, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { Calendar } from "primereact/calendar";
import { InputNumber } from "primereact/inputnumber";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { Layout } from "../components/Layout";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicCharacterCount } from "../components/ui/CivicCharacterCount";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicField } from "../components/ui/CivicField";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicStatCard } from "../components/ui/CivicStatCard";
import { useCommunityStore } from "../store/useCommunityStore";
import type {
  CommunityActivity,
  CommunityActivityBoard,
  CommunityPermissionPolicy,
} from "../types";

type ApiError = Error & { friendlyMessage?: string };

const DESCRIPTION_MAX = 1200;

type ActivityForm = {
  title: string;
  description: string;
  locationLabel: string;
  startsAt: Date | null;
  endsAt: Date | null;
  signupOpensAt: Date | null;
  signupClosesAt: Date | null;
  signupCapacity: number;
};

const defaultValues: ActivityForm = {
  title: "",
  description: "",
  locationLabel: "",
  startsAt: null,
  endsAt: null,
  signupOpensAt: null,
  signupClosesAt: null,
  signupCapacity: 10,
};

const WINDOW_REASON_KEYS: Record<string, string> = {
  "This activity already reached its published capacity.": "community_activities.capacity_full",
  "Signups for this activity are not open yet.": "community_activities.window_not_open",
  "Signups for this activity closed at the published deadline.": "community_activities.window_closed",
};

export function CommunityActivities() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership = memberships.find((m) => m.communityId === activeCommunityId) ?? null;

  const [board, setBoard] = useState<CommunityActivityBoard | null>(null);
  const [policies, setPolicies] = useState<CommunityPermissionPolicy[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [busyActivityId, setBusyActivityId] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<ActivityForm>({ mode: "onChange", defaultValues });

  const watchedDescription = watch("description") ?? "";

  const managePolicy = useMemo(
    () => policies.find((policy) => policy.scope === "MANAGE_ACTIVITIES"),
    [policies]
  );
  const joinPolicy = useMemo(
    () => policies.find((policy) => policy.scope === "JOIN_ACTIVITIES"),
    [policies]
  );
  const canManage = Boolean(activeMembership && managePolicy?.allowedRoles.includes(activeMembership.role));
  const canJoin = Boolean(activeMembership && joinPolicy?.allowedRoles.includes(activeMembership.role));

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

  const loadBoard = useCallback(async () => {
    if (!activeCommunityId) {
      setBoard(null);
      return;
    }
    setLoading(true);
    try {
      const response = await apiClient.get<CommunityActivityBoard>(
        `community/activities/board?communityId=${activeCommunityId}`
      );
      setBoard(response.data);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_activities.load_error"));
      setBoard(null);
    } finally {
      setLoading(false);
    }
  }, [activeCommunityId, t]);

  useEffect(() => {
    loadPolicies();
  }, [loadPolicies]);

  useEffect(() => {
    loadBoard();
  }, [loadBoard]);

  const formatDateTime = (value?: string | null) => (value ? new Date(value).toLocaleString() : "-");

  const windowBadgeSeverity = (state: CommunityActivity["signupWindowState"]) => {
    if (state === "OPEN") return "resolved" as const;
    if (state === "FULL") return "progress" as const;
    return "neutral" as const;
  };

  const translateBlockReason = (activity: CommunityActivity) => {
    const reasonKey = activity.fullReason ? WINDOW_REASON_KEYS[activity.fullReason] : undefined;
    return reasonKey ? t(reasonKey) : activity.fullReason;
  };

  const onToggleSignup = async (activity: CommunityActivity) => {
    if (!activeCommunityId) return;
    const leaving = activity.currentVolunteerStatus === "CONFIRMED";
    setBusyActivityId(activity.id);
    try {
      if (leaving) {
        await apiClient.delete(`community/activities/${activity.id}/signups?communityId=${activeCommunityId}`);
        toast.success(t("community_activities.leave_success"));
      } else {
        await apiClient.post(`community/activities/${activity.id}/signups?communityId=${activeCommunityId}`);
        toast.success(t("community_activities.join_success"));
      }
      await loadBoard();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t(leaving ? "community_activities.leave_error" : "community_activities.join_error"));
    } finally {
      setBusyActivityId(null);
    }
  };

  const onRecordAttendance = async (activity: CommunityActivity, signupId: string, attended: boolean) => {
    if (!activeCommunityId) return;
    setBusyActivityId(activity.id);
    try {
      await apiClient.patch(`community/activities/${activity.id}/attendance?communityId=${activeCommunityId}`, {
        communityId: activeCommunityId,
        signupId,
        attendanceStatus: attended ? "ATTENDED" : "NO_SHOW",
      });
      toast.success(t("community_activities.attendance.success"));
      await loadBoard();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_activities.attendance.error"));
    } finally {
      setBusyActivityId(null);
    }
  };

  const onSubmit = async (values: ActivityForm) => {
    if (!activeCommunityId) return;
    if (!values.startsAt || !values.endsAt || !values.signupOpensAt || !values.signupClosesAt) {
      toast.error(t("community_activities.schedule_error"));
      return;
    }
    setSubmitting(true);
    try {
      const response = await apiClient.post<CommunityActivityBoard>("community/activities", {
        communityId: activeCommunityId,
        title: values.title.trim(),
        description: values.description.trim(),
        locationLabel: values.locationLabel.trim(),
        startsAt: values.startsAt.toISOString(),
        endsAt: values.endsAt.toISOString(),
        signupOpensAt: values.signupOpensAt.toISOString(),
        signupClosesAt: values.signupClosesAt.toISOString(),
        signupCapacity: Number(values.signupCapacity),
      });
      setBoard(response.data);
      toast.success(t("community_activities.create_success"));
      reset(defaultValues);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_activities.create_error"));
    } finally {
      setSubmitting(false);
    }
  };

  if (!activeCommunityId || !activeMembership) {
    return (
      <Layout>
        <CivicCard>
          <CivicEmptyState
            icon="pi pi-calendar"
            title={t("community_activities.no_context_title")}
            description={t("community_activities.no_context_desc")}
            actionLabel={t("nav.communities")}
            onAction={() => navigate("/communities")}
          />
        </CivicCard>
      </Layout>
    );
  }

  const activities = board?.activities ?? [];

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column xl:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("community_activities.title")}
            description={t("community_activities.desc", { community: activeMembership.communityName })}
            className="mb-0"
          />
          <CivicActionBar className="w-full xl:w-auto">
            <div className="community-home-action-copy">
              <div className="u-eyebrow">{t("community_activities.kicker")}</div>
              <p className="u-section-copy text-sm m-0">{t("community_activities.kicker_desc")}</p>
            </div>
            <div className="dashboard-action-cluster">
              <CivicButton
                type="button"
                icon="pi pi-refresh"
                label={t("community_activities.refresh")}
                variant="secondary"
                onClick={loadBoard}
                data-testid="community-activities-refresh"
              />
              <CivicButton
                type="button"
                icon="pi pi-comments"
                label={t("nav.community_rooms")}
                variant="ghost"
                onClick={() => navigate("/communities/rooms")}
              />
            </div>
          </CivicActionBar>
        </div>

        <div className="civic-stat-grid civic-stat-grid-comfortable" data-testid="community-activities-stats-grid">
          <CivicStatCard compact label={t("community_activities.open_activities")} value={board?.openActivities ?? 0} supportingText={t("community_activities.stats_support")} />
          <CivicStatCard compact label={t("community_activities.my_signups")} value={board?.myUpcomingSignups ?? 0} supportingText={t("community_activities.stats_support")} />
        </div>

        <div className="grid mt-6">
          <div className="col-12 xl:col-7">
            <CivicCard title={t("community_activities.activities_title")} data-testid="community-activities-list-card">
              {loading && activities.length === 0 ? (
                <p className="text-secondary m-0">{t("common.loading")}</p>
              ) : activities.length === 0 ? (
                <CivicEmptyState
                  icon="pi pi-calendar"
                  title={t("community_activities.empty_title")}
                  description={t("community_activities.empty_desc")}
                />
              ) : (
                <div className="flex flex-column gap-4">
                  {activities.map((activity) => (
                    <div
                      key={activity.id}
                      className="border-round-xl border-1 border-surface-soft p-4"
                      data-testid={`community-activity-${activity.id}`}
                    >
                      <div className="flex align-items-center gap-2 flex-wrap">
                        <span className="font-black text-main text-lg">{activity.title}</span>
                        <CivicBadge
                          label={t(`community_activities.states.${activity.signupWindowState}`)}
                          severity={windowBadgeSeverity(activity.signupWindowState)}
                        />
                        {activity.currentVolunteerStatus === "CONFIRMED" && (
                          <CivicBadge label={t("community_activities.my_signups")} severity="new" />
                        )}
                      </div>

                      <p className="text-sm text-secondary mt-3 mb-0 line-height-3">{activity.description}</p>

                      <div className="u-surface-note mt-3">
                        <div className="u-eyebrow mb-2">{t("community_activities.window")}</div>
                        <p className="text-sm text-secondary m-0 line-height-3">
                          {t("community_activities.window_help", {
                            opens: formatDateTime(activity.signupOpensAt),
                            closes: formatDateTime(activity.signupClosesAt),
                          })}
                        </p>
                        {!activity.signupOpen && activity.fullReason && (
                          <p className="text-sm text-status-progress mt-2 mb-0 line-height-3">
                            {translateBlockReason(activity)}
                          </p>
                        )}
                      </div>

                      <div className="flex align-items-center gap-4 flex-wrap mt-3">
                        <span className="text-xs text-muted">
                          {t("community_activities.confirmed_count")}: {activity.confirmedCount}/{activity.signupCapacity}
                        </span>
                        <span className="text-xs text-muted">
                          {t("community_activities.fill_rate")}: {activity.fillRatePercent}%
                        </span>
                        <span className="text-xs text-muted">{formatDateTime(activity.startsAt)}</span>
                        <span className="text-xs text-muted">{activity.locationLabel}</span>
                      </div>

                      {canJoin && (
                        <div className="mt-3">
                          <CivicButton
                            type="button"
                            icon={activity.currentVolunteerStatus === "CONFIRMED" ? "pi pi-times" : "pi pi-check"}
                            label={
                              activity.currentVolunteerStatus === "CONFIRMED"
                                ? t("community_activities.leave")
                                : t("community_activities.join")
                            }
                            variant={activity.currentVolunteerStatus === "CONFIRMED" ? "secondary" : "primary"}
                            disabled={!activity.signupOpen && activity.currentVolunteerStatus !== "CONFIRMED"}
                            loading={busyActivityId === activity.id}
                            onClick={() => onToggleSignup(activity)}
                            data-testid={`community-activity-signup-${activity.id}`}
                          />
                        </div>
                      )}

                      {activity.roster.length > 0 && (
                        <div className="mt-4" data-testid={`community-activity-roster-${activity.id}`}>
                          <div className="u-eyebrow mb-2">{t("community_activities.roster_title")}</div>
                          <div className="flex flex-column gap-2">
                            {activity.roster.map((volunteer) => (
                              <div key={volunteer.signupId} className="flex align-items-center gap-2 flex-wrap">
                                <span className="text-sm text-main">{volunteer.volunteerName}</span>
                                {volunteer.status === "CANCELLED" && (
                                  <CivicBadge label={t("community_activities.leave")} severity="neutral" />
                                )}
                                <CivicBadge
                                  label={t(`community_activities.attendance.${volunteer.attendanceStatus}`)}
                                  severity={volunteer.attendanceStatus === "ATTENDED" ? "resolved" : "neutral"}
                                />
                                {canManage && volunteer.status === "CONFIRMED" && (
                                  <span className="flex align-items-center gap-1">
                                    <CivicButton
                                      type="button"
                                      icon="pi pi-check"
                                      variant="ghost"
                                      label={t("community_activities.attendance.mark_attended")}
                                      onClick={() => onRecordAttendance(activity, volunteer.signupId, true)}
                                      data-testid={`community-activity-attended-${volunteer.signupId}`}
                                    />
                                    <CivicButton
                                      type="button"
                                      icon="pi pi-times"
                                      variant="ghost"
                                      label={t("community_activities.attendance.mark_no_show")}
                                      onClick={() => onRecordAttendance(activity, volunteer.signupId, false)}
                                      data-testid={`community-activity-no-show-${volunteer.signupId}`}
                                    />
                                  </span>
                                )}
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </CivicCard>
          </div>

          {canManage && (
            <div className="col-12 xl:col-5">
              <CivicCard title={t("community_activities.create_title")} data-testid="community-activities-create-card">
                <form className="flex flex-column gap-2" onSubmit={handleSubmit(onSubmit)}>
                  <CivicField label={t("community_activities.create_title_label")} error={errors.title?.message}>
                    <Controller
                      name="title"
                      control={control}
                      rules={{
                        required: t("community_activities.title_required"),
                        minLength: { value: 3, message: t("community_activities.title_too_short") },
                      }}
                      render={({ field }) => (
                        <InputText
                          {...field}
                          value={field.value ?? ""}
                          onChange={(e) => field.onChange(e.target.value)}
                          className="w-full"
                          placeholder={t("community_activities.create_title_placeholder")}
                          data-testid="community-activities-title"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_activities.create_description_label")} error={errors.description?.message}>
                    <Controller
                      name="description"
                      control={control}
                      rules={{ required: t("community_activities.description_required") }}
                      render={({ field }) => (
                        <InputTextarea
                          {...field}
                          value={field.value ?? ""}
                          onChange={(e) => field.onChange(e.target.value)}
                          className="w-full"
                          rows={3}
                          placeholder={t("community_activities.create_description_placeholder")}
                          data-testid="community-activities-description"
                        />
                      )}
                    />
                  </CivicField>
                  <CivicCharacterCount current={watchedDescription.length} max={DESCRIPTION_MAX} min={1} />

                  <CivicField label={t("community_activities.create_location_label")} error={errors.locationLabel?.message}>
                    <Controller
                      name="locationLabel"
                      control={control}
                      rules={{ required: t("community_activities.location_required") }}
                      render={({ field }) => (
                        <InputText
                          {...field}
                          value={field.value ?? ""}
                          onChange={(e) => field.onChange(e.target.value)}
                          className="w-full"
                          placeholder={t("community_activities.create_location_placeholder")}
                          data-testid="community-activities-location"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_activities.create_starts_label")} error={errors.startsAt?.message}>
                    <Controller
                      name="startsAt"
                      control={control}
                      rules={{ required: t("community_activities.schedule_error") }}
                      render={({ field }) => (
                        <Calendar
                          appendTo={document.body}
                          value={field.value ?? null}
                          onChange={(e) => field.onChange(e.value ?? null)}
                          showIcon
                          showTime
                          hourFormat="24"
                          className="w-full"
                          data-testid="community-activities-starts"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_activities.create_ends_label")} error={errors.endsAt?.message}>
                    <Controller
                      name="endsAt"
                      control={control}
                      rules={{ required: t("community_activities.schedule_error") }}
                      render={({ field }) => (
                        <Calendar
                          appendTo={document.body}
                          value={field.value ?? null}
                          onChange={(e) => field.onChange(e.value ?? null)}
                          showIcon
                          showTime
                          hourFormat="24"
                          className="w-full"
                          data-testid="community-activities-ends"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_activities.create_opens_label")} error={errors.signupOpensAt?.message}>
                    <Controller
                      name="signupOpensAt"
                      control={control}
                      rules={{ required: t("community_activities.schedule_error") }}
                      render={({ field }) => (
                        <Calendar
                          appendTo={document.body}
                          value={field.value ?? null}
                          onChange={(e) => field.onChange(e.value ?? null)}
                          showIcon
                          showTime
                          hourFormat="24"
                          className="w-full"
                          data-testid="community-activities-opens"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_activities.create_closes_label")} error={errors.signupClosesAt?.message}>
                    <Controller
                      name="signupClosesAt"
                      control={control}
                      rules={{ required: t("community_activities.schedule_error") }}
                      render={({ field }) => (
                        <Calendar
                          appendTo={document.body}
                          value={field.value ?? null}
                          onChange={(e) => field.onChange(e.value ?? null)}
                          showIcon
                          showTime
                          hourFormat="24"
                          className="w-full"
                          data-testid="community-activities-closes"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_activities.create_capacity_label")} error={errors.signupCapacity?.message}>
                    <Controller
                      name="signupCapacity"
                      control={control}
                      rules={{
                        required: t("community_activities.capacity_required"),
                        min: { value: 1, message: t("community_activities.capacity_min") },
                      }}
                      render={({ field }) => (
                        <InputNumber
                          value={field.value ?? 0}
                          onValueChange={(e) => field.onChange(e.value ?? 0)}
                          min={1}
                          className="w-full"
                          data-testid="community-activities-capacity"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicButton
                    type="submit"
                    icon="pi pi-calendar-plus"
                    label={t("community_activities.create_submit")}
                    loading={submitting}
                    data-testid="community-activities-create-submit"
                  />
                </form>
              </CivicCard>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}