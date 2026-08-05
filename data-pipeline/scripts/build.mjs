import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PUBLIC = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "public");
const contracts = {
  "fixtures.json": (data) => Array.isArray(data.fixtures),
  "live.json": (data) => Array.isArray(data.liveMatches) && typeof data.eventsByFixture === "object",
  "standings.json": (data) => Array.isArray(data.tables)
};

for (const [name, valid] of Object.entries(contracts)) {
  const target = path.join(PUBLIC, name);
  if (!fs.existsSync(target)) throw new Error(`Arquivo obrigatório ausente: ${name}`);
  const data = JSON.parse(fs.readFileSync(target, "utf8"));
  if (data.schemaVersion !== 1 || !data.generatedAt || !valid(data)) {
    throw new Error(`Contrato inválido em ${name}`);
  }
}

console.log("JSONs validados com sucesso.");
