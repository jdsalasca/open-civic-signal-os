import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-hot-toast';
import apiClient from '../api/axios';
import { SignalConfirmationState } from '../types';

type Props = { signalId: string };

/**
 * Neighbour evidence and follow state for one report.
 *
 * The follow flag has three states, not two: until the server answers, "not following" is a guess,
 * and a failed lookup used to leave the button claiming the case was not followed. The button stays
 * disabled while loading and becomes a retry after an error.
 */
export function SignalTrustActions({ signalId }: Props) {
  const { t } = useTranslation();
  const [confirmation, setConfirmation] = useState<SignalConfirmationState | null>(null);
  const [following, setFollowing] = useState(false);
  const [followStatus, setFollowStatus] = useState<'loading' | 'known' | 'error'>('loading');
  const [busy, setBusy] = useState(false);

  const loadFollow = useCallback(async () => {
    setFollowStatus('loading');
    try {
      const { data } = await apiClient.get<{ following: boolean }>(`follows/SIGNAL/${signalId}`);
      setFollowing(Boolean(data.following));
      setFollowStatus('known');
    } catch {
      setFollowStatus('error');
    }
  }, [signalId]);

  useEffect(() => {
    let cancelled = false;
    apiClient
      .get<SignalConfirmationState>(`signals/${signalId}/confirmations`)
      .then(({ data }) => {
        if (!cancelled) setConfirmation(data);
      })
      .catch(() => {
        // The confirmation count is additive context; the follow state has its own retry.
      });
    void loadFollow();
    return () => {
      cancelled = true;
    };
  }, [signalId, loadFollow]);

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
      setFollowStatus('known');
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
        onClick={followStatus === 'error' ? () => void loadFollow() : toggleFollow}
        disabled={busy || followStatus === 'loading'}
        aria-pressed={followStatus === 'known' ? following : undefined}
        data-testid="signal-follow-button"
      >
        <i
          className={
            followStatus === 'error' ? 'pi pi-refresh' : following ? 'pi pi-check' : 'pi pi-bell'
          }
          aria-hidden="true"
        />
        {followStatus === 'error'
          ? t('community_feed.follow_retry')
          : following
            ? t('community_feed.following')
            : t('community_feed.follow')}
      </button>
    </div>
  );
}
