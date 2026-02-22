interface CivicCharacterCountProps {
  current: number;
  max: number;
  min?: number;
}

export function CivicCharacterCount({ current, max, min }: CivicCharacterCountProps) {
  const remaining = max - current;
  const isNearLimit = remaining <= Math.max(10, Math.floor(max * 0.1));
  const isBelowMin = typeof min === "number" && current < min;

  return (
    <small className={`text-xs ${isNearLimit || isBelowMin ? "text-status-progress" : "text-muted"}`}>
      {current}/{max}
      {typeof min === "number" ? ` · min ${min}` : ""}
    </small>
  );
}

