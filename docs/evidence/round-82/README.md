# Round 82 evidence

Deliverable: `community-official-announcements` is green. Verified 1 passed (5.5s).

## The mock was never the problem

Round 81 left a note claiming the POST mock was not feeding the rendered list. That was wrong, and
checking it cost one grep: the spec has no POST route at all, only three GETs. It never creates
anything. The posts it asserts come from the `blog?communityId=` fixture, and the timeline was
showing exactly what the fixture provided.

The failure I hit in round 81 was my own bad fix. I had asserted the official timeline contained
"Official maintenance window this Saturday." That string is the *content* of the post titled "Water
interruption notice" (fixture line 100-103) - a different post from the one in the timeline. The
assertion was wrong and I had written it confidently into the plan as a mock divergence.

## Three things were wrong in one 12-line test

1. `page.goto('http://127.0.0.1:5173/communities/blog')` - an absolute URL to another project's port.
   The round 73 class. Now relative.
2. `await expect(page.getByText('Official')).toBeVisible()` - asserted a translated badge
   (`t("community_blog.official_badge")`) by its English literal. Cannot match in every locale;
   the round 65 class.
3. `await expect(page.getByText('Pinned')).toBeVisible()` - worse. `Pinned` exists in the view only
   as a boolean flag (`post.pinned`), with no label and no testid, so there is no element carrying
   that text at all. This assertion could never pass in any locale, and had been sitting in the spec
   as if it verified something.

Replacements assert what the product actually renders:

```ts
await expect(page.getByTestId('pinned-announcements-section')).toContainText('Water interruption notice');
await expect(page.getByTestId('official-timeline-section')).toContainText('Road work update');
await expect(page.getByTestId('official-archive-section')).toContainText('Archived transit reroute');
```

The archive assertion is new coverage: the section had three official posts in the fixture and no
assertion against it at all. Nothing was weakened - two unmatchable text assertions were replaced by
one assertion that runs.

## The gate change is not in this commit, and the reason is more interesting

Adding this spec makes the gate 20 entries. That run gave **93 passed, 1 flaky**, and the flaky was:

```
1) [mobile-chrome] > src/tests/merge-review.spec.ts:157
   "Duplicate review > rejecting records a decision without claiming a merge"
```

Round 80's flake was `community-trust-metrics` on mobile-chrome. This one is `merge-review`, also on
mobile-chrome. Two different specs, two different rounds, same browser project.

That pattern is the actual finding. Two locator fixes did not stop flakiness arriving from a
different direction each time, which is what a per-spec cause would not do. Whatever it is, it is
shared - most likely the mobile-chrome project's viewport, timing, or contention with other workers,
rather than anything inside either spec. Chasing it one flake at a time is what rounds 79 and 80 did,
and it is why the gate reverted twice.

So the gate change is reverted for the third time and the systemic flake is the next round's work.
Committing a 20-spec gate that needs a retry would again trade a real regression for a cosmetic one.

## Verification

`announcements-fixed.txt` - the spec alone, **1 passed (5.5s)**. That is the honest scope of this
commit; the gate remains at 19 specs exactly as in `HEAD`.

## Known limitations

- The mobile-chrome flake is unfixed and is now suspected to be systemic. Until it is understood, the
  gate cannot grow and two consecutive green runs do not prove the change is safe.
- Round 81's evidence contains the mock-divergence claim this round disproves. Correcting the record
  mattered more than the fix, because that claim would have sent the next round after a problem that
  does not exist.
- No production code changed, so no screenshots.