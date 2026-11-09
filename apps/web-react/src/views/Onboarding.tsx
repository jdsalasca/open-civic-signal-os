import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { InputText } from 'primereact/inputtext';
import { useTranslation } from 'react-i18next';
import apiClient from '../api/axios';
import { Layout } from '../components/Layout';
import { CivicButton } from '../components/ui/CivicButton';
import { CivicCard } from '../components/ui/CivicCard';
import { CivicField } from '../components/ui/CivicField';
import { useCommunityStore } from '../store/useCommunityStore';
import { Community } from '../types';

type GeoState = 'idle' | 'locating' | 'found' | 'denied' | 'error';

export function Onboarding() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const markMembershipsStale = useCommunityStore((state) => state.markMembershipsStale);

  const [places, setPlaces] = useState<Community[]>([]);
  const [query, setQuery] = useState('');
  const [joiningId, setJoiningId] = useState<string | null>(null);

  const [geoState, setGeoState] = useState<GeoState>('idle');
  const [nearest, setNearest] = useState<Community | null>(null);

  const [country, setCountry] = useState('Colombia');
  const [region, setRegion] = useState('');
  const [city, setCity] = useState('');
  const [locality, setLocality] = useState('');
  const [neighborhood, setNeighborhood] = useState('');
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    let cancelled = false;
    apiClient
      .get<Community[]>('communities/public')
      .then(({ data }) => {
        if (!cancelled) {
          setPlaces(data ?? []);
        }
      })
      .catch(() => {
        // The manual list is optional; the location and create paths still work.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const filteredPlaces = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
      return places;
    }
    return places.filter((place) =>
      [place.name, place.neighborhood, place.locality, place.city, place.region, place.country]
        .filter(Boolean)
        .some((value) => (value as string).toLowerCase().includes(normalized))
    );
  }, [places, query]);

  const finish = () => {
    markMembershipsStale();
    navigate('/');
  };

  const join = async (communityId: string) => {
    setJoiningId(communityId);
    try {
      await apiClient.post(`communities/${communityId}/join`, { role: 'MEMBER' });
      toast.success(t('onboarding.join_success'));
      finish();
    } catch (err) {
      const apiErr = err as { friendlyMessage?: string };
      toast.error(apiErr.friendlyMessage || t('onboarding.join_error'));
    } finally {
      setJoiningId(null);
    }
  };

  const locate = () => {
    if (!navigator.geolocation) {
      setGeoState('error');
      return;
    }
    setGeoState('locating');
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          const { data } = await apiClient.get<Community>('communities/nearest', {
            params: {
              latitude: position.coords.latitude,
              longitude: position.coords.longitude
            }
          });
          setNearest(data);
          setGeoState('found');
        } catch {
          setGeoState('error');
        }
      },
      (error) => {
        setGeoState(error.code === error.PERMISSION_DENIED ? 'denied' : 'error');
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 600000 }
    );
  };

  const createPlace = async () => {
    if (!city.trim()) {
      toast.error(t('auth.city_name_required'));
      return;
    }
    setCreating(true);
    try {
      await apiClient.post('communities', {
        name: (neighborhood || locality || city).trim(),
        slug: '',
        country: country.trim(),
        region: region.trim(),
        city: city.trim(),
        locality: locality.trim(),
        neighborhood: neighborhood.trim()
      });
      toast.success(t('onboarding.create_success'));
      finish();
    } catch (err) {
      const apiErr = err as { friendlyMessage?: string };
      toast.error(apiErr.friendlyMessage || t('onboarding.create_error'));
    } finally {
      setCreating(false);
    }
  };

  return (
    <Layout>
      <div className="onboarding-page">
        <header className="onboarding-hero">
          <span className="u-eyebrow">{t('onboarding.eyebrow')}</span>
          <h1>{t('onboarding.title')}</h1>
          <p>{t('onboarding.subtitle')}</p>
        </header>

        <div className="onboarding-grid">
          <CivicCard padding="lg" className="onboarding-card">
            <h2>{t('onboarding.location_title')}</h2>
            <p className="text-secondary text-sm">{t('onboarding.location_desc')}</p>
            <CivicButton
              type="button"
              label={t('onboarding.location_button')}
              icon="pi pi-map-marker"
              loading={geoState === 'locating'}
              onClick={locate}
              data-testid="onboarding-locate-button"
            />
            {geoState === 'denied' && (
              <p className="text-sm text-muted mt-3" data-testid="onboarding-location-denied">
                {t('onboarding.location_denied')}
              </p>
            )}
            {geoState === 'error' && (
              <p className="text-sm text-muted mt-3">{t('onboarding.location_error')}</p>
            )}
            {geoState === 'found' && nearest && (
              <div className="onboarding-nearest" data-testid="onboarding-nearest">
                <span className="u-eyebrow">{t('onboarding.location_nearest')}</span>
                <strong>{nearest.name}</strong>
                <span className="text-muted text-sm">
                  {[nearest.region, nearest.country].filter(Boolean).join(', ')}
                </span>
                <CivicButton
                  type="button"
                  label={t('onboarding.join_city', { name: nearest.name })}
                  onClick={() => join(nearest.id)}
                  loading={joiningId === nearest.id}
                />
              </div>
            )}
          </CivicCard>

          <CivicCard padding="lg" className="onboarding-card">
            <h2>{t('onboarding.cities_title')}</h2>
            <p className="text-secondary text-sm">{t('onboarding.cities_desc')}</p>
            <span className="p-input-icon-left w-full mb-3">
              <i className="pi pi-search text-muted" />
              <InputText
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder={t('onboarding.search_placeholder')}
                className="w-full pl-5"
                data-testid="onboarding-city-search"
              />
            </span>
            <ul className="onboarding-city-list">
              {filteredPlaces.map((place) => (
                <li key={place.id}>
                  <div>
                    <strong>{place.name}</strong>
                    <span className="text-muted text-xs">
                      {[place.locality, place.city, place.region, place.country]
                        .filter(Boolean)
                        .join(' · ')}
                    </span>
                  </div>
                  <CivicButton
                    type="button"
                    label={t('onboarding.join')}
                    variant="secondary"
                    size="small"
                    loading={joiningId === place.id}
                    onClick={() => join(place.id)}
                    data-testid={`onboarding-join-${place.id}`}
                  />
                </li>
              ))}
              {filteredPlaces.length === 0 && (
                <li className="text-muted text-sm">{t('onboarding.no_results')}</li>
              )}
            </ul>
          </CivicCard>
        </div>

        <CivicCard padding="lg" className="onboarding-card onboarding-create">
          <h2>{t('onboarding.create_title')}</h2>
          <p className="text-secondary text-sm">{t('onboarding.create_desc')}</p>
          <div className="onboarding-form">
            <CivicField label={t('auth.city_name_label')}>
              <InputText
                value={city}
                onChange={(event) => setCity(event.target.value)}
                placeholder={t('auth.city_name_placeholder')}
                className="w-full"
                data-testid="onboarding-city-input"
              />
            </CivicField>
            <CivicField label={t('auth.city_locality_label')}>
              <InputText
                value={locality}
                onChange={(event) => setLocality(event.target.value)}
                placeholder={t('auth.city_locality_placeholder')}
                className="w-full"
                data-testid="onboarding-locality-input"
              />
            </CivicField>
            <CivicField label={t('auth.city_neighborhood_label')}>
              <InputText
                value={neighborhood}
                onChange={(event) => setNeighborhood(event.target.value)}
                placeholder={t('auth.city_neighborhood_placeholder')}
                className="w-full"
                data-testid="onboarding-neighborhood-input"
              />
            </CivicField>
            <CivicField label={t('auth.city_country_label')}>
              <InputText
                value={country}
                onChange={(event) => setCountry(event.target.value)}
                placeholder={t('auth.city_country_placeholder')}
                className="w-full"
                data-testid="onboarding-country-input"
              />
            </CivicField>
            <CivicField label={t('auth.city_region_label')}>
              <InputText
                value={region}
                onChange={(event) => setRegion(event.target.value)}
                placeholder={t('auth.city_region_placeholder')}
                className="w-full"
                data-testid="onboarding-region-input"
              />
            </CivicField>
          </div>
          <p className="text-xs text-muted">{t('auth.city_place_hint')}</p>
          <CivicButton
            type="button"
            label={t('onboarding.create_button')}
            icon="pi pi-plus"
            loading={creating}
            onClick={createPlace}
            data-testid="onboarding-create-button"
          />
        </CivicCard>
      </div>
    </Layout>
  );
}
