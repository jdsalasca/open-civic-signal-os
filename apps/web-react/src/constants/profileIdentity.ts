export type AvatarPreset = {
  id: string;
  icon: string;
  labelKey: string;
  accentClassName: string;
  artClassName: string;
};

export type AchievementMeta = {
  icon: string;
  titleKey: string;
  descriptionKey: string;
};

export const AVATAR_PRESETS: AvatarPreset[] = [
  { id: "civic-sunrise", icon: "pi pi-sun", labelKey: "settings.avatar_presets.civic_sunrise", accentClassName: "avatar-accent-sunrise", artClassName: "avatar-art-sunrise" },
  { id: "neighborhood-garden", icon: "pi pi-leaf", labelKey: "settings.avatar_presets.neighborhood_garden", accentClassName: "avatar-accent-garden", artClassName: "avatar-art-garden" },
  { id: "library-window", icon: "pi pi-book", labelKey: "settings.avatar_presets.library_window", accentClassName: "avatar-accent-library", artClassName: "avatar-art-library" },
  { id: "bridge-night", icon: "pi pi-link", labelKey: "settings.avatar_presets.bridge_night", accentClassName: "avatar-accent-bridge", artClassName: "avatar-art-bridge" },
  { id: "river-route", icon: "pi pi-compass", labelKey: "settings.avatar_presets.river_route", accentClassName: "avatar-accent-river", artClassName: "avatar-art-river" },
  { id: "campus-sky", icon: "pi pi-building", labelKey: "settings.avatar_presets.campus_sky", accentClassName: "avatar-accent-campus", artClassName: "avatar-art-campus" },
  { id: "plaza-echo", icon: "pi pi-megaphone", labelKey: "settings.avatar_presets.plaza_echo", accentClassName: "avatar-accent-plaza", artClassName: "avatar-art-plaza" },
  { id: "forest-circle", icon: "pi pi-globe", labelKey: "settings.avatar_presets.forest_circle", accentClassName: "avatar-accent-forest", artClassName: "avatar-art-forest" },
  { id: "signal-lantern", icon: "pi pi-bolt", labelKey: "settings.avatar_presets.signal_lantern", accentClassName: "avatar-accent-lantern", artClassName: "avatar-art-lantern" },
  { id: "harbor-light", icon: "pi pi-star", labelKey: "settings.avatar_presets.harbor_light", accentClassName: "avatar-accent-harbor", artClassName: "avatar-art-harbor" }
];

export const ACHIEVEMENT_META: Record<string, AchievementMeta> = {
  VERIFIED_MEMBER: {
    icon: "pi pi-verified",
    titleKey: "settings.achievements.verified_member.title",
    descriptionKey: "settings.achievements.verified_member.description"
  },
  FIRST_REPORT: {
    icon: "pi pi-flag",
    titleKey: "settings.achievements.first_report.title",
    descriptionKey: "settings.achievements.first_report.description"
  },
  TEN_REPORTS: {
    icon: "pi pi-send",
    titleKey: "settings.achievements.ten_reports.title",
    descriptionKey: "settings.achievements.ten_reports.description"
  },
  COMMUNITY_CATALYST: {
    icon: "pi pi-users",
    titleKey: "settings.achievements.community_catalyst.title",
    descriptionKey: "settings.achievements.community_catalyst.description"
  },
  MULTI_COMMUNITY: {
    icon: "pi pi-sitemap",
    titleKey: "settings.achievements.multi_community.title",
    descriptionKey: "settings.achievements.multi_community.description"
  },
  PROFILE_COMPLETE: {
    icon: "pi pi-id-card",
    titleKey: "settings.achievements.profile_complete.title",
    descriptionKey: "settings.achievements.profile_complete.description"
  }
};
