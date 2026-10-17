import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { InputText } from "primereact/inputtext";
import { useTranslation } from "react-i18next";
import { Layout } from "../components/Layout";
import apiClient from "../api/axios";
import { Community } from "../types";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";

/**
 * The public front door: the cities that already exist, how many people are in each, and a way in.
 *
 * <p>Deliberately outside the auth guard. Asking someone to register before they can see whether
 * their city has a community is asking them to commit to something they cannot evaluate. The
 * directory is read from the public endpoint so this page needs no session.
 */
export function Landing() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [cities, setCities] = useState<Community[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [query, setQuery] = useState("");

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const response = await apiClient.get<Community[]>("communities/public");
        if (!cancelled) {
          setCities(response.data || []);
        }
      } catch {
        if (!cancelled) {
          setLoadFailed(true);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const visibleCities = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
      return cities;
    }
    return cities.filter((city) =>
      [city.name, city.country, city.region]
        .filter(Boolean)
        .some((value) => (value as string).toLowerCase().includes(normalized))
    );
  }, [cities, query]);

  const locationLabel = (city: Community) => {
    const parts = [city.region, city.country].filter((value) => value && value.trim().length > 0);
    return parts.length > 0 ? parts.join(", ") : t("landing.location_unknown");
  };

  return (
    <Layout authMode>
      <div className="min-h-screen p-4 md:p-6" data-testid="landing-page">
        <div className="mx-auto w-full" style={{ maxWidth: "72rem" }}>
          <div className="flex justify-content-between align-items-center gap-3 flex-wrap mb-6">
            <div className="flex align-items-center gap-3">
              <div className="p-2 bg-surface-soft border-round-2xl border-1 border-subtle">
                <i className="pi pi-globe text-brand-primary text-2xl"></i>
              </div>
              <span className="font-black text-main text-xl tracking-tight">Open Civic Signal OS</span>
            </div>
            <div className="flex gap-2 flex-wrap">
              <CivicButton
                type="button"
                label={t("landing.login_cta")}
                variant="ghost"
                onClick={() => navigate("/login")}
                data-testid="landing-login-cta"
              />
              <CivicButton
                type="button"
                label={t("landing.register_cta")}
                icon="pi pi-user-plus"
                glow
                onClick={() => navigate("/register")}
                data-testid="landing-register-cta"
              />
            </div>
          </div>

          <div className="text-center mb-8">
            <h1 className="text-4xl md:text-6xl font-black text-main m-0 tracking-tighter">
              {t("landing.title")}
            </h1>
            <p className="text-secondary mt-3 font-medium max-w-42rem mx-auto line-height-3">
              {t("landing.subtitle")}
            </p>
            <div className="flex justify-content-center gap-3 flex-wrap mt-5">
              <CivicButton
                type="button"
                label={t("landing.create_city_cta")}
                icon="pi pi-plus"
                variant="secondary"
                onClick={() => navigate("/register?mode=create")}
                data-testid="landing-create-city"
              />
            </div>
          </div>

          <CivicCard title={t("landing.cities_title")} className="mb-6">
            <div className="mb-4">
              <span className="p-input-icon-left w-full">
                <i className="pi pi-search" />
                <InputText
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder={t("landing.search_placeholder")}
                  className="w-full"
                  data-testid="landing-search-input"
                />
              </span>
            </div>

            {loading ? (
              <p className="text-secondary text-sm m-0" data-testid="landing-loading">
                {t("common.loading")}
              </p>
            ) : loadFailed ? (
              <p className="text-secondary text-sm m-0" data-testid="landing-load-error">
                {t("landing.load_error")}
              </p>
            ) : visibleCities.length === 0 ? (
              <CivicEmptyState
                icon="pi-map-marker"
                title={t("landing.empty_title")}
                description={t("landing.empty_desc")}
                actionLabel={t("landing.create_city_cta")}
                onAction={() => navigate("/register?mode=create")}
                data-testid="landing-empty-state"
              />
            ) : (
              <div className="grid" data-testid="landing-city-list">
                {visibleCities.map((city) => (
                  <div className="col-12 md:col-6 lg:col-4" key={city.id}>
                    <div
                      className="glass-panel border-round-3xl border-1 border-subtle p-4 h-full flex flex-column gap-3"
                      data-testid={`landing-city-card-${city.id}`}
                    >
                      <div className="flex justify-content-between align-items-start gap-2">
                        <div className="min-w-0">
                          <div className="font-black text-main text-lg">{city.name}</div>
                          <div className="text-xs text-muted mt-1">{locationLabel(city)}</div>
                        </div>
                        <CivicBadge
                          label={t("landing.member_count", { count: city.memberCount ?? 0 })}
                          type="category"
                          data-testid={`landing-city-members-${city.id}`}
                        />
                      </div>
                      {city.description && (
                        <p className="text-sm text-secondary m-0 line-height-3">{city.description}</p>
                      )}
                      <div className="mt-auto">
                        <CivicButton
                          type="button"
                          label={t("landing.join_city")}
                          icon="pi pi-arrow-right"
                          className="w-full"
                          onClick={() => navigate(`/register?communityId=${city.id}`)}
                          data-testid={`landing-join-${city.id}`}
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CivicCard>

          <CivicCard variant="brand">
            <div className="flex flex-column gap-2">
              <div className="font-black text-main">{t("landing.why_title")}</div>
              <p className="text-secondary text-sm m-0 line-height-3">{t("landing.why_desc")}</p>
              <p className="text-muted text-xs m-0">
                <Link to="/backlog" className="text-brand-primary font-bold no-underline hover:underline">
                  {t("public_backlog.title")}
                </Link>
              </p>
            </div>
          </CivicCard>
        </div>
      </div>
    </Layout>
  );
}
