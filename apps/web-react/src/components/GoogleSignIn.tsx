import { useCallback, useEffect, useRef, useState } from 'react';
import apiClient from '../api/axios';

const GIS_SCRIPT = 'https://accounts.google.com/gsi/client';

/**
 * Renders Google's own button. The client id comes from the API at runtime so the web image does
 * not need to be rebuilt when the credential changes. Without a configured client id the component
 * renders nothing and the password form stays the only way in.
 */
export function GoogleSignIn({ onCredential }: { onCredential: (credential: string) => void }) {
  const container = useRef<HTMLDivElement>(null);
  const [clientId, setClientId] = useState<string | null>(null);

  const handleCredential = useCallback(
    (response: { credential: string }) => onCredential(response.credential),
    [onCredential]
  );

  useEffect(() => {
    let cancelled = false;
    apiClient
      .get<{ clientId: string }>('auth/google/config')
      .then(({ data }) => {
        if (!cancelled && data?.clientId) {
          setClientId(data.clientId);
        }
      })
      .catch(() => {
        // Google sign-in is optional; the password form still works.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!clientId || !container.current) {
      return;
    }
    const render = () => {
      if (!window.google || !container.current) {
        return;
      }
      window.google.accounts.id.initialize({ client_id: clientId, callback: handleCredential });
      container.current.innerHTML = '';
      window.google.accounts.id.renderButton(container.current, {
        theme: 'outline',
        size: 'large',
        text: 'continue_with',
        width: 320
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
  return <div ref={container} className="flex justify-content-center mt-4" data-testid="google-signin" />;
}
