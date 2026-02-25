import { TFunction } from "i18next";

const normalizeRoleToken = (value: string): string => {
  return value.trim().toUpperCase().replace(/^ROLE_/, "");
};

const humanizeRoleToken = (value: string): string => {
  return value
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((chunk) => chunk.charAt(0).toUpperCase() + chunk.slice(1))
    .join(" ");
};

export const toRoleLabel = (rawRole: string, t: TFunction): string => {
  const normalized = normalizeRoleToken(rawRole);
  return t(`settings.roles.${normalized}`, {
    defaultValue: humanizeRoleToken(normalized),
  });
};

export const toRoleListLabel = (rawRoles: string, t: TFunction): string => {
  return rawRoles
    .split(",")
    .map((role) => role.trim())
    .filter(Boolean)
    .map((role) => toRoleLabel(role, t))
    .join(" • ");
};
