import { useCallback, useEffect, useMemo, useState } from 'react';
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
import { languageForCountry, useSettingsStore } from '../store/useSettingsStore';
import { Community, CommunityMembership } from '../types';

type GeoState = 'idle' | 'locating' | 'found' | 'denied' | 'error';

export function Onboarding() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const markMembershipsStale = useCommunityStore((state) => state.markMembershipsStale);
  const setMemberships = useCommunityStore((state) => state.setMemberships);
  const setActiveCommunityId = useCommunityStore((state) => state.setActiveCommunityId);

  const [places, setPlaces] = useState<Community[]>([]);
  const [placesError, setPlacesError] = useState(false);
  const [placesLoading, setPlacesLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [joiningId, setJoiningId] = useState<string | null>(null);

  const [geoState, setGeoState] = useState<GeoState>('idle');
  const [nearest, setNearest] = useState<Community | null>(null);
  const [detected, setDetected] = useState<{
    city?: string;
    locality?: string;
    neighborhood?: string;
    country?: string;
    region?: string;
  } | null>(null);

  const [country, setCountry] = useState('Colombia');
  const [region, setRegion] = useState('');
  const [city, setCity] = useState('');
  const [locality, setLocality] = useState('');
  const [neighborhood, setNeighborhood] = useState('');
  const [creating, setCreating] = useState(false);

  const loadPlaces = useCallback(async () => {
    setPlacesLoading(true);
    setPlacesError(false);
    try {
      const { data } = await apiClient.get<Community[]>('communities/public');
      setPlaces(data ?? []);
    } catch {
      // The location path and the create form still work; the list offers a retry instead of
      // pretending there are no communities.
      setPlacesError(true);
    } finally {
      setPlacesLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadPlaces();
  }, [loadPlaces]);

  const placeRelevance = useCallback((place: Community) => {
    if (!detected) return 0;
    const pairs: Array<[string | null | undefined, string | null | undefined]> = [
      [place.neighborhood, detected.neighborhood],
      [place.locality, detected.locality],
      [place.city, detected.city],
      [place.region, detected.region],
      [place.country, detected.country],
    ];
    return pairs.reduce(
      (score, [left, right]) =>
        left && right && left.trim().toLowerCase() === right.trim().toLowerCase() ? score + 1 : score,
      0
    );
  }, [detected]);

  const filteredPlaces = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
      return detected
        ? [...places].sort((a, b) => placeRelevance(b) - placeRelevance(a))
        : places;
    }
    const matches = places.filter((place) =>
      [place.name, place.neighborhood, place.locality, place.city, place.region, place.country]
        .filter(Boolean)
        .some((value) => (value as string).toLowerCase().includes(normalized))
    );
    return detected ? matches.sort((a, b) => placeRelevance(b) - placeRelevance(a)) : matches;
  }, [places, query, detected, placeRelevance]);

  const finish = async (communityId?: string) => {
    try {
      const { data } = await apiClient.get<CommunityMembership[]>('communities/my');
      setMemberships(data ?? []);
      if (communityId && (data ?? []).some((membership) => membership.communityId === communityId)) {
        setActiveCommunityId(communityId);
      }
    } catch {
      // The layout will refresh memberships; the person still reaches the app.
    }
    markMembershipsStale();
    navigate('/');
  };

  const join = async (communityId: string) => {
    setJoiningId(communityId);
    try {
      await apiClient.post(`communities/${communityId}/join`, { role: 'MEMBER' });
      toast.success(t('onboarding.join_success'));
      await finish(communityId);
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
        const params = {
          latitude: position.coords.latitude,
          longitude: position.coords.longitude
        };
        try {
          const { data } = await apiClient.get<Community>('communities/nearest', { params });
          setNearest(data);
          setGeoState('found');
        } catch {
          setGeoState('error');
        }
        // OpenStreetMap resolves the barrio/locality for the create form; if it fails, the
        // manual list and the nearest city are still there.
        try {
          const { data } = await apiClient.get<typeof detected>('places/reverse', { params });
          if (data) {
            setDetected(data);
            if (data.city) setCity(data.city);
            if (data.locality) setLocality(data.locality);
            if (data.neighborhood) setNeighborhood(data.neighborhood);
            if (data.country) setCountry(data.country);
            if (data.region) setRegion(data.region);
            const countryLanguage = languageForCountry(data.country);
            const settings = useSettingsStore.getState();
            if (countryLanguage && !settings.languageExplicit) {
              settings.setDetectedLanguage(countryLanguage);
              i18n.changeLanguage(countryLanguage);
            }
          }
        } catch {
          // Keep the manual form.
        }
      },
      (error) => {
        setGeoState(error.code === error.PERMISSION_DENIED ? 'denied' : 'error');
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 600000 }
    );
  };

  // Take the person to their own city first: one attempt per tab. If permission is denied, the
  // manual list and the locate button are still there, so nothing is blocked.
  useEffect(() => {
    if (sessionStorage.getItem('onboarding-auto-locate') === '1') {
      return;
    }
    sessionStorage.setItem('onboarding-auto-locate', '1');
    locate();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const createPlace = async () => {
    if (!city.trim()) {
      toast.error(t('auth.city_name_required'));
      return;
    }
    setCreating(true);
    try {
      const { data } = await apiClient.post<Community>('communities', {
        name: (neighborhood || locality || city).trim(),
        slug: '',
        country: country.trim(),
        region: region.trim(),
        city: city.trim(),
        locality: locality.trim(),
        neighborhood: neighborhood.trim()
      });
      toast.success(t('onboarding.create_success'));
      await finish(data?.id);
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
                  data-testid={`onboarding-join-nearest-${nearest.id}`}
                />
                {detected && (
                  <div className="onboarding-hierarchy" data-testid="onboarding-detected-place">
                    {detected.neighborhood && (
                      <span>
                        <small>{t('onboarding.neighborhood')}</small>
                        {detected.neighborhood}
                      </span>
                    )}
                    {detected.locality && (
                      <span>
                        <small>{t('onboarding.locality')}</small>
                        {detected.locality}
                      </span>
                    )}
                    {detected.city && (
                      <span>
                        <small>{t('onboarding.municipality')}</small>
                        {detected.city}
                      </span>
                    )}
                    {detected.region && (
                      <span>
                        <small>{t('onboarding.department')}</small>
                        {detected.region}
                      </span>
                    )}
                    {detected.country && (
                      <span>
                        <small>{t('onboarding.country')}</small>
                        {detected.country}
                      </span>
                    )}
                  </div>
                )}
              </div>
            )}
          </CivicCard>

          <CivicCard padding="lg" className="onboarding-card">
            <h2>{t('onboarding.cities_title')}</h2>
            <p className="text-secondary text-sm">{t('onboarding.cities_desc')}</p>
            <label className="onboarding-search-label" htmlFor="onboarding-city-search">
              {t('onboarding.search_label')}
            </label>
            <span className="p-input-icon-left w-full mb-3">
              <i className="pi pi-search text-muted" />
              <InputText
                id="onboarding-city-search"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder={t('onboarding.search_placeholder')}
                className="w-full pl-5"
                data-testid="onboarding-city-search"
              />
            </span>
            {placesLoading ? (
              <p className="text-sm text-muted" role="status">{t('common.loading')}</p>
            ) : placesError ? (
              <div className="onboarding-places-error" role="alert" data-testid="onboarding-places-error">
                <span>{t('onboarding.places_error')}</span>
                <CivicButton
                  type="button"
                  label={t('common.retry')}
                  variant="secondary"
                  size="small"
                  onClick={() => void loadPlaces()}
                />
              </div>
            ) : (
              <ul className="onboarding-city-list">
              {filteredPlaces.map((place) => (
                <li key={place.id}>
                  <div>
                    <strong>{place.name}</strong>
                    {placeRelevance(place) > 0 && (
                      <span className="u-pill onboarding-near-badge">{t('onboarding.near_you')}</span>
                    )}
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
            )}
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
