import { useCallback, useEffect, useMemo, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { classNames } from "primereact/utils";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { Layout } from "../components/Layout";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicCharacterCount } from "../components/ui/CivicCharacterCount";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicField } from "../components/ui/CivicField";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicSelect } from "../components/ui/CivicSelect";
import { CivicStatCard } from "../components/ui/CivicStatCard";
import { FORM_LIMITS } from "../constants/formLimits";
import { useCommunityStore } from "../store/useCommunityStore";
import type { CommunityProjectBoard, CommunityProjectStatus, CommunityProposal } from "../types";

type ApiError = Error & { friendlyMessage?: string };

type ProjectBoardForm = {
  linkedProposalId: string | null;
  title: string;
  summary: string;
  dueDate: string;
};

type ProjectTaskForm = {
  title: string;
  details: string;
  assigneeUsername: string;
  dueDate: string;
};

const defaultBoardValues: ProjectBoardForm = {
  linkedProposalId: null,
  title: "",
  summary: "",
  dueDate: "",
};

const defaultTaskValues: ProjectTaskForm = {
  title: "",
  details: "",
  assigneeUsername: "",
  dueDate: "",
};

export function CommunityProjects() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership = memberships.find((membership) => membership.communityId === activeCommunityId) ?? null;
  const [boards, setBoards] = useState<CommunityProjectBoard[]>([]);
  const [selectedBoardId, setSelectedBoardId] = useState<string | null>(null);
  const [proposalOptions, setProposalOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [loadingBoards, setLoadingBoards] = useState(false);
  const [updatingTaskId, setUpdatingTaskId] = useState<string | null>(null);
  const [commentDrafts, setCommentDrafts] = useState<Record<string, string>>({});
  const [commentingTaskId, setCommentingTaskId] = useState<string | null>(null);

  const {
    control: boardControl,
    handleSubmit: handleBoardSubmit,
    reset: resetBoard,
    watch: watchBoard,
    formState: { errors: boardErrors, isSubmitting: isSubmittingBoard },
  } = useForm<ProjectBoardForm>({
    mode: "onChange",
    defaultValues: defaultBoardValues,
  });

  const {
    control: taskControl,
    handleSubmit: handleTaskSubmit,
    reset: resetTask,
    watch: watchTask,
    formState: { errors: taskErrors, isSubmitting: isSubmittingTask },
  } = useForm<ProjectTaskForm>({
    mode: "onChange",
    defaultValues: defaultTaskValues,
  });

  const selectedBoard = useMemo(
    () => boards.find((board) => board.id === selectedBoardId) ?? boards[0] ?? null,
    [boards, selectedBoardId]
  );

  const watchedBoardTitle = watchBoard("title") ?? "";
  const watchedBoardSummary = watchBoard("summary") ?? "";
  const watchedTaskTitle = watchTask("title") ?? "";
  const watchedTaskDetails = watchTask("details") ?? "";

  const canManageBoards =
    activeMembership?.role === "COORDINATOR" || activeMembership?.role === "PUBLIC_SERVANT_LIAISON";

  const loadBoards = useCallback(async () => {
    if (!activeCommunityId) {
      setBoards([]);
      setSelectedBoardId(null);
      return;
    }
    setLoadingBoards(true);
    try {
      const response = await apiClient.get<CommunityProjectBoard[]>(`community/projects?communityId=${activeCommunityId}`);
      const nextBoards = response.data ?? [];
      setBoards(nextBoards);
      setSelectedBoardId((current) => {
        if (nextBoards.length === 0) {
          return null;
        }
        return nextBoards.some((board) => board.id === current) ? current : nextBoards[0].id;
      });
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_projects.load_error"));
    } finally {
      setLoadingBoards(false);
    }
  }, [activeCommunityId, t]);

  const loadProposalOptions = useCallback(async () => {
    if (!activeCommunityId) {
      setProposalOptions([]);
      return;
    }
    try {
      const response = await apiClient.get<CommunityProposal[]>(`community/proposals?communityId=${activeCommunityId}`);
      setProposalOptions((response.data ?? []).map((proposal) => ({
        label: proposal.title,
        value: proposal.id,
      })));
    } catch {
      setProposalOptions([]);
    }
  }, [activeCommunityId]);

  useEffect(() => {
    loadBoards();
    loadProposalOptions();
  }, [loadBoards, loadProposalOptions]);

  const onSubmitBoard = async (data: ProjectBoardForm) => {
    if (!activeCommunityId) {
      toast.error(t("community_projects.community_required"));
      return;
    }
    try {
      const response = await apiClient.post<CommunityProjectBoard>("community/projects", {
        communityId: activeCommunityId,
        linkedProposalId: data.linkedProposalId || null,
        title: data.title,
        summary: data.summary,
        dueDate: data.dueDate || null,
      });
      toast.success(t("community_projects.create_success"));
      resetBoard(defaultBoardValues);
      await loadBoards();
      setSelectedBoardId(response.data.id);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_projects.create_error"));
    }
  };

  const onSubmitTask = async (data: ProjectTaskForm) => {
    if (!selectedBoard) {
      return;
    }
    try {
      const response = await apiClient.post<CommunityProjectBoard>(`community/projects/${selectedBoard.id}/tasks`, {
        title: data.title,
        details: data.details,
        assigneeUsername: data.assigneeUsername.trim() || null,
        dueDate: data.dueDate || null,
      });
      setBoards((current) => current.map((board) => (board.id === response.data.id ? response.data : board)));
      resetTask(defaultTaskValues);
      toast.success(t("community_projects.task_create_success"));
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_projects.task_create_error"));
    }
  };

  const updateTaskStatus = async (
    taskId: string,
    title: string,
    details: string,
    status: CommunityProjectStatus,
    assigneeUsername?: string | null,
    dueDate?: string | null
  ) => {
    if (!selectedBoard) {
      return;
    }
    setUpdatingTaskId(taskId);
    try {
      const response = await apiClient.patch<CommunityProjectBoard>(`community/projects/${selectedBoard.id}/tasks/${taskId}`, {
        title,
        details,
        status,
        assigneeUsername: assigneeUsername || null,
        dueDate: dueDate || null,
      });
      setBoards((current) => current.map((board) => (board.id === response.data.id ? response.data : board)));
      toast.success(t("community_projects.task_update_success"));
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_projects.task_update_error"));
    } finally {
      setUpdatingTaskId(null);
    }
  };

  const addComment = async (taskId: string) => {
    if (!selectedBoard) {
      return;
    }
    const draft = (commentDrafts[taskId] ?? "").trim();
    if (draft.length < FORM_LIMITS.projects.commentMin) {
      toast.error(t("community_projects.comment_too_short"));
      return;
    }
    setCommentingTaskId(taskId);
    try {
      const response = await apiClient.post<CommunityProjectBoard>(
        `community/projects/${selectedBoard.id}/tasks/${taskId}/comments`,
        { content: draft }
      );
      setBoards((current) => current.map((board) => (board.id === response.data.id ? response.data : board)));
      setCommentDrafts((current) => ({ ...current, [taskId]: "" }));
      toast.success(t("community_projects.comment_success"));
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_projects.comment_error"));
    } finally {
      setCommentingTaskId(null);
    }
  };

  const columns = useMemo(
    () => [
      { status: "TODO" as const, title: t("community_projects.columns.todo") },
      { status: "IN_PROGRESS" as const, title: t("community_projects.columns.in_progress") },
      { status: "DONE" as const, title: t("community_projects.columns.done") },
    ],
    [t]
  );

  const statusLabels = useMemo<Record<CommunityProjectStatus, string>>(
    () => ({
      TODO: t("community_projects.columns.todo"),
      IN_PROGRESS: t("community_projects.columns.in_progress"),
      DONE: t("community_projects.columns.done"),
    }),
    [t]
  );

  const getNextStatus = (status: CommunityProjectStatus) => {
    if (status === "TODO") return "IN_PROGRESS";
    if (status === "IN_PROGRESS") return "DONE";
    return null;
  };

  const getPrevStatus = (status: CommunityProjectStatus) => {
    if (status === "DONE") return "IN_PROGRESS";
    if (status === "IN_PROGRESS") return "TODO";
    return null;
  };

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column xl:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("community_projects.title")}
            description={t("community_projects.desc", {
              community: activeMembership?.communityName ?? t("dashboard.community_default"),
            })}
            className="mb-0"
          />
          <CivicActionBar className="w-full xl:w-auto">
            <div className="community-home-action-copy">
              <div className="u-eyebrow">{t("community_projects.badge")}</div>
              <p className="u-section-copy text-sm m-0">{t("community_projects.badge_desc")}</p>
            </div>
            <div className="dashboard-action-cluster">
              <CivicButton type="button" icon="pi pi-file-edit" label={t("community_proposals.open_hub")} variant="ghost" onClick={() => navigate("/communities/proposals")} />
              <CivicButton type="button" icon="pi pi-comments" label={t("nav.dialogues")} variant="ghost" onClick={() => navigate("/communities/threads")} />
            </div>
          </CivicActionBar>
        </div>

        {!activeCommunityId ? (
          <CivicCard>
            <CivicEmptyState
              icon="pi-briefcase"
              title={t("community_projects.no_context_title")}
              description={t("community_projects.no_context_desc")}
              actionLabel={t("nav.communities")}
              onAction={() => navigate("/communities")}
            />
          </CivicCard>
        ) : (
          <div className="grid">
            <div className="col-12 xl:col-4">
              <CivicCard title={t("community_projects.create_title")} className="mb-6" data-testid="community-project-board-create-card">
                <form className="flex flex-column gap-3" onSubmit={handleBoardSubmit(onSubmitBoard)}>
                  <CivicField label={t("community_projects.linked_proposal_label")} helpText={t("community_projects.linked_proposal_help")}>
                    <Controller
                      name="linkedProposalId"
                      control={boardControl}
                      render={({ field }) => (
                        <CivicSelect
                          value={field.value}
                          onChange={(e) => field.onChange(e.value)}
                          options={proposalOptions}
                          placeholder={t("community_projects.linked_proposal_placeholder")}
                          showClear
                          className="w-full"
                          data-testid="project-board-linked-proposal-select"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_projects.title_label")} error={boardErrors.title?.message}>
                    <Controller
                      name="title"
                      control={boardControl}
                      rules={{
                        required: t("common.required"),
                        minLength: { value: FORM_LIMITS.projects.titleMin, message: t("community_projects.title_too_short") },
                        maxLength: { value: FORM_LIMITS.projects.titleMax, message: t("community_projects.title_too_long") },
                      }}
                      render={({ field, fieldState }) => (
                        <div className="flex flex-column gap-2">
                          <InputText
                            {...field}
                            onChange={(e) => field.onChange(e.target.value)}
                            className={classNames("w-full", { "p-invalid": fieldState.error })}
                            maxLength={FORM_LIMITS.projects.titleMax}
                            data-testid="project-board-title-input"
                          />
                          <CivicCharacterCount current={watchedBoardTitle.length} max={FORM_LIMITS.projects.titleMax} min={FORM_LIMITS.projects.titleMin} />
                        </div>
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_projects.summary_label")} error={boardErrors.summary?.message}>
                    <Controller
                      name="summary"
                      control={boardControl}
                      rules={{
                        required: t("common.required"),
                        minLength: { value: FORM_LIMITS.projects.summaryMin, message: t("community_projects.summary_too_short") },
                        maxLength: { value: FORM_LIMITS.projects.summaryMax, message: t("community_projects.summary_too_long") },
                      }}
                      render={({ field, fieldState }) => (
                        <div className="flex flex-column gap-2">
                          <InputTextarea
                            {...field}
                            rows={5}
                            onChange={(e) => field.onChange(e.target.value)}
                            className={classNames("w-full", { "p-invalid": fieldState.error })}
                            maxLength={FORM_LIMITS.projects.summaryMax}
                            data-testid="project-board-summary-input"
                          />
                          <CivicCharacterCount current={watchedBoardSummary.length} max={FORM_LIMITS.projects.summaryMax} min={FORM_LIMITS.projects.summaryMin} />
                        </div>
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_projects.due_date_label")} helpText={t("community_projects.due_date_help")}>
                    <Controller
                      name="dueDate"
                      control={boardControl}
                      render={({ field }) => (
                        <InputText {...field} type="date" onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="project-board-due-date-input" />
                      )}
                    />
                  </CivicField>

                  <div className="flex justify-content-end">
                    <CivicButton type="submit" icon="pi pi-briefcase" label={t("community_projects.create_action")} loading={isSubmittingBoard} disabled={!canManageBoards} data-testid="project-board-submit-button" />
                  </div>
                </form>
              </CivicCard>

              <CivicCard title={t("community_projects.board_list_title")} data-testid="community-project-board-list-card">
                {loadingBoards ? (
                  <p className="text-secondary m-0">{t("common.loading")}</p>
                ) : boards.length === 0 ? (
                  <CivicEmptyState icon="pi-briefcase" title={t("community_projects.empty_title")} description={t("community_projects.empty_desc")} />
                ) : (
                  <div className="flex flex-column gap-3">
                    {boards.map((board) => (
                      <button
                        key={board.id}
                        type="button"
                        className="community-feed-list-card text-left border-none bg-transparent p-0 cursor-pointer"
                        onClick={() => setSelectedBoardId(board.id)}
                        data-testid={`community-project-board-row-${board.id}`}
                      >
                        <div className="flex justify-content-between gap-3 align-items-start">
                          <div className="min-w-0 flex-1">
                            <div className="u-eyebrow">{t("community_projects.badge")}</div>
                            <h3 className="text-lg font-black text-main m-0 mt-2">{board.title}</h3>
                            <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{board.summary}</p>
                          </div>
                          <span className="u-pill">{board.taskCounts.todo + board.taskCounts.inProgress + board.taskCounts.done}</span>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </CivicCard>
            </div>
            <div className="col-12 xl:col-8">
              {selectedBoard ? (
                <div className="flex flex-column gap-4" data-testid="community-project-board-detail-card">
                  <CivicCard title={selectedBoard.title}>
                    <div className="civic-stat-grid civic-stat-grid-comfortable mb-4">
                      <CivicStatCard compact label={t("community_projects.stats.todo")} value={selectedBoard.taskCounts.todo} />
                      <CivicStatCard compact label={t("community_projects.stats.in_progress")} value={selectedBoard.taskCounts.inProgress} />
                      <CivicStatCard compact label={t("community_projects.stats.done")} value={selectedBoard.taskCounts.done} />
                      <CivicStatCard compact label={t("community_projects.stats.owner")} value={selectedBoard.ownerUsername} />
                    </div>
                    <div className="u-surface-note mb-4">
                      <p className="text-secondary m-0 line-height-3">{selectedBoard.summary}</p>
                    </div>
                    <div className="u-meta-row">
                      <span>{selectedBoard.linkedProposalTitle ?? t("community_projects.no_linked_proposal")}</span>
                      <span>{selectedBoard.dueDate ?? t("community_projects.no_due_date")}</span>
                    </div>
                  </CivicCard>

                  <CivicCard title={t("community_projects.task_create_title")} data-testid="community-project-task-create-card">
                    <form className="flex flex-column gap-3" onSubmit={handleTaskSubmit(onSubmitTask)}>
                      <div className="grid">
                        <div className="col-12 md:col-6">
                          <CivicField label={t("community_projects.task_title_label")} error={taskErrors.title?.message}>
                            <Controller
                              name="title"
                              control={taskControl}
                              rules={{
                                required: t("common.required"),
                                minLength: { value: FORM_LIMITS.projects.taskTitleMin, message: t("community_projects.task_title_too_short") },
                                maxLength: { value: FORM_LIMITS.projects.taskTitleMax, message: t("community_projects.task_title_too_long") },
                              }}
                              render={({ field, fieldState }) => (
                                <div className="flex flex-column gap-2">
                                  <InputText
                                    {...field}
                                    onChange={(e) => field.onChange(e.target.value)}
                                    className={classNames("w-full", { "p-invalid": fieldState.error })}
                                    maxLength={FORM_LIMITS.projects.taskTitleMax}
                                    data-testid="project-task-title-input"
                                  />
                                  <CivicCharacterCount current={watchedTaskTitle.length} max={FORM_LIMITS.projects.taskTitleMax} min={FORM_LIMITS.projects.taskTitleMin} />
                                </div>
                              )}
                            />
                          </CivicField>
                        </div>
                        <div className="col-12 md:col-3">
                          <CivicField label={t("community_projects.task_assignee_label")} helpText={t("community_projects.task_assignee_help")}>
                            <Controller
                              name="assigneeUsername"
                              control={taskControl}
                              render={({ field }) => (
                                <InputText {...field} onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="project-task-assignee-input" />
                              )}
                            />
                          </CivicField>
                        </div>
                        <div className="col-12 md:col-3">
                          <CivicField label={t("community_projects.due_date_label")}>
                            <Controller
                              name="dueDate"
                              control={taskControl}
                              render={({ field }) => (
                                <InputText {...field} type="date" onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="project-task-due-date-input" />
                              )}
                            />
                          </CivicField>
                        </div>
                      </div>
                      <CivicField label={t("community_projects.task_details_label")} error={taskErrors.details?.message}>
                        <Controller
                          name="details"
                          control={taskControl}
                          rules={{
                            required: t("common.required"),
                            minLength: { value: FORM_LIMITS.projects.taskDetailsMin, message: t("community_projects.task_details_too_short") },
                            maxLength: { value: FORM_LIMITS.projects.taskDetailsMax, message: t("community_projects.task_details_too_long") },
                          }}
                          render={({ field, fieldState }) => (
                            <div className="flex flex-column gap-2">
                              <InputTextarea
                                {...field}
                                rows={4}
                                onChange={(e) => field.onChange(e.target.value)}
                                className={classNames("w-full", { "p-invalid": fieldState.error })}
                                maxLength={FORM_LIMITS.projects.taskDetailsMax}
                                data-testid="project-task-details-input"
                              />
                              <CivicCharacterCount current={watchedTaskDetails.length} max={FORM_LIMITS.projects.taskDetailsMax} min={FORM_LIMITS.projects.taskDetailsMin} />
                            </div>
                          )}
                        />
                      </CivicField>
                      <div className="flex justify-content-end">
                        <CivicButton type="submit" icon="pi pi-plus" label={t("community_projects.task_create_action")} loading={isSubmittingTask} disabled={!canManageBoards} data-testid="project-task-submit-button" />
                      </div>
                    </form>
                  </CivicCard>
                  <div className="grid" data-testid="community-project-kanban-board">
                    {columns.map((column) => {
                      const tasks = selectedBoard.tasks.filter((task) => task.status === column.status);
                      return (
                        <div className="col-12 lg:col-4" key={column.status}>
                          <CivicCard title={column.title} className="h-full" data-testid={`community-project-column-${column.status.toLowerCase()}`}>
                            <div className="u-meta-row mb-3">
                              <span>{t("community_projects.column_count", { count: tasks.length })}</span>
                            </div>
                            {tasks.length === 0 ? (
                              <CivicEmptyState
                                icon="pi-list-check"
                                title={t("community_projects.column_empty_title", { status: column.title })}
                                description={t("community_projects.column_empty_desc", { status: column.title })}
                              />
                            ) : (
                              <div className="flex flex-column gap-3">
                                {tasks.map((task) => (
                                  <div key={task.id} className="u-surface-note" data-testid={`community-project-task-${task.id}`}>
                                    <div className="flex justify-content-between gap-3 align-items-start flex-wrap">
                                      <div className="min-w-0 flex-1">
                                        <div className="u-eyebrow mb-2">{task.assigneeUsername ?? t("community_projects.unassigned")}</div>
                                        <h3 className="text-base font-black text-main m-0">{task.title}</h3>
                                      </div>
                                      <span className="u-pill">{task.comments.length}</span>
                                    </div>
                                    <p className="text-sm text-secondary mt-3 mb-0 line-height-3">{task.details}</p>
                                    <div className="u-meta-row mt-3">
                                      <span>{task.dueDate ?? t("community_projects.no_due_date")}</span>
                                      <span>{statusLabels[task.status]}</span>
                                    </div>
                                    <div className="flex gap-2 flex-wrap mt-3">
                                      {getPrevStatus(task.status) && (
                                        <CivicButton
                                          type="button"
                                          size="small"
                                          variant="ghost"
                                          label={t("community_projects.move_back")}
                                          onClick={() => updateTaskStatus(task.id, task.title, task.details, getPrevStatus(task.status)!, task.assigneeUsername, task.dueDate)}
                                          loading={updatingTaskId === task.id}
                                          disabled={!canManageBoards}
                                          data-testid={`project-task-move-back-${task.id}`}
                                        />
                                      )}
                                      {getNextStatus(task.status) && (
                                        <CivicButton
                                          type="button"
                                          size="small"
                                          variant="secondary"
                                          label={t("community_projects.move_forward")}
                                          onClick={() => updateTaskStatus(task.id, task.title, task.details, getNextStatus(task.status)!, task.assigneeUsername, task.dueDate)}
                                          loading={updatingTaskId === task.id}
                                          disabled={!canManageBoards}
                                          data-testid={`project-task-move-forward-${task.id}`}
                                        />
                                      )}
                                    </div>
                                    <div className="flex flex-column gap-2 mt-3">
                                      {task.comments.map((comment) => (
                                        <div key={comment.id} className="u-surface-chip">
                                          <div className="u-meta-row mb-1">
                                            <span>{comment.authorUsername}</span>
                                            <span>{new Date(comment.createdAt).toLocaleDateString()}</span>
                                          </div>
                                          <p className="text-sm text-secondary m-0 line-height-3">{comment.content}</p>
                                        </div>
                                      ))}
                                    </div>
                                    <div className="flex flex-column gap-2 mt-3">
                                      <InputTextarea
                                        rows={2}
                                        value={commentDrafts[task.id] ?? ""}
                                        onChange={(e) => setCommentDrafts((current) => ({ ...current, [task.id]: e.target.value }))}
                                        className="w-full"
                                        maxLength={FORM_LIMITS.projects.commentMax}
                                        data-testid={`project-task-comment-input-${task.id}`}
                                        placeholder={t("community_projects.comment_placeholder")}
                                      />
                                      <div className="flex justify-content-between align-items-center gap-3 flex-wrap">
                                        <CivicCharacterCount
                                          current={(commentDrafts[task.id] ?? "").length}
                                          max={FORM_LIMITS.projects.commentMax}
                                          min={FORM_LIMITS.projects.commentMin}
                                        />
                                        <CivicButton
                                          type="button"
                                          size="small"
                                          variant="ghost"
                                          label={t("community_projects.comment_action")}
                                          loading={commentingTaskId === task.id}
                                          onClick={() => addComment(task.id)}
                                          data-testid={`project-task-comment-submit-${task.id}`}
                                        />
                                      </div>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            )}
                          </CivicCard>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ) : (
                <CivicCard>
                  <CivicEmptyState
                    icon="pi-list-check"
                    title={t("community_projects.empty_detail_title")}
                    description={t("community_projects.empty_detail_desc")}
                  />
                </CivicCard>
              )}
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
