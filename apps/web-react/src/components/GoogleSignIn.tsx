import { useCallback, useEffect, useRef } from 'react';

const GIS_SCRIPT = 'https://accounts.google.com/gsi/client';

/**
 * Renders Google's own button. The client id arrives from the login page, which reads it from the
 * API at runtime, so the web image does not need a rebuild when the credential changes. Without a
 * configured client id the component renders nothing and the password form (dev only) stays the
 * only way in.
 */
export function GoogleSignIn({
  clientId,
  onCredential
}: {
  clientId: string;
  onCredential: (credential: string) => void;
}) {
  const container = useRef<HTMLDivElement>(null);

  const handleCredential = useCallback(
    (response: { credential: string }) => onCredential(response.credential),
    [onCredential]
  );

  useEffect(() => {
    if (!clientId || !container.current) {
      return;
    }
    const render = () => {
      if (!window.google || !container.current) {
        return;
      }
      const width = Math.min(360, Math.max(240, container.current.clientWidth || 320));
      window.google.accounts.id.initialize({ client_id: clientId, callback: handleCredential });
      container.current.innerHTML = '';
      window.google.accounts.id.renderButton(container.current, {
        theme: 'outline',
        size: 'large',
        shape: 'pill',
        text: 'continue_with',
        logo_alignment: 'left',
        width
      });
    };

    if (window.google) {
      render();
      return;
    }
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${GIS_SCRIPT}"]`);
    if (existing) {
      existing.addEventListener('load', render);
      return () => existing.removeEventListener('load', render);
    }
    const script = document.createElement('script');
    script.src = GIS_SCRIPT;
    script.async = true;
    script.defer = true;
    script.addEventListener('load', render);
    document.head.appendChild(script);
    return () => script.removeEventListener('load', render);
  }, [clientId, handleCredential]);

  if (!clientId) {
    return null;
  }
  return <div ref={container} className="google-signin-slot" data-testid="google-signin" />;
}
