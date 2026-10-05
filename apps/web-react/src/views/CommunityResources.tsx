import { useCallback, useEffect, useMemo, useState, type ChangeEvent } from "react";
import { Controller, useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { Calendar } from "primereact/calendar";
import { InputNumber } from "primereact/inputnumber";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { Checkbox, type CheckboxChangeEvent } from "primereact/checkbox";
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
import { CivicStatCard } from "../components/ui/CivicStatCard";
import { useCommunityStore } from "../store/useCommunityStore";
import type {
  CommunityPermissionPolicy,
  CommunityResource,
  CommunityResourceBoard,
  CommunityResourceBooking,
} from "../types";

type ApiError = Error & { friendlyMessage?: string };

type BookingForm = { purpose: string; startsAt: Date | null; endsAt: Date | null };
type ResourceForm = {
  name: string;
  description: string;
  locationLabel: string;
  requiresApproval: boolean;
  minNoticeHours: number;
  maxBookingHours: number;
};

const bookingDefaults: BookingForm = { purpose: "", startsAt: null, endsAt: null };
const resourceDefaults: ResourceForm = {
  name: "",
  description: "",
  locationLabel: "",
  requiresApproval: true,
  minNoticeHours: 0,
  maxBookingHours: 8,
};

const statusSeverity = (status: CommunityResourceBooking["status"]) => {
  if (status === "APPROVED") return "resolved" as const;
  if (status === "PENDING_APPROVAL") return "progress" as const;
  if (status === "REJECTED") return "rejected" as const;
  return "neutral" as const;
};

export function CommunityResources() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership = memberships.find((m) => m.communityId === activeCommunityId) ?? null;

  const [board, setBoard] = useState<CommunityResourceBoard | null>(null);
  const [policies, setPolicies] = useState<CommunityPermissionPolicy[]>([]);
  const [selectedResourceId, setSelectedResourceId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [creating, setCreating] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);

  const {
    control: bookingControl,
    handleSubmit: submitBooking,
    reset: resetBooking,
    formState: { errors: bookingErrors },
  } = useForm<BookingForm>({ mode: "onChange", defaultValues: bookingDefaults });

  const {
    control: resourceControl,
    handleSubmit: submitResource,
    reset: resetResource,
    formState: { errors: resourceErrors },
  } = useForm<ResourceForm>({ mode: "onChange", defaultValues: resourceDefaults });

  const managePolicy = useMemo(
    () => policies.find((policy) => policy.scope === "MANAGE_RESOURCES"),
    [policies]
  );
  const bookPolicy = useMemo(
    () => policies.find((policy) => policy.scope === "BOOK_RESOURCES"),
    [policies]
  );
  const canManage = Boolean(activeMembership && managePolicy?.allowedRoles.includes(activeMembership.role));
  const canBook = Boolean(activeMembership && bookPolicy?.allowedRoles.includes(activeMembership.role));

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
      const response = await apiClient.get<CommunityResourceBoard>(
        `community/resources/board?communityId=${activeCommunityId}`
      );
      setBoard(response.data);
      setSelectedResourceId((current) => current ?? response.data.resources[0]?.id ?? null);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_resources.load_error"));
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

  const formatDateTime = (value?: string | null) => formatStamp(value) ?? "-";

  const selectedResource = board?.resources.find((item) => item.id === selectedResourceId) ?? null;

  const onRequestBooking = async (values: BookingForm) => {
    if (!activeCommunityId || !selectedResourceId) return;
    if (!values.startsAt || !values.endsAt) {
      toast.error(t("community_resources.schedule_error"));
      return;
    }
    setSubmitting(true);
    try {
      await apiClient.post("community/resources/bookings", {
        communityId: activeCommunityId,
        resourceId: selectedResourceId,
        purpose: values.purpose.trim(),
        startsAt: values.startsAt.toISOString(),
        endsAt: values.endsAt.toISOString(),
      });
      toast.success(t("community_resources.request_success"));
      resetBooking(bookingDefaults);
      await loadBoard();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_resources.request_error"));
    } finally {
      setSubmitting(false);
    }
  };

  const onCancelBooking = async (booking: CommunityResourceBooking) => {
    if (!activeCommunityId) return;
    setBusyId(booking.id);
    try {
      await apiClient.delete(`community/resources/bookings/${booking.id}?communityId=${activeCommunityId}`);
      toast.success(t("community_resources.cancel_success"));
      await loadBoard();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_resources.cancel_error"));
    } finally {
      setBusyId(null);
    }
  };

  const onDecide = async (booking: CommunityResourceBooking, approve: boolean, note: string) => {
    if (!activeCommunityId) return;
    setBusyId(booking.id);
    try {
      await apiClient.patch("community/resources/bookings", {
        communityId: activeCommunityId,
        bookingId: booking.id,
        approve,
        decisionNote: note.trim() || undefined,
      });
      toast.success(t("community_resources.decision_success"));
      await loadBoard();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_resources.decision_error"));
    } finally {
      setBusyId(null);
    }
  };

  const onArchive = async (resource: CommunityResource) => {
    if (!activeCommunityId) return;
    setBusyId(resource.id);
    try {
      const response = await apiClient.delete<CommunityResourceBoard>(
        `community/resources/${resource.id}?communityId=${activeCommunityId}`
      );
      setBoard(response.data);
      if (selectedResourceId === resource.id) {
        setSelectedResourceId(response.data.resources[0]?.id ?? null);
      }
      toast.success(t("community_resources.create_success"));
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_resources.create_error"));
    } finally {
      setBusyId(null);
    }
  };

  const onSubmitResource = async (values: ResourceForm) => {
    if (!activeCommunityId) return;
    setCreating(true);
    try {
      const response = await apiClient.post<CommunityResourceBoard>("community/resources", {
        communityId: activeCommunityId,
        name: values.name.trim(),
        description: values.description.trim(),
        locationLabel: values.locationLabel.trim(),
        requiresApproval: values.requiresApproval,
        minNoticeHours: Number(values.minNoticeHours),
        maxBookingHours: Number(values.maxBookingHours),
      });
      setBoard(response.data);
      setSelectedResourceId(response.data.resources[0]?.id ?? null);
      toast.success(t("community_resources.create_success"));
      resetResource(resourceDefaults);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_resources.create_error"));
    } finally {
      setCreating(false);
    }
  };

  if (!activeCommunityId || !activeMembership) {
    return (
      <Layout>
        <CivicCard>
          <CivicEmptyState
            icon="pi pi-building"
            title={t("community_resources.no_context_title")}
            description={t("community_resources.no_context_desc")}
            actionLabel={t("nav.communities")}
            onAction={() => navigate("/communities")}
          />
        </CivicCard>
      </Layout>
    );
  }

  const resources = board?.resources ?? [];
  const myBookings = board?.myBookings ?? [];
  const approvals = board?.approvalsQueue ?? [];

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column xl:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("community_resources.title")}
            description={t("community_resources.desc", { community: activeMembership.communityName })}
            className="mb-0"
          />
          <CivicActionBar className="w-full xl:w-auto">
            <div className="community-home-action-copy">
              <div className="u-eyebrow">{t("community_resources.kicker")}</div>
              <p className="u-section-copy text-sm m-0">{t("community_resources.kicker_desc")}</p>
            </div>
            <div className="dashboard-action-cluster">
              <CivicButton
                type="button"
                icon="pi pi-refresh"
                label={t("community_resources.refresh")}
                variant="secondary"
                onClick={loadBoard}
                data-testid="community-resources-refresh"
              />
              <CivicButton
                type="button"
                icon="pi pi-calendar"
                label={t("nav.community_activities")}
                variant="ghost"
                onClick={() => navigate("/communities/activities")}
              />
            </div>
          </CivicActionBar>
        </div>

        <div className="civic-stat-grid civic-stat-grid-comfortable" data-testid="community-resources-stats-grid">
          <CivicStatCard compact label={t("community_resources.resource_count")} value={board?.resourceCount ?? 0} supportingText={t("community_resources.stats_support")} />
          <CivicStatCard compact label={t("community_resources.pending_approvals")} value={board?.pendingApprovals ?? 0} supportingText={t("community_resources.stats_support")} />
        </div>

        <div className="grid mt-6">
          <div className="col-12 xl:col-5 flex flex-column gap-6">
            <CivicCard title={t("community_resources.resources_title")} data-testid="community-resources-list-card">
              {loading && resources.length === 0 ? (
                <p className="text-secondary m-0">{t("common.loading")}</p>
              ) : resources.length === 0 ? (
                <CivicEmptyState
                  icon="pi pi-building"
                  title={t("community_resources.empty_title")}
                  description={t("community_resources.empty_desc")}
                />
              ) : (
                <div className="flex flex-column gap-3">
                  {resources.map((resource) => (
                    <button
                      key={resource.id}
                      type="button"
                      onClick={() => setSelectedResourceId(resource.id)}
                      data-testid={`community-resource-${resource.id}`}
                      aria-current={resource.id === selectedResourceId}
                      className={`border-round-xl border-1 p-4 text-left w-full ${
                        resource.id === selectedResourceId
                          ? "border-brand-primary bg-surface-soft"
                          : "border-surface-soft bg-transparent"
                      }`}
                    >
                      <div className="flex align-items-center gap-2 flex-wrap">
                        <span className="font-black text-main">{resource.name}</span>
                        <CivicBadge
                          label={t(
                            resource.requiresApproval
                              ? "community_resources.policy_requires_approval"
                              : "community_resources.policy_auto"
                          )}
                          severity={resource.requiresApproval ? "progress" : "resolved"}
                        />
                      </div>
                      <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{resource.description}</p>
                      <small className="text-muted">{resource.locationLabel}</small>
                    </button>
                  ))}
                </div>
              )}
            </CivicCard>

            <CivicCard title={t("community_resources.my_bookings_title")} data-testid="community-resources-my-bookings">
              {myBookings.length === 0 ? (
                <p className="text-secondary m-0">{t("community_resources.no_upcoming_bookings")}</p>
              ) : (
                <div className="flex flex-column gap-3">
                  {myBookings.map((booking) => (
                    <BookingRow
                      key={booking.id}
                      booking={booking}
                      busy={busyId === booking.id}
                      onCancel={() => onCancelBooking(booking)}
                    />
                  ))}
                </div>
              )}
            </CivicCard>

            {canManage && (
              <CivicCard title={t("community_resources.approvals_title")} data-testid="community-resources-approvals">
                {approvals.length === 0 ? (
                  <p className="text-secondary m-0">{t("community_resources.approvals_empty")}</p>
                ) : (
                  <div className="flex flex-column gap-3">
                    {approvals.map((booking) => (
                      <ApprovalRow
                        key={booking.id}
                        booking={booking}
                        busy={busyId === booking.id}
                        onDecide={(approve, note) => onDecide(booking, approve, note)}
                        formatDateTime={formatDateTime}
                        labels={{
                          approve: t("community_resources.approve"),
                          reject: t("community_resources.reject"),
                          note: t("community_resources.decision_note_label"),
                          notePlaceholder: t("community_resources.decision_note_placeholder"),
                        }}
                      />
                    ))}
                  </div>
                )}
              </CivicCard>
            )}
          </div>

          <div className="col-12 xl:col-7 flex flex-column gap-6">
            {selectedResource && (
              <>
                <CivicCard title={selectedResource.name} data-testid="community-resources-detail-card">
                  <div className="flex flex-column gap-4">
                    <p className="text-secondary m-0 line-height-3">{selectedResource.description}</p>

                    <div className="u-surface-note">
                      <div className="u-eyebrow mb-2">{t("community_resources.policy")}</div>
                      <ul className="m-0 pl-3 flex flex-column gap-1">
                        <li className="text-sm text-secondary">
                          {selectedResource.requiresApproval
                            ? t("community_resources.policy_requires_approval")
                            : t("community_resources.policy_auto")}
                        </li>
                        <li className="text-sm text-secondary">
                          {t("community_resources.policy_notice", { hours: selectedResource.minNoticeHours })}
                        </li>
                        <li className="text-sm text-secondary">
                          {t("community_resources.policy_max_duration", { hours: selectedResource.maxBookingHours })}
                        </li>
                      </ul>
                    </div>

                    <div>
                      <div className="u-eyebrow mb-2">{t("community_resources.upcoming_bookings")}</div>
                      {selectedResource.upcomingBookings.length === 0 ? (
                        <p className="text-secondary m-0" data-testid="community-resources-no-bookings">
                          {t("community_resources.no_upcoming_bookings")}
                        </p>
                      ) : (
                        <div className="flex flex-column gap-2">
                          {selectedResource.upcomingBookings.map((booking) => (
                            <div key={booking.id} className="border-round-xl border-1 border-surface-soft p-3">
                              <div className="flex align-items-center gap-2 flex-wrap">
                                <span className="text-sm text-main">{booking.requesterName}</span>
                                <CivicBadge
                                  label={t(`community_resources.statuses.${booking.status}`)}
                                  severity={statusSeverity(booking.status)}
                                />
                              </div>
                              <small className="text-muted">
                                {formatDateTime(booking.startsAt)} → {formatDateTime(booking.endsAt)}
                              </small>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>

                    {canManage && (
                      <div>
                        <CivicButton
                          type="button"
                          icon="pi pi-inbox"
                          label={t("community_resources.archive_resource")}
                          variant="ghost"
                          loading={busyId === selectedResource.id}
                          onClick={() => onArchive(selectedResource)}
                          data-testid="community-resources-archive"
                        />
                      </div>
                    )}
                  </div>
                </CivicCard>

                {canBook && (
                  <CivicCard title={t("community_resources.request_title")} data-testid="community-resources-request-card">
                    <form className="flex flex-column gap-2" onSubmit={submitBooking(onRequestBooking)}>
                      <CivicField
                        label={t("community_resources.request_purpose_label")}
                        error={bookingErrors.purpose?.message}
                      >
                        <Controller
                          name="purpose"
                          control={bookingControl}
                          rules={{ required: t("community_resources.request_purpose_label") }}
                          render={({ field }) => (
                            <InputText
                              {...field}
                              value={field.value ?? ""}
                              onChange={(e) => field.onChange(e.target.value)}
                              className="w-full"
                              placeholder={t("community_resources.request_purpose_placeholder")}
                              data-testid="community-resources-purpose"
                            />
                          )}
                        />
                      </CivicField>

                      <CivicField label={t("community_resources.request_starts_label")} error={bookingErrors.startsAt?.message}>
                        <Controller
                          name="startsAt"
                          control={bookingControl}
                          rules={{ required: t("community_resources.schedule_error") }}
                          render={({ field }) => (
                            <Calendar
                              appendTo={document.body}
                              value={field.value ?? null}
                              onChange={(e) => field.onChange(e.value ?? null)}
                              showIcon
                              showTime
                              hourFormat="24"
                              className="w-full"
                              data-testid="community-resources-starts"
                            />
                          )}
                        />
                      </CivicField>

                      <CivicField label={t("community_resources.request_ends_label")} error={bookingErrors.endsAt?.message}>
                        <Controller
                          name="endsAt"
                          control={bookingControl}
                          rules={{ required: t("community_resources.schedule_error") }}
                          render={({ field }) => (
                            <Calendar
                              appendTo={document.body}
                              value={field.value ?? null}
                              onChange={(e) => field.onChange(e.value ?? null)}
                              showIcon
                              showTime
                              hourFormat="24"
                              className="w-full"
                              data-testid="community-resources-ends"
                            />
                          )}
                        />
                      </CivicField>

                      <CivicButton
                        type="submit"
                        icon="pi pi-calendar-plus"
                        label={t("community_resources.request_submit")}
                        loading={submitting}
                        data-testid="community-resources-submit"
                      />
                    </form>
                  </CivicCard>
                )}
              </>
            )}

            {canManage && (
              <CivicCard title={t("community_resources.create_title")} data-testid="community-resources-create-card">
                <form className="flex flex-column gap-2" onSubmit={submitResource(onSubmitResource)}>
                  <CivicField label={t("community_resources.create_name_label")} error={resourceErrors.name?.message}>
                    <Controller
                      name="name"
                      control={resourceControl}
                      rules={{
                        required: t("community_resources.name_required"),
                        minLength: { value: 3, message: t("community_resources.name_too_short") },
                      }}
                      render={({ field }) => (
                        <InputText
                          {...field}
                          value={field.value ?? ""}
                          onChange={(e) => field.onChange(e.target.value)}
                          className="w-full"
                          placeholder={t("community_resources.create_name_placeholder")}
                          data-testid="community-resources-name"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField
                    label={t("community_resources.create_description_label")}
                    error={resourceErrors.description?.message}
                  >
                    <Controller
                      name="description"
                      control={resourceControl}
                      rules={{ required: t("community_resources.description_required") }}
                      render={({ field }) => (
                        <InputTextarea
                          {...field}
                          value={field.value ?? ""}
                          onChange={(e) => field.onChange(e.target.value)}
                          className="w-full"
                          rows={3}
                          placeholder={t("community_resources.create_description_placeholder")}
                          data-testid="community-resources-description"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_resources.create_location_label")} error={resourceErrors.locationLabel?.message}>
                    <Controller
                      name="locationLabel"
                      control={resourceControl}
                      rules={{ required: t("community_resources.location_required") }}
                      render={({ field }) => (
                        <InputText
                          {...field}
                          value={field.value ?? ""}
                          onChange={(e) => field.onChange(e.target.value)}
                          className="w-full"
                          placeholder={t("community_resources.create_location_placeholder")}
                          data-testid="community-resources-location"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_resources.create_approval_label")}>
                    <Controller
                      name="requiresApproval"
                      control={resourceControl}
                      render={({ field }) => (
                        <Checkbox
                          inputId="community-resources-requires-approval"
                          checked={Boolean(field.value)}
                          onChange={(e: CheckboxChangeEvent) => field.onChange(e.value)}
                          data-testid="community-resources-requires-approval"
                        />
                      )}
                    />
                  </CivicField>

                  <div className="grid">
                    <div className="col-12 md:col-6">
                      <CivicField label={t("community_resources.create_min_notice_label")}>
                        <Controller
                          name="minNoticeHours"
                          control={resourceControl}
                          render={({ field }) => (
                            <InputNumber
                              value={field.value ?? 0}
                              onValueChange={(e) => field.onChange(e.value ?? 0)}
                              min={0}
                              className="w-full"
                              data-testid="community-resources-min-notice"
                            />
                          )}
                        />
                      </CivicField>
                    </div>
                    <div className="col-12 md:col-6">
                      <CivicField label={t("community_resources.create_max_duration_label")}>
                        <Controller
                          name="maxBookingHours"
                          control={resourceControl}
                          render={({ field }) => (
                            <InputNumber
                              value={field.value ?? 0}
                              onValueChange={(e) => field.onChange(e.value ?? 0)}
                              min={1}
                              className="w-full"
                              data-testid="community-resources-max-duration"
                            />
                          )}
                        />
                      </CivicField>
                    </div>
                  </div>

                  <CivicButton
                    type="submit"
                    icon="pi pi-plus"
                    label={t("community_resources.create_submit")}
                    loading={creating}
                    data-testid="community-resources-create-submit"
                  />
                </form>
              </CivicCard>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}

function BookingRow({
  booking,
  busy,
  onCancel,
}: {
  booking: CommunityResourceBooking;
  busy: boolean;
  onCancel: () => void;
}) {
  const { t } = useTranslation();
  return (
    <div className="border-round-xl border-1 border-surface-soft p-3" data-testid={`community-resources-my-booking-${booking.id}`}>
      <div className="flex align-items-center gap-2 flex-wrap">
        <span className="font-black text-main">{booking.resourceName}</span>
        <CivicBadge label={t(`community_resources.statuses.${booking.status}`)} severity={statusSeverity(booking.status)} />
      </div>
      <p className="text-sm text-secondary mt-2 mb-0">{booking.purpose}</p>
      <small className="text-muted">
        {formatStamp(booking.startsAt) ?? "-"} → {formatStamp(booking.endsAt) ?? "-"}
      </small>
      {booking.decisionNote && (
        <p className="text-sm text-secondary mt-2 mb-0">
          {t("community_resources.decision_note_label")}: {booking.decisionNote}
        </p>
      )}
      {booking.status !== "CANCELLED" && booking.status !== "REJECTED" && (
        <CivicButton
          type="button"
          icon="pi pi-times"
          label={t("community_resources.cancel_booking")}
          variant="ghost"
          loading={busy}
          onClick={onCancel}
          data-testid={`community-resources-cancel-${booking.id}`}
        />
      )}
    </div>
  );
}

function ApprovalRow({
  booking,
  busy,
  onDecide,
  formatDateTime,
  labels,
}: {
  booking: CommunityResourceBooking;
  busy: boolean;
  onDecide: (approve: boolean, note: string) => void;
  formatDateTime: (value?: string | null) => string;
  labels: { approve: string; reject: string; note: string; notePlaceholder: string };
}) {
  const [note, setNote] = useState("");
  return (
    <div className="border-round-xl border-1 border-surface-soft p-3" data-testid={`community-resources-approval-${booking.id}`}>
      <div className="flex align-items-center gap-2 flex-wrap">
        <span className="font-black text-main">{booking.resourceName}</span>
        <span className="text-sm text-secondary">{booking.requesterName}</span>
        <CivicBadge label={booking.status} severity={statusSeverity(booking.status)} />
      </div>
      <p className="text-sm text-secondary mt-2 mb-0">{booking.purpose}</p>
      <small className="text-muted">
        {formatDateTime(booking.startsAt)} → {formatDateTime(booking.endsAt)}
      </small>
      <InputText
        value={note}
        onChange={(e: ChangeEvent<HTMLInputElement>) => setNote(e.target.value)}
        className="w-full mt-2"
        placeholder={labels.notePlaceholder}
        aria-label={labels.note}
        data-testid={`community-resources-note-${booking.id}`}
      />
      <div className="flex align-items-center gap-2 flex-wrap mt-2">
        <CivicButton
          type="button"
          icon="pi pi-check"
          label={labels.approve}
          loading={busy}
          onClick={() => onDecide(true, note)}
          data-testid={`community-resources-approve-${booking.id}`}
        />
        <CivicButton
          type="button"
          icon="pi pi-times"
          label={labels.reject}
          variant="secondary"
          loading={busy}
          onClick={() => onDecide(false, note)}
          data-testid={`community-resources-reject-${booking.id}`}
        />
      </div>
    </div>
  );
}