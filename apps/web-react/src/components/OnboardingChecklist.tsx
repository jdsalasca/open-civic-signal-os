import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import apiClient from '../api/axios';
import { CivicCard } from '../components/ui/CivicCard';

type Checklist = {
  joinedCommunity: boolean;
  contributed: boolean;
  followingContent: boolean;
  allComplete: boolean;
};

/** Three concrete first actions; disappears once all three are true. */
export function OnboardingChecklist() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [checklist, setChecklist] = useState<Checklist | null>(null);

  useEffect(() => {
    let cancelled = false;
    apiClient
      .get<Checklist>('onboarding/checklist')
      .then(({ data }) => {
        if (!cancelled) setChecklist(data);
      })
      .catch(() => {
        if (!cancelled) setChecklist(null);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (!checklist || checklist.allComplete) {
    return null;
  }

  const steps = [
    {
      key: 'joinedCommunity',
      done: checklist.joinedCommunity,
      label: t('onboarding_checklist.join'),
      action: () => navigate('/communities')
    },
    {
      key: 'contributed',
      done: checklist.contributed,
      label: t('onboarding_checklist.contribute'),
      action: () => navigate('/report')
    },
    {
      key: 'followingContent',
      done: checklist.followingContent,
      label: t('onboarding_checklist.follow'),
      action: () => navigate('/communities/feed')
    }
  ];
  const completed = steps.filter((step) => step.done).length;

  return (
    <CivicCard
      title={t('onboarding_checklist.title')}
      className="mb-6"
      data-testid="onboarding-checklist"
    >
      <p className="text-sm text-secondary m-0 mb-3">
        {t('onboarding_checklist.progress', { completed, total: steps.length })}
      </p>
      <div className="onboarding-checklist-list">
        {steps.map((step) => (
          <button
            key={step.key}
            type="button"
            className={step.done ? 'onboarding-checklist-step is-done' : 'onboarding-checklist-step'}
            onClick={step.action}
            disabled={step.done}
            data-testid={`onboarding-step-${step.key}`}
          >
            <i className={step.done ? 'pi pi-check-circle' : 'pi pi-circle'} aria-hidden="true" />
            <span>{step.label}</span>
          </button>
        ))}
      </div>
    </CivicCard>
  );
}
