export const FORM_LIMITS = {
  report: {
    titleMin: 5,
    titleMax: 120,
    descriptionMin: 20,
    descriptionMax: 1200,
  },
  blog: {
    titleMin: 8,
    titleMax: 140,
    contentMin: 20,
    contentMax: 4000,
  },
  threads: {
    titleMin: 6,
    titleMax: 140,
    messageMin: 8,
    messageMax: 2000,
  },
} as const;
