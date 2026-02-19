import { readFileSync, writeFileSync, existsSync } from "node:fs";

function parseCsv(content) {
  const lines = content.split("\n").filter(l => l.trim());
  const header = lines[0].split(",");
  return lines.slice(1).map(line => {
    const values = line.split(",");
    const obj = {};
    header.forEach((h, i) => obj[h.trim()] = values[i]?.trim());
    return obj;
  });
}

function mapToSignal(raw, source) {
  return {
    id: `sig-${source}-${Math.random().toString(36).substr(2, 5)}`,
    title: raw.title || raw.text || raw.subject || "Untitled Signal",
    urgency: parseInt(raw.urgency || 1),
    impact: parseInt(raw.impact || 1),
    affectedPeople: parseInt(raw.affectedPeople || raw.reach || 0),
    communityVotes: parseInt(raw.votes || 0),
    category: raw.category || "uncategorized",
    status: "NEW",
    metadata: {
      source,
      ingestedAt: new Date().toISOString()
    }
  };
}

function main() {
  const inputPath = process.argv[2];
  const type = process.argv[3] || "json"; // json, csv, chat
  const targetPath = "examples/feedback.json";

  if (!inputPath || !existsSync(inputPath)) {
    console.error("Usage: node src/ingest.mjs <input-file> [json|csv|chat]");
    process.exit(1);
  }

  const rawContent = readFileSync(inputPath, "utf8");
  let records = [];

  if (type === "csv") {
    records = parseCsv(rawContent);
  } else {
    records = JSON.parse(rawContent);
    if (!Array.isArray(records)) records = [records];
  }

  const newSignals = records.map(r => mapToSignal(r, type));
  
  let existing = [];
  if (existsSync(targetPath)) {
    existing = JSON.parse(readFileSync(targetPath, "utf8"));
  }

  const updated = [...existing, ...newSignals];
  writeFileSync(targetPath, JSON.stringify(updated, null, 2), "utf8");

  console.log(`Ingested ${newSignals.length} signals from ${inputPath} into ${targetPath}`);
}

main();
