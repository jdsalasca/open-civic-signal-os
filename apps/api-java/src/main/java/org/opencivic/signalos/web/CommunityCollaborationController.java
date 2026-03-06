package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.service.CommunityCollaborationService;
import org.opencivic.signalos.service.CommunityProjectBoardService;
import org.opencivic.signalos.service.CommunityProposalDeliberationService;
import org.opencivic.signalos.service.CommunityProposalService;
import org.opencivic.signalos.service.GovernanceLibraryService;
import org.opencivic.signalos.web.dto.ArchiveCommunityBlogPostRequest;
import org.opencivic.signalos.web.dto.CommunityBlogPostResponse;
import org.opencivic.signalos.web.dto.CommunityFeedItemResponse;
import org.opencivic.signalos.web.dto.CommunityHomeResponse;
import org.opencivic.signalos.web.dto.CommunityProjectBoardResponse;
import org.opencivic.signalos.web.dto.CommunityProposalDeliberationResponse;
import org.opencivic.signalos.web.dto.CommunityProposalResponse;
import org.opencivic.signalos.web.dto.CreateCommunityProjectBoardRequest;
import org.opencivic.signalos.web.dto.CreateCommunityProjectTaskCommentRequest;
import org.opencivic.signalos.web.dto.CreateCommunityProjectTaskRequest;
import org.opencivic.signalos.web.dto.CreateCommunityProposalDeliberationRequest;
import org.opencivic.signalos.web.dto.CommunityThreadMessageResponse;
import org.opencivic.signalos.web.dto.CommunityThreadResponse;
import org.opencivic.signalos.web.dto.CreateGovernanceDocumentRequest;
import org.opencivic.signalos.web.dto.CreateGovernanceDocumentVersionRequest;
import org.opencivic.signalos.web.dto.CreateCommunityBlogPostRequest;
import org.opencivic.signalos.web.dto.CreateCommunityProposalRequest;
import org.opencivic.signalos.web.dto.CreateCommunityThreadMessageRequest;
import org.opencivic.signalos.web.dto.CreateCommunityThreadRequest;
import org.opencivic.signalos.web.dto.GovernanceDocumentResponse;
import org.opencivic.signalos.web.dto.ModerateThreadMessageRequest;
import org.opencivic.signalos.web.dto.ModerateCommunityProposalEntryRequest;
import org.opencivic.signalos.web.dto.UpdateCommunityProjectTaskRequest;
import org.opencivic.signalos.web.dto.UpdateCommunityBlogPostRequest;
import org.opencivic.signalos.web.dto.UpdateCommunityProposalRequest;
import org.opencivic.signalos.web.dto.ApiPageResponse;
import org.opencivic.signalos.web.dto.ReactionStateResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.opencivic.signalos.service.CivicEngagementService;
import org.opencivic.signalos.web.dto.CivicCommentResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
public class CommunityCollaborationController {
    private final CommunityCollaborationService collaborationService;
    private final CommunityProjectBoardService projectBoardService;
    private final CommunityProposalService proposalService;
    private final CommunityProposalDeliberationService proposalDeliberationService;
    private final GovernanceLibraryService governanceLibraryService;
    private final CivicEngagementService engagementService;

    public CommunityCollaborationController(
        CommunityCollaborationService collaborationService,
        CommunityProjectBoardService projectBoardService,
        CommunityProposalService proposalService,
        CommunityProposalDeliberationService proposalDeliberationService,
        GovernanceLibraryService governanceLibraryService,
        CivicEngagementService engagementService
    ) {
        this.collaborationService = collaborationService;
        this.projectBoardService = projectBoardService;
        this.proposalService = proposalService;
        this.proposalDeliberationService = proposalDeliberationService;
        this.governanceLibraryService = governanceLibraryService;
        this.engagementService = engagementService;
    }

    @GetMapping("/blog/{id}/comments")
    public List<CivicCommentResponse> getBlogComments(@PathVariable UUID id) {
        return engagementService.getComments(id, "BLOG");
    }

    @GetMapping("/blog/comments/count")
    public Map<UUID, Long> getBlogCommentCounts(@RequestParam List<UUID> postIds) {
        return engagementService.getCommentCounts(postIds, "BLOG");
    }

    @PostMapping("/blog/{id}/comments")
    public CivicCommentResponse addBlogComment(@PathVariable UUID id, @RequestBody Map<String, String> body, Principal principal) {
        return engagementService.addComment(id, "BLOG", body.get("content"), principal.getName());
    }

    @PostMapping("/blog/{id}/react")
    public ReactionStateResponse reactToBlog(@PathVariable UUID id, @RequestBody Map<String, String> body, Principal principal) {
        return engagementService.react(id, "BLOG", body.get("type"), principal.getName());
    }

    @GetMapping("/threads")
    public ApiPageResponse<CommunityThreadResponse> getThreads(
        @RequestParam UUID communityId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false, defaultValue = "RELEVANCE") String sortBy,
        @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
        Principal principal
    ) {
        return ApiPageResponse.from(
            collaborationService.getThreads(communityId, status, sortBy, pageable, principal.getName())
        );
    }

    @PostMapping("/threads")
    public CommunityThreadResponse createThread(
        @Valid @RequestBody CreateCommunityThreadRequest request,
        Principal principal
    ) {
        return collaborationService.createThread(
            request.sourceCommunityId(),
            request.targetCommunityId(),
            request.relatedSignalId(),
            request.title(),
            principal.getName()
        );
    }

    @PostMapping("/threads/{threadId}/messages")
    public CommunityThreadMessageResponse addMessage(
        @PathVariable UUID threadId,
        @Valid @RequestBody CreateCommunityThreadMessageRequest request,
        Principal principal
    ) {
        return collaborationService.addMessage(
            threadId,
            request.sourceCommunityId(),
            request.content(),
            request.parentMessageId(),
            principal.getName()
        );
    }

    @PostMapping("/threads/{threadId}/messages/{messageId}/react")
    public CommunityThreadMessageResponse reactToMessage(
        @PathVariable UUID threadId,
        @PathVariable UUID messageId,
        @RequestBody java.util.Map<String, String> body,
        Principal principal
    ) {
        return collaborationService.reactToMessage(
            threadId,
            messageId,
            body.get("type"),
            principal.getName()
        );
    }

    @PatchMapping("/threads/{threadId}/messages/{messageId}/moderate")
    public CommunityThreadMessageResponse moderateMessage(
        @PathVariable UUID threadId,
        @PathVariable UUID messageId,
        @Valid @RequestBody ModerateThreadMessageRequest request,
        Principal principal
    ) {
        return collaborationService.moderateMessage(
            threadId,
            messageId,
            request.hidden(),
            request.reason(),
            principal.getName()
        );
    }

    @GetMapping("/blog")
    public List<CommunityBlogPostResponse> getBlog(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return collaborationService.getBlogTimeline(communityId, principal.getName());
    }

    @GetMapping("/blog/archive")
    public List<CommunityBlogPostResponse> getBlogArchive(
        @RequestParam UUID communityId,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) LocalDate dateFrom,
        @RequestParam(required = false) LocalDate dateTo,
        Principal principal
    ) {
        return collaborationService.getBlogArchive(
            communityId,
            query,
            dateFrom,
            dateTo,
            principal.getName()
        );
    }

    @PostMapping("/blog")
    public CommunityBlogPostResponse createBlog(
        @Valid @RequestBody CreateCommunityBlogPostRequest request,
        Principal principal
    ) {
        return collaborationService.createBlogPost(
            request.communityId(),
            request.title(),
            request.content(),
            request.statusTag(),
            request.pinned(),
            principal.getName()
        );
    }

    @PutMapping("/blog/{postId}")
    public CommunityBlogPostResponse updateBlog(
        @PathVariable UUID postId,
        @Valid @RequestBody UpdateCommunityBlogPostRequest request,
        Principal principal
    ) {
        return collaborationService.updateBlogPost(
            postId,
            request.title(),
            request.content(),
            request.statusTag(),
            request.pinned(),
            principal.getName()
        );
    }

    @PatchMapping("/blog/{postId}/archive")
    public CommunityBlogPostResponse archiveBlog(
        @PathVariable UUID postId,
        @Valid @RequestBody ArchiveCommunityBlogPostRequest request,
        Principal principal
    ) {
        return collaborationService.archiveBlogPost(
            postId,
            request.archived(),
            principal.getName()
        );
    }

    @GetMapping("/feed")
    public List<CommunityFeedItemResponse> getFeed(
        @RequestParam UUID communityId,
        @RequestParam(defaultValue = "7") int days,
        Principal principal
    ) {
        return collaborationService.getCommunityFeed(communityId, days, principal.getName());
    }

    @GetMapping("/home")
    public CommunityHomeResponse getCommunityHome(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return collaborationService.getCommunityHome(communityId, principal.getName());
    }

    @GetMapping("/proposals")
    public List<CommunityProposalResponse> getProposals(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return proposalService.getProposals(communityId, principal.getName());
    }

    @GetMapping("/proposals/{proposalId}")
    public CommunityProposalResponse getProposal(
        @PathVariable UUID proposalId,
        Principal principal
    ) {
        return proposalService.getProposal(proposalId, principal.getName());
    }

    @PostMapping("/proposals")
    public CommunityProposalResponse createProposal(
        @Valid @RequestBody CreateCommunityProposalRequest request,
        Principal principal
    ) {
        return proposalService.createProposal(request, principal.getName());
    }

    @PutMapping("/proposals/{proposalId}")
    public CommunityProposalResponse updateProposal(
        @PathVariable UUID proposalId,
        @Valid @RequestBody UpdateCommunityProposalRequest request,
        Principal principal
    ) {
        return proposalService.updateProposal(proposalId, request, principal.getName());
    }

    @GetMapping("/projects")
    public List<CommunityProjectBoardResponse> getProjects(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return projectBoardService.getBoards(communityId, principal.getName());
    }

    @GetMapping("/projects/{projectId}")
    public CommunityProjectBoardResponse getProject(
        @PathVariable UUID projectId,
        Principal principal
    ) {
        return projectBoardService.getBoard(projectId, principal.getName());
    }

    @PostMapping("/projects")
    public CommunityProjectBoardResponse createProject(
        @Valid @RequestBody CreateCommunityProjectBoardRequest request,
        Principal principal
    ) {
        return projectBoardService.createBoard(request, principal.getName());
    }

    @PostMapping("/projects/{projectId}/tasks")
    public CommunityProjectBoardResponse createProjectTask(
        @PathVariable UUID projectId,
        @Valid @RequestBody CreateCommunityProjectTaskRequest request,
        Principal principal
    ) {
        return projectBoardService.createTask(projectId, request, principal.getName());
    }

    @PatchMapping("/projects/{projectId}/tasks/{taskId}")
    public CommunityProjectBoardResponse updateProjectTask(
        @PathVariable UUID projectId,
        @PathVariable UUID taskId,
        @Valid @RequestBody UpdateCommunityProjectTaskRequest request,
        Principal principal
    ) {
        return projectBoardService.updateTask(projectId, taskId, request, principal.getName());
    }

    @PostMapping("/projects/{projectId}/tasks/{taskId}/comments")
    public CommunityProjectBoardResponse addProjectTaskComment(
        @PathVariable UUID projectId,
        @PathVariable UUID taskId,
        @Valid @RequestBody CreateCommunityProjectTaskCommentRequest request,
        Principal principal
    ) {
        return projectBoardService.addTaskComment(projectId, taskId, request, principal.getName());
    }

    @GetMapping("/governance")
    public List<GovernanceDocumentResponse> getGovernanceDocuments(
        @RequestParam UUID communityId,
        @RequestParam(required = false) String documentType,
        @RequestParam(required = false) String visibility,
        @RequestParam(required = false) String query,
        Principal principal
    ) {
        return governanceLibraryService.getDocuments(communityId, documentType, visibility, query, principal.getName());
    }

    @GetMapping("/governance/{documentId}")
    public GovernanceDocumentResponse getGovernanceDocument(
        @PathVariable UUID documentId,
        Principal principal
    ) {
        return governanceLibraryService.getDocument(documentId, principal.getName());
    }

    @PostMapping("/governance")
    public GovernanceDocumentResponse createGovernanceDocument(
        @Valid @RequestBody CreateGovernanceDocumentRequest request,
        Principal principal
    ) {
        return governanceLibraryService.createDocument(request, principal.getName());
    }

    @PostMapping("/governance/{documentId}/versions")
    public GovernanceDocumentResponse createGovernanceDocumentVersion(
        @PathVariable UUID documentId,
        @Valid @RequestBody CreateGovernanceDocumentVersionRequest request,
        Principal principal
    ) {
        return governanceLibraryService.addVersion(documentId, request, principal.getName());
    }

    @GetMapping("/proposals/{proposalId}/deliberation")
    public CommunityProposalDeliberationResponse getProposalDeliberation(
        @PathVariable UUID proposalId,
        Principal principal
    ) {
        return proposalDeliberationService.getDeliberation(proposalId, principal.getName());
    }

    @PostMapping("/proposals/{proposalId}/deliberation")
    public CommunityProposalDeliberationResponse createProposalDeliberationEntry(
        @PathVariable UUID proposalId,
        @Valid @RequestBody CreateCommunityProposalDeliberationRequest request,
        Principal principal
    ) {
        return proposalDeliberationService.createEntry(proposalId, request, principal.getName());
    }

    @PatchMapping("/proposals/{proposalId}/deliberation/{entryId}/moderate")
    public CommunityProposalDeliberationResponse moderateProposalDeliberationEntry(
        @PathVariable UUID proposalId,
        @PathVariable UUID entryId,
        @Valid @RequestBody ModerateCommunityProposalEntryRequest request,
        Principal principal
    ) {
        return proposalDeliberationService.moderateEntry(proposalId, entryId, request, principal.getName());
    }
}
