import { readFileSync, writeFileSync } from "node:fs";

function score(signal) {
  return (
    signal.urgency * 30 +
    signal.impact * 25 +
    Math.min(signal.affectedPeople / 10, 30) +
    Math.min(signal.communityVotes / 5, 15)
  );
}

function main() {
  const inputPath = process.argv[2] ?? "examples/feedback.json";
  const outputPath = process.argv[3] ?? "examples/prioritized-backlog.json";
  const raw = readFileSync(inputPath, "utf8");
  const data = JSON.parse(raw);

  if (!Array.isArray(data)) {
    throw new Error("Input must be an array of civic signals.");
  }

  const prioritized = data
    .map((item) => ({ ...item, priorityScore: Number(score(item).toFixed(2)) }))
    .sort((a, b) => b.priorityScore - a.priorityScore);

  writeFileSync(outputPath, JSON.stringify(prioritized, null, 2), "utf8");
  console.log(`Prioritized backlog generated: ${outputPath}`);
}

main();
