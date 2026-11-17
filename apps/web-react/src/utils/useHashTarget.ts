import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * A feed card links to `#blog-post-…` or `#thread-…`. Those pages render their list
 * asynchronously, so the browser's native fragment jump happens before the target exists and the
 * reader lands at the top. This resolves the fragment once the list is ready.
 */
export function useHashTarget(ready: boolean) {
  const { hash } = useLocation();

  useEffect(() => {
    if (!ready || !hash) return;
    const id = decodeURIComponent(hash.replace(/^#/, ''));
    const target = document.getElementById(id);
    if (!target) return;
    target.scrollIntoView({ block: 'start' });
    if (!target.hasAttribute('tabindex')) {
      target.setAttribute('tabindex', '-1');
    }
    target.focus({ preventScroll: true });
  }, [hash, ready]);
}
