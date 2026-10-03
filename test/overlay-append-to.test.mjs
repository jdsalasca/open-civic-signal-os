// Guards a rule that no functional browser test can catch.
//
// A PrimeReact overlay rendered inline is clipped by any ancestor with overflow, and the popup
// then opens half-offscreen or not at all. Nothing throws and every existing functional spec still
// passes: a booking flow can be driven without ever opening the calendar popup. The only way to
// notice is to look at it.
//
// So this reads the sources and asserts the mount target directly. Removing appendTo is a
// one-line change; this is the check that fails when someone does.

import { readFileSync, readdirSync } from 'node:fs';
import path from 'node:path';

const viewDir = path.resolve(import.meta.dirname, '..', 'apps', 'web-react', 'src', 'views');

// PrimeReact components that render a floating panel instead of inline content.
const OVERLAY_COMPONENTS = [
  'Dropdown',
  'Select',
  'MultiSelect',
  'Calendar',
  'OverlayPanel',
  'Listbox',
];

const failures = [];

console.log('floating panels mount outside their container');

for (const file of readdirSync(viewDir).filter((f) => f.endsWith('.tsx'))) {
  const lines = readFileSync(path.join(viewDir, file), 'utf8').split(/\r?\n/);
  const missing = [];

  lines.forEach((line, index) => {
    for (const component of OVERLAY_COMPONENTS) {
      if (!new RegExp(`<${component}\\b`).test(line)) continue;
      // Read to the end of the opening tag, not just this line: props wrap across lines.
      const chunk = lines.slice(index, index + 16).join('\n');
      const end = chunk.indexOf('>');
      const props = end > 0 ? chunk.slice(0, end) : chunk;
      if (!/appendTo/.test(props)) {
        missing.push(`${component} at ${file}:${index + 1}`);
      }
    }
  });

  if (missing.length === 0) {
    console.log(`  ok  ${file}`);
  } else {
    failures.push(file);
    console.error(`  FAIL ${file}: ${missing.join(', ')}`);
  }
}

if (failures.length > 0) {
  console.error(
    `\nThese floating panels render inline and will be clipped by any overflow ancestor:\n  ${failures.join('\n  ')}`,
  );
  process.exit(1);
}
console.log('\nall views mount their floating panels on document.body');