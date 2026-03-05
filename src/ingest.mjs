import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { createHash } from "node:crypto";

function parseCsv(content) {
  const lines = content.split("\n").filter((line) => line.trim());
  if (lines.length < 2) {
    throw new Error("CSV input must include a header and at least one data row.");
  }

  const header = lines[0].split(",").map((h) => h.trim());
  return lines.slice(1).map((line, rowIndex) => {
    const values = line.split(",");
    if (values.length !== header.length) {
      throw new Error(`Invalid CSV row ${rowIndex + 2}: expected ${header.length} columns, got ${values.length}.`);
    }

    const obj = {};
    header.forEach((h, i) => {
      obj[h] = values[i]?.trim();
    });
    return obj;
  });
}

function parseInteger(value, fallback, fieldName) {
  if (value === undefined || value === null || value === "") {
    return fallback;
  }

  const parsed = Number.parseInt(String(value), 10);
  if (Number.isNaN(parsed)) {
    throw new Error(`Invalid numeric value for '${fieldName}': '${value}'.`);
  }
  return parsed;
}

function stableStringify(value) {
  if (Array.isArray(value)) {
    return `[${value.map(stableStringify).join(",")}]`;
  }
  if (value && typeof value === "object") {
    const keys = Object.keys(value).sort();
    const pairs = keys.map((key) => `${JSON.stringify(key)}:${stableStringify(value[key])}`);
    return `{${pairs.join(",")}}`;
  }
  return JSON.stringify(value);
}

function buildDeterministicId(raw, source) {
  const fingerprint = createHash("sha256")
    .update(`${source}:${stableStringify(raw)}`)
    .digest("hex")
    .slice(0, 12);
  return `sig-${source}-${fingerprint}`;
}

function mapToSignal(raw, source) {
  const title = raw.title || raw.text || raw.subject || "Untitled Signal";
  const urgency = parseInteger(raw.urgency, 1, "urgency");
  const impact = parseInteger(raw.impact, 1, "impact");
  const affectedPeople = parseInteger(raw.affectedPeople ?? raw.reach, 0, "affectedPeople");
  const communityVotes = parseInteger(raw.votes, 0, "votes");
  const id = buildDeterministicId(raw, source);

  return {
    id,
    title,
    urgency,
    impact,
    affectedPeople,
    communityVotes,
    category: raw.category || "uncategorized",
    status: raw.status || "NEW",
    metadata: {
      source,
      transformVersion: "ingest-v2",
      fingerprint: id,
      ingestedAt: new Date().toISOString()
    }
  };
}

function readRecords(inputPath, type) {
  const rawContent = readFileSync(inputPath, "utf8");
  if (type === "csv") {
    return parseCsv(rawContent);
  }

  const records = JSON.parse(rawContent);
  return Array.isArray(records) ? records : [records];
}

function mergeIdempotent(existing, incoming) {
  const mergedById = new Map(existing.map((item) => [item.id, item]));

  for (const item of incoming) {
    const previous = mergedById.get(item.id);
    if (previous) {
      const previousIngestedAt = previous?.metadata?.ingestedAt;
      mergedById.set(item.id, {
        ...item,
        metadata: {
          ...item.metadata,
          ingestedAt: previousIngestedAt || item.metadata.ingestedAt
        }
      });
      continue;
    }
    mergedById.set(item.id, item);
  }

  return Array.from(mergedById.values());
}

function main() {
  const inputPath = process.argv[2];
  const type = process.argv[3] || "json"; // json, csv, chat
  const targetPath = process.argv[4] || "examples/feedback.json";

  if (!inputPath || !existsSync(inputPath)) {
    console.error("Usage: node src/ingest.mjs <input-file> [json|csv|chat] [target-file]");
    process.exit(1);
  }

  const records = readRecords(inputPath, type);
  const newSignals = records.map((record) => mapToSignal(record, type));

  let existing = [];
  if (existsSync(targetPath)) {
    existing = JSON.parse(readFileSync(targetPath, "utf8"));
    if (!Array.isArray(existing)) {
      throw new Error(`Target file must contain an array: ${targetPath}`);
    }
  }

  const updated = mergeIdempotent(existing, newSignals);
  writeFileSync(targetPath, JSON.stringify(updated, null, 2), "utf8");

  const deduped = newSignals.length - (updated.length - existing.length);
  console.log(`Ingested ${newSignals.length} records from ${inputPath} into ${targetPath} (${Math.max(0, deduped)} deduplicated).`);
}

main();
