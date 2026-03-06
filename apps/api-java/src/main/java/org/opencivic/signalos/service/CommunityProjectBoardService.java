package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityProjectBoard;
import org.opencivic.signalos.domain.CommunityProjectStatus;
import org.opencivic.signalos.domain.CommunityProjectTask;
import org.opencivic.signalos.domain.CommunityProjectTaskComment;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProjectBoardRepository;
import org.opencivic.signalos.repository.CommunityProjectTaskCommentRepository;
import org.opencivic.signalos.repository.CommunityProjectTaskRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityProjectBoardResponse;
import org.opencivic.signalos.web.dto.CommunityProjectTaskCommentResponse;
import org.opencivic.signalos.web.dto.CommunityProjectTaskCountsResponse;
import org.opencivic.signalos.web.dto.CommunityProjectTaskResponse;
import org.opencivic.signalos.web.dto.CreateCommunityProjectBoardRequest;
import org.opencivic.signalos.web.dto.CreateCommunityProjectTaskCommentRequest;
import org.opencivic.signalos.web.dto.CreateCommunityProjectTaskRequest;
import org.opencivic.signalos.web.dto.UpdateCommunityProjectTaskRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityProjectBoardService {
    private final CommunityAccessService accessService;
    private final CommunityProjectBoardRepository boardRepository;
    private final CommunityProjectTaskRepository taskRepository;
    private final CommunityProjectTaskCommentRepository commentRepository;
    private final CommunityProposalRepository proposalRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public CommunityProjectBoardService(
        CommunityAccessService accessService,
        CommunityProjectBoardRepository boardRepository,
        CommunityProjectTaskRepository taskRepository,
        CommunityProjectTaskCommentRepository commentRepository,
        CommunityProposalRepository proposalRepository,
        CommunityMembershipRepository membershipRepository,
        UserRepository userRepository
    ) {
        this.accessService = accessService;
        this.boardRepository = boardRepository;
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.proposalRepository = proposalRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public List<CommunityProjectBoardResponse> getBoards(UUID communityId, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireMembership(user.getId(), communityId);
        return boardRepository.findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(communityId).stream()
            .map(this::toBoardResponse)
            .toList();
    }

    public CommunityProjectBoardResponse getBoard(UUID projectId, String username) {
        User user = accessService.getCurrentUser(username);
        CommunityProjectBoard board = getBoardEntity(projectId);
        accessService.requireMembership(user.getId(), board.getCommunityId());
        return toBoardResponse(board);
    }

    @Transactional
    public CommunityProjectBoardResponse createBoard(CreateCommunityProjectBoardRequest request, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireScope(user.getId(), request.communityId(), CommunityPermissionScope.MANAGE_PROJECT_BOARDS);
        CommunityProposal linkedProposal = validateProposal(request.communityId(), request.linkedProposalId());

        CommunityProjectBoard board = new CommunityProjectBoard();
        board.setCommunityId(request.communityId());
        board.setLinkedProposalId(linkedProposal == null ? null : linkedProposal.getId());
        board.setTitle(request.title().trim());
        board.setSummary(request.summary().trim());
        board.setOwnerId(user.getId());
        board.setDueDate(request.dueDate());
        board.setCreatedAt(LocalDateTime.now());
        board.setUpdatedAt(LocalDateTime.now());
        return toBoardResponse(boardRepository.save(board));
    }

    @Transactional
    public CommunityProjectBoardResponse createTask(UUID projectId, CreateCommunityProjectTaskRequest request, String username) {
        User user = accessService.getCurrentUser(username);
        CommunityProjectBoard board = getBoardEntity(projectId);
        accessService.requireScope(user.getId(), board.getCommunityId(), CommunityPermissionScope.MANAGE_PROJECT_BOARDS);

        CommunityProjectTask task = new CommunityProjectTask();
        task.setProjectBoardId(board.getId());
        task.setTitle(request.title().trim());
        task.setDetails(request.details().trim());
        task.setStatus(CommunityProjectStatus.TODO);
        task.setAssigneeId(resolveAssigneeId(board.getCommunityId(), request.assigneeUsername()));
        task.setDueDate(request.dueDate());
        task.setSortOrder(taskRepository.findByProjectBoardIdOrderBySortOrderAscCreatedAtAsc(board.getId()).size());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);
        return toBoardResponse(board);
    }

    @Transactional
    public CommunityProjectBoardResponse updateTask(
        UUID projectId,
        UUID taskId,
        UpdateCommunityProjectTaskRequest request,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        CommunityProjectBoard board = getBoardEntity(projectId);
        accessService.requireScope(user.getId(), board.getCommunityId(), CommunityPermissionScope.MANAGE_PROJECT_BOARDS);
        CommunityProjectTask task = getTaskEntity(taskId);
        if (!task.getProjectBoardId().equals(board.getId())) {
            throw new IllegalArgumentException("Task does not belong to the selected project board.");
        }

        task.setTitle(request.title().trim());
        task.setDetails(request.details().trim());
        task.setStatus(CommunityProjectStatus.valueOf(request.status().trim().toUpperCase()));
        task.setAssigneeId(resolveAssigneeId(board.getCommunityId(), request.assigneeUsername()));
        task.setDueDate(request.dueDate());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);
        return toBoardResponse(board);
    }

    @Transactional
    public CommunityProjectBoardResponse addTaskComment(
        UUID projectId,
        UUID taskId,
        CreateCommunityProjectTaskCommentRequest request,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        CommunityProjectBoard board = getBoardEntity(projectId);
        accessService.requireScope(user.getId(), board.getCommunityId(), CommunityPermissionScope.ADD_THREAD_MESSAGE);
        CommunityProjectTask task = getTaskEntity(taskId);
        if (!task.getProjectBoardId().equals(board.getId())) {
            throw new IllegalArgumentException("Task does not belong to the selected project board.");
        }

        CommunityProjectTaskComment comment = new CommunityProjectTaskComment();
        comment.setTaskId(taskId);
        comment.setAuthorId(user.getId());
        comment.setContent(request.content().trim());
        comment.setCreatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);
        return toBoardResponse(board);
    }

    private CommunityProjectBoardResponse toBoardResponse(CommunityProjectBoard board) {
        CommunityProposal linkedProposal = board.getLinkedProposalId() == null
            ? null
            : proposalRepository.findById(board.getLinkedProposalId()).orElse(null);
        User owner = userRepository.findById(board.getOwnerId()).orElse(null);

        List<CommunityProjectTask> tasks = taskRepository.findByProjectBoardIdOrderBySortOrderAscCreatedAtAsc(board.getId());
        Map<UUID, List<CommunityProjectTaskComment>> commentsByTask = tasks.stream()
            .collect(Collectors.toMap(
                CommunityProjectTask::getId,
                task -> commentRepository.findByTaskIdOrderByCreatedAtAsc(task.getId())
            ));

        int todo = 0;
        int inProgress = 0;
        int done = 0;
        for (CommunityProjectTask task : tasks) {
            switch (task.getStatus()) {
                case TODO -> todo++;
                case IN_PROGRESS -> inProgress++;
                case DONE -> done++;
            }
        }

        return new CommunityProjectBoardResponse(
            board.getId(),
            board.getCommunityId(),
            board.getLinkedProposalId(),
            linkedProposal == null ? null : linkedProposal.getTitle(),
            board.getOwnerId(),
            owner == null ? "deleted_user" : owner.getUsername(),
            board.getTitle(),
            board.getSummary(),
            board.getDueDate(),
            new CommunityProjectTaskCountsResponse(todo, inProgress, done),
            tasks.stream().map(task -> toTaskResponse(task, commentsByTask.getOrDefault(task.getId(), List.of()))).toList(),
            board.getCreatedAt(),
            board.getUpdatedAt()
        );
    }

    private CommunityProjectTaskResponse toTaskResponse(
        CommunityProjectTask task,
        List<CommunityProjectTaskComment> comments
    ) {
        User assignee = task.getAssigneeId() == null ? null : userRepository.findById(task.getAssigneeId()).orElse(null);
        return new CommunityProjectTaskResponse(
            task.getId(),
            task.getProjectBoardId(),
            task.getTitle(),
            task.getDetails(),
            task.getStatus().name(),
            task.getAssigneeId(),
            assignee == null ? null : assignee.getUsername(),
            task.getDueDate(),
            task.getSortOrder(),
            comments.stream().map(this::toCommentResponse).toList(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }

    private CommunityProjectTaskCommentResponse toCommentResponse(CommunityProjectTaskComment comment) {
        User author = userRepository.findById(comment.getAuthorId()).orElse(null);
        return new CommunityProjectTaskCommentResponse(
            comment.getId(),
            comment.getTaskId(),
            comment.getAuthorId(),
            author == null ? "deleted_user" : author.getUsername(),
            comment.getContent(),
            comment.getCreatedAt()
        );
    }

    private UUID resolveAssigneeId(UUID communityId, String assigneeUsername) {
        if (assigneeUsername == null || assigneeUsername.trim().isBlank()) {
            return null;
        }
        User assignee = userRepository.findByUsername(assigneeUsername.trim())
            .orElseThrow(() -> new IllegalArgumentException("Assignee username was not found."));
        CommunityMembership membership = membershipRepository.findByUserIdAndCommunityId(assignee.getId(), communityId)
            .orElseThrow(() -> new IllegalArgumentException("Assignee must belong to the same community."));
        return membership.getUserId();
    }

    private CommunityProposal validateProposal(UUID communityId, UUID proposalId) {
        if (proposalId == null) {
            return null;
        }
        CommunityProposal proposal = proposalRepository.findById(proposalId)
            .orElseThrow(() -> new ResourceNotFoundException("Community proposal not found: " + proposalId));
        if (!proposal.getCommunityId().equals(communityId)) {
            throw new IllegalArgumentException("Linked proposal must belong to the same community.");
        }
        return proposal;
    }

    private CommunityProjectBoard getBoardEntity(UUID projectId) {
        return boardRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Community project board not found: " + projectId));
    }

    private CommunityProjectTask getTaskEntity(UUID taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Community project task not found: " + taskId));
    }
}
