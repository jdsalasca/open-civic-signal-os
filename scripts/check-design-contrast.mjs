#!/usr/bin/env node
/**
 * WCAG AA contrast gate for the Paper Craft design tokens.
 *
 * <p>Tokens live in apps/web-react/src/styles/theme.scss inside two mixins
 * (`papercraft-light-tokens` / `papercraft-dark-tokens`). This script reads those
 * declarations straight from the source of truth, resolves aliases, and fails when a
 * declared text pair drops under 4.5:1 (or a non-text UI pair under 3:1). It exists so a
 * palette tweak cannot silently ship unreadable ink: the check is cheap, deterministic,
 * and runs outside the browser.
 *
 * Usage: node scripts/check-design-contrast.mjs
 */
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const THEME_SCSS = path.resolve(here, '..', 'apps', 'web-react', 'src', 'styles', 'theme.scss');

const AA_TEXT = 4.5;
const AA_UI = 3;

/** [foreground token, background token, minimum ratio, kind] */
const TEXT_PAIRS = [
  ['text-main', 'bg-app', AA_TEXT],
  ['text-main', 'bg-surface', AA_TEXT],
  ['text-main', 'bg-card', AA_TEXT],
  ['text-main', 'bg-elevated', AA_TEXT],
  ['text-secondary', 'bg-app', AA_TEXT],
  ['text-secondary', 'bg-card', AA_TEXT],
  ['text-muted', 'bg-app', AA_TEXT],
  ['text-muted', 'bg-surface', AA_TEXT],
  ['text-muted', 'bg-card', AA_TEXT],
  ['link-color', 'bg-app', AA_TEXT],
  ['link-color', 'bg-card', AA_TEXT],
  ['on-brand', 'brand-primary', AA_TEXT],
  ['on-brand', 'status-new', AA_TEXT],
  ['on-brand', 'status-progress', AA_TEXT],
  ['on-brand', 'status-resolved', AA_TEXT],
  ['on-brand', 'status-rejected', AA_TEXT],
  ['status-new', 'bg-card', AA_TEXT],
  ['status-progress', 'bg-card', AA_TEXT],
  ['status-resolved', 'bg-card', AA_TEXT],
  ['status-rejected', 'bg-card', AA_TEXT],
  ['tooltip-text', 'tooltip-bg', AA_TEXT],
  ['field-text', 'field-bg', AA_TEXT],
  ['interactive-neutral-text', 'interactive-neutral-bg', AA_TEXT],
  ['logo-icon', 'logo-surface', AA_TEXT],
  ['color-accent', 'bg-card', AA_TEXT],
  ['brand-accent', 'bg-card', AA_TEXT],
  ['color-plum', 'bg-card', AA_TEXT],
];

/** Non-text pairs (focus ring, chart series) only need 3:1. */
const UI_PAIRS = [
  ['focus-ring-color', 'bg-app', AA_UI],
  ['chart-1', 'bg-card', AA_UI],
  ['chart-2', 'bg-card', AA_UI],
  ['chart-3', 'bg-card', AA_UI],
  ['chart-4', 'bg-card', AA_UI],
  ['chart-5', 'bg-card', AA_UI],
  ['chart-6', 'bg-card', AA_UI],
];

function parseMixin(scss, name) {
  const pattern = new RegExp(`@mixin\\s+${name}\\s*\\{([\\s\\S]*?)\\n\\}`);
  const match = scss.match(pattern);
  if (!match) {
    throw new Error(`theme.scss does not declare @mixin ${name}`);
  }
  const tokens = new Map();
  for (const [, token, value] of match[1].matchAll(/(--[a-z0-9-]+)\s*:\s*([^;]+);/gi)) {
    tokens.set(token, value.trim());
  }
  return tokens;
}

function parseHex(hex) {
  let clean = hex.slice(1);
  if (clean.length === 3) {
    clean = clean.split('').map((c) => c + c).join('');
  }
  return [0, 2, 4].map((i) => parseInt(clean.slice(i, i + 2), 16));
}

function toHex([r, g, b]) {
  return `#${[r, g, b].map((c) => Math.round(c).toString(16).padStart(2, '0')).join('')}`;
}

/** Translucent paints are composited over the app ground before measuring. */
function composite(raw, baseHex) {
  const match = raw.match(/^rgba?\(\s*([\d.]+)[,\s]+([\d.]+)[,\s]+([\d.]+)(?:[,\s/]+([\d.]+))?\s*\)$/i);
  if (!match) {
    return null;
  }
  const [, r, g, b, a = '1'] = match;
  const [br, bg, bb] = parseHex(baseHex);
  const alpha = Number(a);
  return toHex([
    Number(r) * alpha + br * (1 - alpha),
    Number(g) * alpha + bg * (1 - alpha),
    Number(b) * alpha + bb * (1 - alpha),
  ]);
}

function resolve(tokens, token, seen = new Set()) {
  const key = token.startsWith('--') ? token : `--${token}`;
  if (seen.has(key)) {
    throw new Error(`circular token reference at ${key}`);
  }
  seen.add(key);
  const raw = tokens.get(key);
  if (!raw) {
    throw new Error(`token ${key} is not defined in this theme`);
  }
  const alias = raw.match(/^var\(\s*(--[a-z0-9-]+)\s*\)$/i);
  if (alias) {
    return resolve(tokens, alias[1], seen);
  }
  if (/^#[0-9a-f]{3,8}$/i.test(raw)) {
    return raw;
  }
  const base = tokens.get('--bg-app');
  const blended = composite(raw, base);
  if (blended) {
    return blended;
  }
  throw new Error(`token ${key} is not a colour (got "${raw}")`);
}

function channel(value) {
  const c = value / 255;
  return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
}

function luminance(hex) {
  let clean = hex.slice(1);
  if (clean.length === 3) {
    clean = clean.split('').map((c) => c + c).join('');
  }
  const [r, g, b] = [0, 2, 4].map((i) => parseInt(clean.slice(i, i + 2), 16));
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
}

function contrast(a, b) {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

function checkTheme(themeName, tokens) {
  const failures = [];
  const rows = [];
  const run = (pairs, kind) => {
    for (const [fgToken, bgToken, minimum] of pairs) {
      const fg = resolve(tokens, fgToken);
      const bg = resolve(tokens, bgToken);
      const ratio = contrast(fg, bg);
      const ok = ratio >= minimum;
      rows.push({ themeName, kind, fgToken, bgToken, fg, bg, ratio, minimum, ok });
      if (!ok) {
        failures.push({ themeName, fgToken, bgToken, ratio: ratio.toFixed(2), minimum });
      }
    }
  };
  run(TEXT_PAIRS, 'text');
  run(UI_PAIRS, 'ui');
  return { rows, failures };
}

function main() {
  const scss = readFileSync(THEME_SCSS, 'utf8');
  const light = parseMixin(scss, 'papercraft-light-tokens');
  const dark = parseMixin(scss, 'papercraft-dark-tokens');

  if (light.size === 0 || dark.size === 0) {
    throw new Error('one of the theme mixins parsed to zero tokens');
  }

  const results = [checkTheme('light', light), checkTheme('dark', dark)];
  for (const { rows, failures } of results) {
    for (const row of rows) {
      const mark = row.ok ? 'PASS' : 'FAIL';
      console.log(
        `[${mark}] ${row.themeName.padEnd(5)} ${row.fgToken} (${row.fg}) on ${row.bgToken} (${row.bg}) = ` +
          `${row.ratio.toFixed(2)}:1 (min ${row.minimum}:1)`,
      );
    }
    if (failures.length > 0) {
      console.error(`\n${failures.length} contrast failure(s):`);
      for (const failure of failures) {
        console.error(
          `  ${failure.themeName}: ${failure.fgToken} on ${failure.bgToken} = ${failure.ratio}:1 ` +
            `(needs ${failure.minimum}:1)`,
        );
      }
      process.exitCode = 1;
    }
  }

  const total = results.reduce((sum, r) => sum + r.rows.length, 0);
  const passed = results.reduce((sum, r) => sum + r.rows.filter((row) => row.ok).length, 0);
  console.log(`\n${passed}/${total} token pairs meet their WCAG threshold.`);
}

main();
