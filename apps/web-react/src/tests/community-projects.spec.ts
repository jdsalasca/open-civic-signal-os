import { expect, test } from "@playwright/test";
import type { CommunityProjectBoard, CommunityProjectTask } from "../types";

const communityId = "11111111-1111-1111-1111-111111111111";
const proposalId = "22222222-2222-2222-2222-222222222222";
const boardId = "33333333-3333-3333-3333-333333333333";
const taskId = "44444444-4444-4444-4444-444444444444";

test.describe("Community project boards", () => {
  test("creates a board, adds a task, moves it, and leaves a task note", async ({ page }) => {
    let createBoardPayload: Record<string, unknown> | null = null;
    let createTaskPayload: Record<string, unknown> | null = null;
    let updateTaskPayload: Record<string, unknown> | null = null;
    let addCommentPayload: Record<string, unknown> | null = null;

    let boards: CommunityProjectBoard[] = [
      {
        id: boardId,
        communityId,
        linkedProposalId: proposalId,
        linkedProposalTitle: "Safer school crossing",
        ownerId: "55555555-5555-5555-5555-555555555555",
        ownerUsername: "coordinator",
        title: "School crossing delivery board",
        summary: "Coordinate paint, signage, volunteer presence, and final inspection for the safer crossing plan.",
        dueDate: "2026-03-28",
        taskCounts: {
          todo: 1,
          inProgress: 0,
          done: 0,
        },
        tasks: [
          {
            id: taskId,
            projectBoardId: boardId,
            title: "Confirm school committee palette",
            details: "Close the final paint palette and volunteer roster for the first execution day.",
            status: "TODO",
            assigneeId: "66666666-6666-6666-6666-666666666666",
            assigneeUsername: "project_member",
            dueDate: "2026-03-20",
            sortOrder: 0,
            comments: [],
            createdAt: "2026-03-06T09:00:00",
            updatedAt: "2026-03-06T09:00:00",
          },
        ],
        createdAt: "2026-03-06T08:00:00",
        updatedAt: "2026-03-06T09:00:00",
      },
    ];

    await page.addInitScript(() => {
      window.localStorage.setItem(
        "auth-storage",
        JSON.stringify({
          state: {
            accessToken: "test-token",
            userName: "coordinator",
            activeRole: "CITIZEN",
            rawRoles: ["CITIZEN"],
            isLoggedIn: true,
            isHydrated: true,
          },
          version: 0,
        })
      );
      window.localStorage.setItem(
        "community-storage",
        JSON.stringify({
          state: {
            activeCommunityId: communityId,
            membershipsLoadedAt: Date.now(),
            memberships: [
              {
                userId: "55555555-5555-5555-5555-555555555555",
                communityId,
                communityName: "Los Rosales",
                communitySlug: "los-rosales",
                parentCommunityId: null,
                breadcrumb: [{ id: communityId, name: "Los Rosales", slug: "los-rosales" }],
                role: "COORDINATOR",
                createdBy: "55555555-5555-5555-5555-555555555555",
                createdAt: "2026-03-06T08:00:00",
              },
            ],
            threadListStateByCommunity: {},
          },
          version: 0,
        })
      );
    });

    await page.route("**/api/communities/my", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            userId: "55555555-5555-5555-5555-555555555555",
            communityId,
            communityName: "Los Rosales",
            communitySlug: "los-rosales",
            parentCommunityId: null,
            breadcrumb: [{ id: communityId, name: "Los Rosales", slug: "los-rosales" }],
            role: "COORDINATOR",
            createdBy: "55555555-5555-5555-5555-555555555555",
            createdAt: "2026-03-06T08:00:00",
          },
        ]),
      });
    });

    await page.route("**/api/community/proposals?communityId=*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            id: proposalId,
            communityId,
            authorId: "55555555-5555-5555-5555-555555555555",
            authorUsername: "coordinator",
            relatedSignalId: null,
            relatedSignalTitle: null,
            title: "Safer school crossing",
            templateKey: "STANDARD_COMMUNITY_PROPOSAL",
            status: "PROPOSED",
            problemStatement: "Families lack a safer crossing near the main gate.",
            proposedSolution: "Paint a raised crossing and improve signage.",
            estimatedCost: "COP 18M",
            beneficiariesSummary: "Students, families, and neighbors.",
            supportingLinks: ["https://example.com/proposals/crossing"],
            createdAt: "2026-03-05T10:00:00",
            updatedAt: "2026-03-05T10:00:00",
          },
        ]),
      });
    });

    await page.route("**/api/community/projects?communityId=*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(boards),
      });
    });

    await page.route("**/api/community/projects", async (route) => {
      createBoardPayload = route.request().postDataJSON() as Record<string, unknown>;
      const newBoard = {
        id: "77777777-7777-7777-7777-777777777777",
        communityId,
        linkedProposalId: proposalId,
        linkedProposalTitle: "Safer school crossing",
        ownerId: "55555555-5555-5555-5555-555555555555",
        ownerUsername: "coordinator",
        title: String(createBoardPayload.title),
        summary: String(createBoardPayload.summary),
        dueDate: String(createBoardPayload.dueDate),
        taskCounts: { todo: 0, inProgress: 0, done: 0 },
        tasks: [],
        createdAt: "2026-03-06T10:00:00",
        updatedAt: "2026-03-06T10:00:00",
      };
      boards = [newBoard, ...boards];
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(newBoard),
      });
    });

    await page.route("**/api/community/projects/*/tasks", async (route) => {
      createTaskPayload = route.request().postDataJSON() as Record<string, unknown>;
      const targetBoard = boards[0];
      const createdTask: CommunityProjectTask = {
        id: "88888888-8888-8888-8888-888888888888",
        projectBoardId: targetBoard.id,
        title: String(createTaskPayload.title),
        details: String(createTaskPayload.details),
        status: "TODO",
        assigneeId: "66666666-6666-6666-6666-666666666666",
        assigneeUsername: "project_member",
        dueDate: String(createTaskPayload.dueDate),
        sortOrder: targetBoard.tasks.length,
        comments: [],
        createdAt: "2026-03-06T10:10:00",
        updatedAt: "2026-03-06T10:10:00",
      };
      const updatedBoard = {
        ...targetBoard,
        taskCounts: { ...targetBoard.taskCounts, todo: targetBoard.taskCounts.todo + 1 },
        tasks: [...targetBoard.tasks, createdTask],
        updatedAt: "2026-03-06T10:10:00",
      };
      boards = [updatedBoard, ...boards.slice(1)];
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(updatedBoard),
      });
    });

    await page.route("**/api/community/projects/*/tasks/*", async (route) => {
      updateTaskPayload = route.request().postDataJSON() as Record<string, unknown>;
      const updatedBoard: CommunityProjectBoard = {
        ...boards[0],
        taskCounts: { todo: 0, inProgress: 1, done: 0 },
        tasks: boards[0].tasks.map<CommunityProjectTask>((task) =>
          task.id === taskId
            ? {
                ...task,
                status: "IN_PROGRESS",
                details: String(updateTaskPayload?.details),
                updatedAt: "2026-03-06T10:20:00",
              }
            : task
        ),
        updatedAt: "2026-03-06T10:20:00",
      };
      boards = [updatedBoard, ...boards.slice(1)];
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(updatedBoard),
      });
    });

    await page.route("**/api/community/projects/*/tasks/*/comments", async (route) => {
      addCommentPayload = route.request().postDataJSON() as Record<string, unknown>;
      const updatedBoard: CommunityProjectBoard = {
        ...boards[0],
        tasks: boards[0].tasks.map<CommunityProjectTask>((task) =>
          task.id === taskId
            ? {
                ...task,
                comments: [
                  ...task.comments,
                  {
                    id: "99999999-9999-9999-9999-999999999999",
                    taskId,
                    authorId: "55555555-5555-5555-5555-555555555555",
                    authorUsername: "coordinator",
                    content: String(addCommentPayload?.content),
                    createdAt: "2026-03-06T10:30:00",
                  },
                ],
                updatedAt: "2026-03-06T10:30:00",
              }
            : task
        ),
        updatedAt: "2026-03-06T10:30:00",
      };
      boards = [updatedBoard, ...boards.slice(1)];
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(updatedBoard),
      });
    });

    await page.goto("http://127.0.0.1:5173/communities/projects");

    await expect(page.getByTestId("community-project-board-detail-card")).toContainText("School crossing delivery board");
    await expect(page.getByTestId("community-project-column-todo")).toContainText("Confirm school committee palette");

    await page.getByTestId("project-board-linked-proposal-select").click();
    await page.getByText("Safer school crossing", { exact: true }).click();
    await page.getByTestId("project-board-title-input").fill("Crossing volunteers delivery board");
    await page
      .getByTestId("project-board-summary-input")
      .fill("Coordinate volunteer shifts, paint prep, procurement, and final walkthrough for the crossing rollout.");
    await page.getByTestId("project-board-due-date-input").fill("2026-04-02");
    await page.getByTestId("project-board-submit-button").click();

    await expect.poll(() => createBoardPayload).not.toBeNull();
    expect(createBoardPayload).toMatchObject({
      communityId,
      linkedProposalId: proposalId,
      title: "Crossing volunteers delivery board",
    });
    await expect(page.getByTestId("community-project-board-detail-card")).toContainText("Crossing volunteers delivery board");

    await page.getByTestId("project-task-title-input").fill("Schedule Saturday volunteer shift");
    await page
      .getByTestId("project-task-details-input")
      .fill("Lock the volunteer schedule, assign setup leads, and confirm arrival windows for Saturday.");
    await page.getByTestId("project-task-assignee-input").fill("project_member");
    await page.getByTestId("project-task-due-date-input").fill("2026-03-29");
    await page.getByTestId("project-task-submit-button").click();

    await expect.poll(() => createTaskPayload).not.toBeNull();
    expect(createTaskPayload).toMatchObject({
      title: "Schedule Saturday volunteer shift",
      assigneeUsername: "project_member",
    });
    await expect(page.getByTestId("community-project-column-todo")).toContainText("Schedule Saturday volunteer shift");

    await page.getByTestId(`project-task-move-forward-${taskId}`).click();
    await expect.poll(() => updateTaskPayload).not.toBeNull();
    expect(updateTaskPayload).toMatchObject({
      status: "IN_PROGRESS",
      assigneeUsername: "project_member",
    });
    await expect(page.getByTestId("community-project-column-in_progress")).toContainText("Confirm school committee palette");

    await page
      .getByTestId(`project-task-comment-input-${taskId}`)
      .fill("School committee approved the warmer palette after the afternoon walkthrough.");
    await page.getByTestId(`project-task-comment-submit-${taskId}`).click();

    await expect.poll(() => addCommentPayload).not.toBeNull();
    expect(addCommentPayload).toMatchObject({
      content: "School committee approved the warmer palette after the afternoon walkthrough.",
    });
    await expect(page.getByTestId(`community-project-task-${taskId}`)).toContainText(
      "School committee approved the warmer palette after the afternoon walkthrough."
    );
  });
});
