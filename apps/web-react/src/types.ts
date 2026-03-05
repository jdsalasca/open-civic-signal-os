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
  category: string;
  status: string;
  priorityScore: number;
  scoreBreakdown: ScoreBreakdown;
  communityVotes: number;
  reactions: Record<string, number>;
  viewerReaction?: string;
  explainabilitySummary: ExplainabilitySummary;
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
  viewerScope: ProfileVisibility;
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
  createdAt?: string;
};

export type CommunityMembership = {
  userId: string;
  communityId: string;
  communityName: string;
  communitySlug: string;
  role: "MEMBER" | "MODERATOR" | "COORDINATOR" | "PUBLIC_SERVANT_LIAISON";
  createdBy: string;
  createdAt: string;
};

export type CommunityThreadMessage = {
  id: string;
  threadId: string;
  authorId: string;
  sourceCommunityId: string;
  parentMessageId?: string;
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
  messages: CommunityThreadMessage[];
};

export type CommunityBlogPost = {
  id: string;
  communityId: string;
  authorId: string;
  authorUsername: string;
  authorRole: string;
  title: string;
  content: string;
  statusTag: string;
  publishedAt: string;
  updatedAt: string;
  reactions: Record<string, number>;
  viewerReaction?: string;
};

export type SignalStatusEntry = {
  id: string;
  signalId: string;
  statusFrom: string;
  statusTo: string;
  changedBy: string;
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

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

export type ThreadStatusFilter = "ALL" | "ACTIVE" | "STALE";
