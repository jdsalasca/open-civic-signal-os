export type ScoreBreakdown = {
  urgency: number;
  impact: number;
  affectedPeople: number;
  communityVotes: number;
};

export type Signal = {
  id: string;
  title: string;
  description: string;
  category: string;
  status: string;
  priorityScore: number;
  scoreBreakdown: ScoreBreakdown;
};

export type Notification = {
  id: string;
  channel: string;
  message: string;
  recipientGroup: string;
  sentAt: string;
};

export type UserRole = "PUBLIC_SERVANT" | "CITIZEN" | "GUEST";

export type AuthInfo = {
  user: string;
  role: UserRole;
  token: string;
};
