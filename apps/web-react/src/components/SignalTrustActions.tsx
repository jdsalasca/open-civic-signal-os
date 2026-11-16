import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-hot-toast';
import apiClient from '../api/axios';
import { SignalConfirmationState } from '../types';

type Props = { signalId: string };

/** Neighbour evidence and follow state for one report. */
export function SignalTrustActions({ signalId }: Props) {
  const { t } = useTranslation();
  const [confirmation, setConfirmation] = useState<SignalConfirmationState | null>(null);
  const [following, setFollowing] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.allSettled([
      apiClient.get<SignalConfirmationState>(`signals/${signalId}/confirmations`),
      apiClient.get<{ following: boolean }>(`follows/SIGNAL/${signalId}`)
    ]).then(([confirmationResult, followResult]) => {
      if (cancelled) return;
      if (confirmationResult.status === 'fulfilled') {
        setConfirmation(confirmationResult.value.data);
      }
      if (followResult.status === 'fulfilled') {
        setFollowing(Boolean(followResult.value.data.following));
      }
    });
    return () => {
      cancelled = true;
    };
  }, [signalId]);

  const toggleConfirmation = async () => {
    setBusy(true);
    try {
      const method = confirmation?.confirmedByViewer ? 'delete' : 'post';
      const { data } = await apiClient.request<SignalConfirmationState>({
        url: `signals/${signalId}/confirmations`,
        method
      });
      setConfirmation(data);
      toast.success(data.confirmedByViewer ? t('community_feed.confirmed') : t('community_feed.unconfirmed'));
    } catch {
      toast.error(t('community_feed.confirm_error'));
    } finally {
      setBusy(false);
    }
  };

  const toggleFollow = async () => {
    setBusy(true);
    try {
      await apiClient.request({
        url: `follows/SIGNAL/${signalId}`,
        method: following ? 'delete' : 'post'
      });
      setFollowing((current) => !current);
      toast.success(following ? t('community_feed.unfollow_success') : t('community_feed.follow_success'));
    } catch {
      toast.error(t('community_feed.follow_error'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="signal-trust-actions" data-testid="signal-trust-actions">
      <button
        type="button"
        onClick={toggleConfirmation}
        disabled={busy}
        aria-pressed={Boolean(confirmation?.confirmedByViewer)}
        data-testid="signal-confirm-button"
      >
        <i className="pi pi-eye" aria-hidden="true" />
        {confirmation?.confirmedByViewer ? t('community_feed.confirmed') : t('community_feed.confirm')}
        {confirmation && confirmation.count > 0 ? ` · ${confirmation.count}` : ''}
      </button>
      <button
        type="button"
        onClick={toggleFollow}
        disabled={busy}
        aria-pressed={following}
        data-testid="signal-follow-button"
      >
        <i className={following ? 'pi pi-check' : 'pi pi-bell'} aria-hidden="true" />
        {following ? t('community_feed.following') : t('community_feed.follow')}
      </button>
    </div>
  );
}
