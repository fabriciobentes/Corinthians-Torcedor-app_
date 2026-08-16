import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PUBLIC = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "public");
const contracts = {
  "fixtures.json": (data) =>
    Array.isArray(data.fixtures) &&
    data.fixtures.every((match) => Array.isArray(match.broadcasters)),
  "live.json": (data) =>
    Array.isArray(data.liveMatches) &&
    typeof data.eventsByFixture === "object",
  "standings.json": (data) =>
    Array.isArray(data.competitions) &&
    data.competitions.every((competition) =>
      Array.isArray(competition.groups) && Array.isArray(competition.brackets)
    ),
  "stats.json": (data) =>
    data.summary?.matches > 0 &&
    Array.isArray(data.form) &&
    Array.isArray(data.recentMatches) &&
    Array.isArray(data.matchDetails)
};

for (const [name, valid] of Object.entries(contracts)) {
  const target = path.join(PUBLIC, name);
  if (!fs.existsSync(target)) throw new Error(`Arquivo obrigatório ausente: ${name}`);
  const data = JSON.parse(fs.readFileSync(target, "utf8"));
  if (data.schemaVersion !== 2 || !data.generatedAt || !valid(data)) {
    throw new Error(`Contrato inválido em ${name}`);
  }
}

console.log("JSONs validados com sucesso.");
