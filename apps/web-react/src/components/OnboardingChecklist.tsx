import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import apiClient from '../api/axios';

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
  const nextStep = steps.find((step) => !step.done);

  return (
    <div className="onboarding-checklist-compact" data-testid="onboarding-checklist">
      <span className="onboarding-checklist-progress">
        {t('onboarding_checklist.progress', { completed, total: steps.length })}
      </span>
      {nextStep && (
        <button
          type="button"
          className="onboarding-checklist-next"
          onClick={nextStep.action}
          data-testid={`onboarding-step-${nextStep.key}`}
        >
          {nextStep.label}
          <i className="pi pi-arrow-right" aria-hidden="true" />
        </button>
      )}
    </div>
  );
}
