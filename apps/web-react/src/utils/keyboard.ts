export const isSubmitShortcut = (event: { key: string; ctrlKey: boolean; metaKey: boolean }): boolean => {
  return event.key === "Enter" && (event.ctrlKey || event.metaKey);
};
