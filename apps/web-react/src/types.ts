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
