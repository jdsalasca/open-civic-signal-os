const IMAGE_MARKDOWN_REGEX = /!\[[^\]]*\]\((https?:\/\/[^)\s]+)\)/i;
const IMAGE_MARKDOWN_GLOBAL_REGEX = /!\[[^\]]*\]\((https?:\/\/[^)\s]+)\)/g;
const IMAGE_URL_REGEX = /^https?:\/\/[^\s]+$/i;

export const extractFirstImageUrl = (content: string): string | null => {
  const match = content.match(IMAGE_MARKDOWN_REGEX);
  return match?.[1] ?? null;
};

export const stripMarkdownImages = (content: string): string => {
  return content.replace(IMAGE_MARKDOWN_GLOBAL_REGEX, "").trim();
};

export const prependImageToContent = (content: string, imageUrl?: string, alt = "community-image"): string => {
  const trimmedContent = content.trim();
  const normalizedImageUrl = (imageUrl || "").trim();
  if (!normalizedImageUrl) {
    return trimmedContent;
  }
  return `![${alt}](${normalizedImageUrl})${trimmedContent ? `\n\n${trimmedContent}` : ""}`;
};

export const isValidImageUrl = (value?: string): boolean => {
  if (!value) return true;
  return IMAGE_URL_REGEX.test(value.trim());
};
