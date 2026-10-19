import { classNames } from "primereact/utils";
import { AVATAR_PRESETS } from "../../constants/profileIdentity";

type CivicIdentityAvatarProps = {
  presetId?: string | null;
  fallbackLabel: string;
  size?: "sm" | "md" | "lg";
  className?: string;
};

export function CivicIdentityAvatar({
  presetId,
  fallbackLabel,
  size = "md",
  className
}: CivicIdentityAvatarProps) {
  const preset = AVATAR_PRESETS.find((item) => item.id === presetId) ?? AVATAR_PRESETS[0];

  return (
    <div
      className={classNames(
        "civic-identity-avatar",
        `civic-identity-avatar-${size}`,
        preset.accentClassName,
        className
      )}
      aria-hidden="true"
    >
      <div className={classNames("civic-identity-avatar-art", preset.artClassName)}>
        <div className="civic-identity-avatar-glow" />
        <div className="civic-identity-avatar-orb">
          <i className={classNames(preset.icon, "civic-identity-avatar-icon")} />
        </div>
        <span className="civic-identity-avatar-letter">{fallbackLabel.slice(0, 1).toUpperCase() || "U"}</span>
      </div>
    </div>
  );
}
