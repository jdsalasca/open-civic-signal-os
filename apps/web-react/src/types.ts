export type ScoreBreakdown = {
  urgency: number;
  impact: number;
  affectedPeople: number;
  communityVotes: number;
};

export type ExplainabilityFactor = {
  key: "urgency" | "impact" | "affectedPeople" | "communityVotes";
  contribution: number;
};

export type ExplainabilitySummary = {
  version: string;
  topFactors: ExplainabilityFactor[];
  summary: string;
};

export type CivicComment = {
  id: string;
  parentId: string;
  parentType: 'SIGNAL' | 'BLOG';
  authorId: string;
  authorUsername: string;
  authorRole: string;
  content: string;
  createdAt: string;
};

export type Signal = {
  id: string;
  title: string;
  description: string;
  imageUrl?: string;
  assignedToUsername?: string | null;
  locationLabel?: string | null;
  evidenceUrls?: string[];
  category: string;
  status: string;
  priorityScore: number;
  scoreBreakdown: ScoreBreakdown;
  communityVotes: number;
  reactions: Record<string, number>;
  viewerReaction?: string;
  explainabilitySummary: ExplainabilitySummary;
  latitude?: number | null;
  longitude?: number | null;
};

export type Notification = {
  id: string;
  channel: string;
  message: string;
  recipientGroup: string;
  sentAt: string;
};

export type SignalMeta = {
  totalSignals: number;
  unresolvedSignals: number;
  lastUpdatedAt: string | null;
};

export type UserRole = "SUPER_ADMIN" | "PUBLIC_SERVANT" | "CITIZEN" | "GUEST";

export type ProfileVisibility = "PUBLIC" | "COMMUNITY" | "ADMINS";
export type InterfaceMode = "SIMPLE" | "ADVANCED";

export type UserProfile = {
  username: string;
  displayName: string;
  email?: string | null;
  verified: boolean;
  civicRole?: string | null;
  bio?: string | null;
  affiliations: string[];
  profileVisibility: ProfileVisibility;
  affiliationVisibility: ProfileVisibility;
  activityVisibility: ProfileVisibility;
  interfaceMode: InterfaceMode;
  avatarPreset: string;
  achievements: ProfileAchievement[];
  viewerScope: ProfileVisibility;
};

export type ProfileAchievement = {
  key: string;
  earned: boolean;
  currentProgress: number;
  targetProgress: number;
};

export type AuthInfo = {
  user: string;
  role: UserRole;
  token: string;
};

export type Community = {
  id: string;
  name: string;
  slug: string;
  description?: string;
  parentCommunityId?: string | null;
  createdAt?: string;
};

export type CommunityBreadcrumbItem = {
  id: string;
  name: string;
  slug: string;
};

export type CommunityTreeNode = {
  id: string;
  name: string;
  slug: string;
  description?: string;
  parentCommunityId?: string | null;
  children: CommunityTreeNode[];
};

export type CommunityMembership = {
  userId: string;
  communityId: string;
  communityName: string;
  communitySlug: string;
  parentCommunityId?: string | null;
  breadcrumb: CommunityBreadcrumbItem[];
  role: "MEMBER" | "MODERATOR" | "COORDINATOR" | "PUBLIC_SERVANT_LIAISON";
  createdBy: string;
  createdAt: string;
};

export type CommunityPermissionScope =
  | "CAST_PROPOSAL_VOTE"
  | "CREATE_PROPOSAL"
  | "MANAGE_MODERATION_QUEUE"
  | "MANAGE_PRIVACY_SETTINGS"
  | "MANAGE_DECISION_LEDGER"
  | "MANAGE_GOVERNANCE_LIBRARY"
  | "MANAGE_PROJECT_BOARDS"
  | "CREATE_THREAD"
  | "ADD_THREAD_MESSAGE"
  | "MODERATE_THREAD_MESSAGE"
  | "CREATE_OFFICIAL_UPDATE"
  | "UPDATE_OFFICIAL_UPDATE"
  | "MANAGE_MEMBERSHIPS"
  | "MANAGE_PERMISSION_POLICIES"
  | "VIEW_SENSITIVE_DATA";

export type CommunityPermissionPolicy = {
  communityId: string;
  scope: CommunityPermissionScope;
  allowedRoles: CommunityMembership["role"][];
  updatedBy?: string | null;
  updatedAt?: string | null;
};

export type SensitiveDataAccessType =
  | "PROFILE_ADMIN_VIEW"
  | "SIGNAL_EXPORT";

export type SensitiveDataAccessLog = {
  id: string;
  accessType: SensitiveDataAccessType;
  actorUserId: string;
  actorUsername: string;
  targetUserId?: string | null;
  targetUsername?: string | null;
  communityId?: string | null;
  communityName?: string | null;
  note: string;
  createdAt: string;
};

export type CommunityOpenDataPolicy =
  | "DISABLED"
  | "AGGREGATED_PUBLIC"
  | "AGGREGATED_AND_DECISIONS";

export type CommunityPrivacyPolicy = {
  communityId: string;
  communityName: string;
  openDataPolicy: CommunityOpenDataPolicy;
  updatedByUsername?: string | null;
  updatedAt?: string | null;
  recentAccessLogs: SensitiveDataAccessLog[];
};

export type CommunityThreadMessage = {
  id: string;
  threadId: string;
  authorId: string;
  sourceCommunityId: string;
  parentMessageId?: string;
  depth: number;
  directReplyCount: number;
  content: string;
  hidden: boolean;
  moderationReason?: string;
  createdAt: string;
  reactions: Record<string, number>;
  viewerReaction?: string;
};

export type CommunityThread = {
  id: string;
  sourceCommunityId: string;
  targetCommunityId: string;
  relatedSignalId?: string;
  title: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  totalMessages: number;
  totalReplies: number;
  totalReactions: number;
  relevanceScore: number;
  relevanceSummary: string;
  messages: CommunityThreadMessage[];
};

export type CommunityBlogPost = {
  id: string;
  communityId: string;
  authorId: string;
  authorUsername: string;
  authorRole: string;
  official: boolean;
  pinned: boolean;
  title: string;
  content: string;
  statusTag: string;
  archivedBy?: string | null;
  archivedAt?: string | null;
  publishedAt: string;
  updatedAt: string;
  reactions: Record<string, number>;
  viewerReaction?: string;
};

export type SignalStatusEntry = {
  id: string;
  signalId: string;
  eventType: "CREATED" | "STATUS_CHANGED" | "ASSIGNED";
  statusFrom: string;
  statusTo: string;
  changedBy: string;
  assignedToUsername?: string | null;
  reason: string;
  createdAt: string;
};

export type CommunityFeedItem = {
  type: "signal" | "blog" | "thread-update";
  id: string;
  communityId: string;
  title: string;
  summary: string;
  happenedAt: string;
  freshness: string;
};

export type CommunityProposal = {
  id: string;
  communityId: string;
  authorId: string;
  authorUsername: string;
  relatedSignalId?: string | null;
  relatedSignalTitle?: string | null;
  title: string;
  templateKey: string;
  status: string;
  problemStatement: string;
  proposedSolution: string;
  estimatedCost: string;
  beneficiariesSummary: string;
  supportingLinks: string[];
  voteMode: CommunityProposalVoteMode;
  resultVisibility: CommunityProposalVoteVisibility;
  eligibilityRule: CommunityProposalVoteEligibility;
  votingOpensAt?: string | null;
  votingClosesAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CommunityProposalVoteMode = "YES_NO" | "SCORE_1_5";
export type CommunityProposalVoteVisibility = "COMMUNITY" | "AFTER_VOTE";
export type CommunityProposalVoteEligibility = "ALL_MEMBERS" | "VERIFIED_MEMBERS";
export type CommunityProposalVoteChoice = "FOR" | "AGAINST";

export type CommunityProposalDeliberationType = "PRO" | "CON" | "QUESTION" | "EVIDENCE";

export type CommunityProposalDeliberationEntry = {
  id: string;
  proposalId: string;
  authorId: string;
  authorUsername: string;
  entryType: CommunityProposalDeliberationType;
  content: string;
  supportingLink?: string | null;
  hidden: boolean;
  moderationReason?: string | null;
  hiddenByUsername?: string | null;
  hiddenAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CommunityProposalDeliberationCounts = {
  pros: number;
  cons: number;
  questions: number;
  evidence: number;
  visibleEntries: number;
  hiddenEntries: number;
};

export type CommunityProposalDeliberation = {
  proposalId: string;
  counts: CommunityProposalDeliberationCounts;
  entries: CommunityProposalDeliberationEntry[];
};

export type CommunityProposalVoteConfig = {
  voteMode: CommunityProposalVoteMode;
  resultVisibility: CommunityProposalVoteVisibility;
  eligibilityRule: CommunityProposalVoteEligibility;
  votingOpensAt?: string | null;
  votingClosesAt?: string | null;
};

export type CommunityProposalVoteRecord = {
  voterId: string;
  voterUsername: string;
  membershipRole: string;
  verifiedMember: boolean;
  choice?: CommunityProposalVoteChoice | null;
  scoreValue?: number | null;
  castAt: string;
};

export type CommunityProposalVoteScoreBucket = {
  score: number;
  count: number;
};

export type CommunityProposalVoteTally = {
  visible: boolean;
  visibilityReason?: string | null;
  totalBallots: number;
  distinctVoters: number;
  turnoutPercentage: number;
  forVotes: number;
  againstVotes: number;
  averageScore?: number | null;
  scoreDistribution: CommunityProposalVoteScoreBucket[];
};

export type CommunityProposalVoteAuditSummary = {
  acceptedVotes: number;
  duplicateBlockedAttempts: number;
  eligibilityBlockedAttempts: number;
  closedWindowBlockedAttempts: number;
};

export type CommunityProposalVoting = {
  proposalId: string;
  config: CommunityProposalVoteConfig;
  openForVoting: boolean;
  canCurrentUserVote: boolean;
  blockedReason?: string | null;
  currentUserVote?: CommunityProposalVoteRecord | null;
  tally: CommunityProposalVoteTally;
  auditSummary: CommunityProposalVoteAuditSummary;
};

export type CommunityProjectStatus = "TODO" | "IN_PROGRESS" | "DONE";

export type CommunityProjectTaskComment = {
  id: string;
  taskId: string;
  authorId: string;
  authorUsername: string;
  content: string;
  createdAt: string;
};

export type CommunityProjectTask = {
  id: string;
  projectBoardId: string;
  title: string;
  details: string;
  status: CommunityProjectStatus;
  assigneeId?: string | null;
  assigneeUsername?: string | null;
  dueDate?: string | null;
  sortOrder: number;
  comments: CommunityProjectTaskComment[];
  createdAt: string;
  updatedAt: string;
};

export type CommunityProjectTaskCounts = {
  todo: number;
  inProgress: number;
  done: number;
};

export type CommunityProjectBoard = {
  id: string;
  communityId: string;
  linkedProposalId?: string | null;
  linkedProposalTitle?: string | null;
  ownerId: string;
  ownerUsername: string;
  title: string;
  summary: string;
  dueDate?: string | null;
  taskCounts: CommunityProjectTaskCounts;
  tasks: CommunityProjectTask[];
  createdAt: string;
  updatedAt: string;
};

export type GovernanceDocumentType = "STATUTE" | "REGULATION" | "MINUTES" | "AGREEMENT" | "BUDGET" | "REPORT";
export type GovernanceDocumentVisibility = "PUBLIC" | "COMMUNITY" | "ADMINS";

export type GovernanceDocumentVersion = {
  id: string;
  documentId: string;
  createdBy: string;
  authorUsername: string;
  versionNumber: number;
  content: string;
  changeSummary: string;
  sourceUrl: string | null;
  effectiveDate: string | null;
  meetingDate: string | null;
  createdAt: string;
};

export type GovernanceDocument = {
  id: string;
  communityId: string;
  createdBy: string;
  authorUsername: string;
  title: string;
  summary: string;
  documentType: GovernanceDocumentType;
  visibility: GovernanceDocumentVisibility;
  tags: string[];
  currentVersionNumber: number;
  currentVersion: GovernanceDocumentVersion | null;
  versions: GovernanceDocumentVersion[];
  createdAt: string;
  updatedAt: string;
};

export type CommunityDecisionType =
  | "APPROVAL"
  | "REJECTION"
  | "PRIORITIZATION"
  | "DIRECTIVE"
  | "STATUS_UPDATE";

export type CommunityDecisionStatus =
  | "RECORDED"
  | "IN_EXECUTION"
  | "COMPLETED"
  | "REJECTED"
  | "SUPERSEDED";

export type CommunityDecisionBasisType =
  | "COMMUNITY_VOTE"
  | "COORDINATOR_REVIEW"
  | "GOVERNANCE_RECORD"
  | "STAFF_DIRECTIVE"
  | "MIXED_RECORD";

export type CommunityDecision = {
  id: string;
  communityId: string;
  linkedProposalId?: string | null;
  linkedProposalTitle?: string | null;
  governanceDocumentId?: string | null;
  governanceDocumentTitle?: string | null;
  projectBoardId?: string | null;
  projectBoardTitle?: string | null;
  decidedBy: string;
  decidedByUsername: string;
  executionOwnerId?: string | null;
  executionOwnerUsername?: string | null;
  decisionType: CommunityDecisionType;
  decisionStatus: CommunityDecisionStatus;
  approvalBasisType: CommunityDecisionBasisType;
  approvalBasisSummary: string;
  title: string;
  summary: string;
  decidedAt: string;
  effectiveDate?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CommunityHomeSignal = {
  id: string;
  title: string;
  status: string;
  priorityScore?: number | null;
};

export type CommunityHome = {
  communityId: string;
  generatedAt: string;
  freshness: string;
  activeRoomsCount: number;
  officialUpdates: CommunityBlogPost[];
  hotThreads: CommunityThread[];
  topSignals: CommunityHomeSignal[];
};

export type CommunityTrustMetricsPeriod = "LAST_7_DAYS" | "LAST_30_DAYS" | "LAST_90_DAYS";

export type TrustMetricCard = {
  key: string;
  label: string;
  value: string;
  unit: string;
  definition: string;
  formula: string;
  supportingText: string;
};

export type TrustMetricBreakdownItem = {
  label: string;
  value: number;
  share: number;
};

export type TrustMetricBreakdown = {
  key: string;
  title: string;
  description: string;
  items: TrustMetricBreakdownItem[];
};

export type CommunityTrustMetrics = {
  communityId: string;
  communityName: string;
  communitySlug: string;
  period: CommunityTrustMetricsPeriod;
  startDate: string;
  endDate: string;
  generatedAt: string;
  lastUpdatedAt: string | null;
  freshness: string;
  lowData: boolean;
  lowDataReason: string | null;
  cards: TrustMetricCard[];
  breakdowns: TrustMetricBreakdown[];
};

export type CommunityModerationReasonCode =
  | "ABUSE"
  | "HARASSMENT"
  | "SPAM"
  | "MISINFORMATION"
  | "OFF_TOPIC"
  | "OTHER";

export type CommunityModerationTargetType =
  | "THREAD_MESSAGE"
  | "PROPOSAL_DELIBERATION";

export type CommunityModerationReportStatus =
  | "OPEN"
  | "ACTIONED"
  | "DISMISSED";

export type CommunitySanctionType =
  | "WARN"
  | "LIMIT_POSTING_7_DAYS"
  | "SUSPEND_7_DAYS"
  | "SUSPEND_30_DAYS";

export type CommunitySanction = {
  id: string;
  sanctionType: CommunitySanctionType;
  status: string;
  reason: string;
  targetUserId: string;
  targetUsername: string;
  issuedByUserId: string;
  issuedByUsername: string;
  startsAt: string;
  endsAt?: string | null;
  appealAvailable: boolean;
};

export type CommunityModerationAction = {
  actionType: string;
  actorUsername: string;
  note: string;
  happenedAt: string;
};

export type CommunityModerationReport = {
  id: string;
  communityId: string;
  targetType: CommunityModerationTargetType;
  targetId: string;
  targetPreview: string;
  reporterUserId: string;
  reporterUsername: string;
  reportedUserId: string;
  reportedUsername: string;
  reasonCode: CommunityModerationReasonCode;
  details: string;
  status: CommunityModerationReportStatus;
  contentHidden: boolean;
  falsePositiveReviewRecommended: boolean;
  resolutionReason?: string | null;
  resolvedByUsername?: string | null;
  sanction?: CommunitySanction | null;
  actionHistory: CommunityModerationAction[];
  createdAt: string;
  resolvedAt?: string | null;
};

export type CommunityModerationQueue = {
  communityId: string;
  openReports: number;
  actionedReports: number;
  dismissedReports: number;
  activeSanctions: number;
  reports: CommunityModerationReport[];
};

export type SignalMapFilters = {
  category?: string | null;
  statuses: string[];
  fromDate?: string | null;
  toDate?: string | null;
};

export type CommunitySignalMapPoint = {
  signalId: string;
  communityId: string;
  communityName: string;
  title: string;
  category: string;
  status: string;
  locationLabel?: string | null;
  latitude: number;
  longitude: number;
  priorityScore: number;
  heatWeight: number;
  createdAt: string;
};

export type CommunitySignalCluster = {
  clusterKey: string;
  communityId: string;
  communityName: string;
  latitude: number;
  longitude: number;
  signalCount: number;
  cumulativePriorityScore: number;
  primaryCategory: string;
  topSignalId?: string | null;
  topSignalTitle?: string | null;
};

export type CommunitySignalMap = {
  communityId: string;
  communityName: string;
  communitySlug: string;
  generatedAt: string;
  freshness: string;
  filters: SignalMapFilters;
  availableCategories: string[];
  availableStatuses: string[];
  mappedSignalsCount: number;
  unmappedSignalsCount: number;
  cumulativeHeatScore: number;
  points: CommunitySignalMapPoint[];
  clusters: CommunitySignalCluster[];
};

export type CommunitySignalHeatCell = {
  communityId: string;
  communityName: string;
  communitySlug: string;
  latitude: number;
  longitude: number;
  mappedSignalsCount: number;
  unmappedSignalsCount: number;
  cumulativeHeatScore: number;
  averagePriorityScore: number;
  topCategory: string;
  topSignalId?: string | null;
  topSignalTitle?: string | null;
};

export type CommunitySignalsHeatMap = {
  generatedAt: string;
  freshness: string;
  filters: SignalMapFilters;
  availableCategories: string[];
  availableStatuses: string[];
  visibleCommunitiesCount: number;
  totalMappedSignalsCount: number;
  totalHeatScore: number;
  communities: CommunitySignalHeatCell[];
};

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

export type ThreadStatusFilter = "ALL" | "ACTIVE" | "STALE";
export type ThreadSortBy = "RELEVANCE" | "RECENT";
