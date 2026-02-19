import { readFileSync, writeFileSync } from "node:fs";

function calculateBreakdown(signal) {
  return {
    urgency: signal.urgency * 30,
    impact: signal.impact * 25,
    affectedPeople: Math.min(signal.affectedPeople / 10, 30),
    communityVotes: Math.min(signal.communityVotes / 5, 15)
  };
}

function score(breakdown) {
  return (
    breakdown.urgency +
    breakdown.impact +
    breakdown.affectedPeople +
    breakdown.communityVotes
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
    .map((item) => {
      const breakdown = calculateBreakdown(item);
      return { 
        ...item, 
        priorityScore: Number(score(breakdown).toFixed(2)),
        scoreBreakdown: breakdown,
        status: item.status ?? "NEW"
      };
    })
    .sort((a, b) => b.priorityScore - a.priorityScore);

  writeFileSync(outputPath, JSON.stringify(prioritized, null, 2), "utf8");
  console.log(`Prioritized backlog generated: ${outputPath}`);
}

main();
