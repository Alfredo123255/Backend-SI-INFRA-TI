import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const inputPath = "../src/main/resources/data/inventario.xlsx";
const outputPath = "../src/main/resources/data/inventario.updated.xlsx";

const input = await FileBlob.load(inputPath);
const workbook = await SpreadsheetFile.importXlsx(input);
const sheet = workbook.worksheets.getItem("ActivosTI");

const before = await workbook.inspect({
  kind: "table",
  sheetId: "ActivosTI",
  range: "A1:R9",
  include: "values,formulas",
  tableMaxRows: 9,
  tableMaxCols: 18,
});
console.log("BEFORE");
console.log(before.ndjson);

const beforePreview = await workbook.render({
  sheetName: "ActivosTI",
  range: "A1:R9",
  scale: 1.5,
  format: "png",
});
await fs.writeFile("before.png", new Uint8Array(await beforePreview.arrayBuffer()));

sheet.getRange("Q4:R4").values = [[36.8, 410]];
sheet.getRange("K9").values = [["Analytics"]];

workbook.recalculate();

const after = await workbook.inspect({
  kind: "table",
  sheetId: "ActivosTI",
  range: "A1:R9",
  include: "values,formulas",
  tableMaxRows: 9,
  tableMaxCols: 18,
});
console.log("AFTER");
console.log(after.ndjson);

const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!",
  options: { useRegex: true, maxResults: 300 },
  summary: "final formula error scan",
});
console.log("ERRORS");
console.log(errors.ndjson);

const afterPreview = await workbook.render({
  sheetName: "ActivosTI",
  range: "A1:R9",
  scale: 1.5,
  format: "png",
});
await fs.writeFile("after.png", new Uint8Array(await afterPreview.arrayBuffer()));

const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(outputPath);
