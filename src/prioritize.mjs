import { readFileSync } from "node:fs";

// This script used to rank signals in Node, duplicating the scoring formula from
// PrioritizationServiceImpl.getBreakdown. That was removed on purpose:
//
//   1. AGENTS.md is explicit that the backend owns domain calculations and that no other layer
//      implements ranking independently. A second copy is a second set of weights that nobody
//      reviews.
//   2. A copy cannot detect that it has drifted. The real formula is versioned and published at
//      GET /api/signals/formula; this file had no way to learn the version, so it would have
//      kept agreeing with a stale formula long after the backend moved on.
//   3. It sorted by score with no tiebreaker, so equal scores fell to input order, which is
//      neither the backend's order nor guaranteed stable across a differently ordered file.
//
// What remains is a shape check on the example dataset, which is genuinely useful offline and
// invents no scores. For a real ranked backlog, ask the backend:
//
//   npm run ranking:check
//
// That calls the backend twice and fails if the two orderings disagree.

function main() {
  const inputPath = process.argv[2] ?? "examples/feedback.json";
  const raw = readFileSync(inputPath, "utf8");
  let data;
  try {
    data = JSON.parse(raw);
  } catch (error) {
    throw new Error(`Example dataset is not valid JSON: ${error.message}`);
  }

  if (!Array.isArray(data)) {
    throw new Error("Input must be an array of civic signals.");
  }

  const problems = [];
  const seenIds = new Set();

  data.forEach((signal, index) => {
    const where = `signal at index ${index}`;
    if (!signal || typeof signal !== "object") {
      problems.push(`${where} is not an object`);
      return;
    }
    if (typeof signal.id !== "string" || signal.id.length === 0) {
      problems.push(`${where} has no id`);
    } else if (seenIds.has(signal.id)) {
      // Duplicate ids would break anything that cites a signal as evidence.
      problems.push(`${where} repeats id ${signal.id}`);
    } else {
      seenIds.add(signal.id);
    }

    for (const field of ["title", "category"]) {
      if (typeof signal[field] !== "string" || signal[field].trim().length === 0) {
        problems.push(`${where} has no ${field}`);
      }
    }

    for (const field of ["urgency", "impact", "affectedPeople", "communityVotes"]) {
      const value = signal[field];
      if (typeof value !== "number" || !Number.isFinite(value) || value < 0) {
        problems.push(`${where} has a non-numeric or negative ${field}`);
      }
    }
  });

  if (problems.length > 0) {
    throw new Error(
      `Example dataset failed its shape check:\n  - ${problems.join("\n  - ")}`
    );
  }

  console.log(
    `Example dataset is well-formed: ${data.length} signals, ${seenIds.size} unique ids.`
  );
  console.log(
    "Scores are not computed here on purpose. Ask the backend for a ranking: npm run ranking:check"
  );
}

main();
