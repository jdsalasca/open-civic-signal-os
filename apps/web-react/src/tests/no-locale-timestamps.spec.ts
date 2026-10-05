import { expect, test } from '@playwright/test';
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * No view may render a timestamp through the browser's locale.
 *
 * `toLocaleString`, `toLocaleDateString` and `toLocaleTimeString` with no locale argument render
 * from the reader's own settings, so two residents looking at the same audit trail see two
 * different-looking claims about the same data. Rounds 63 and 64 found this by eye, one screen at a
 * time, across 24 call sites; a per-screen assertion cannot stop the next one from being added, but
 * this check can.
 *
 * `formatDate`, `formatTime` and `formatStamp` in `src/utils/formatStamp.ts` are the only sanctioned
 * renderers.
 *
 * `src/tests` is excluded on purpose: an assertion about how a locale renders is legitimate there,
 * and the whole point of this file is to ban the call from the UI, not from the test that proves it.
 */
const SRC_ROOT = fileURLToPath(new URL('..', import.meta.url));

/**
 * Comments mention these method names when explaining the defect, so they are removed first. Newlines
 * are preserved so a reported line number still matches the file the developer will open.
 */
function stripComments(source: string): string {
  return source
    .replace(/\/\*[\s\S]*?\*\//g, (block) => block.replace(/[^\n]/g, ' '))
    .split('\n')
    .map((line) => {
      const at = line.indexOf('//');
      // Leave "https://" and friends alone so a URL cannot swallow the rest of the line.
      if (at === -1 || line[at - 1] === ':') return line;
      return line.slice(0, at);
    })
    .join('\n');
}

function sourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry: string) => {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      return entry === 'tests' ? [] : sourceFiles(full);
    }
    return /\.tsx?$/.test(entry) ? [full] : [];
  });
}

test('no view renders a timestamp through the reader browser locale', () => {
  const offenders = sourceFiles(SRC_ROOT).flatMap((file) =>
    stripComments(readFileSync(file, 'utf8'))
      .split('\n')
      .flatMap((line, index) =>
        // A fresh regex per line: a shared global one would carry lastIndex between lines.
        line.match(/toLocale(?:String|DateString|TimeString)/g)
          ? [`${relative(SRC_ROOT, file).split(sep).join('/')}:${index + 1}`]
          : [],
      ),
  );

  expect(
    offenders,
    `Use formatDate, formatTime or formatStamp from src/utils/formatStamp.ts instead. Offenders:\n${offenders.join('\n')}`,
  ).toEqual([]);
});