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
  | "CREATE_PROPOSAL"
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
