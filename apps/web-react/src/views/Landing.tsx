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

/**
 * The public front door: the cities that already exist, how many people are in each, and a way in.
 *
 * <p>Deliberately outside the auth guard. Asking someone to register before they can see whether
 * their city has a community is asking them to commit to something they cannot evaluate. The
 * directory is read from the public endpoint so this page needs no session.
 *
 * <p>The page is also the product's one chance to be understood at a glance: a paper-cut hero that
 * shows a neighbourhood raising its signals, the two real ways in, how the loop works, and the
 * trust promises that back the ranking. Everything below the hero is still the directory, because
 * a stranger should be able to see their city before committing to it.
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

  const steps = [
    { icon: "pi-megaphone", title: t("landing.step_report_title"), desc: t("landing.step_report_desc") },
    { icon: "pi-chart-bar", title: t("landing.step_prioritise_title"), desc: t("landing.step_prioritise_desc") },
    { icon: "pi-building", title: t("landing.step_respond_title"), desc: t("landing.step_respond_desc") },
  ];

  const trustPoints = [
    t("landing.trust_point_evidence"),
    t("landing.trust_point_transparent"),
    t("landing.trust_point_human"),
  ];

  const scrollToDirectory = () => {
    if (typeof document === "undefined") {
      return;
    }
    document.getElementById("landing-directory")?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  return (
    <Layout authMode>
      <main className="landing-shell" data-testid="landing-page">
        <div className="flex justify-content-between align-items-center gap-3 flex-wrap mb-5">
          <div className="flex align-items-center gap-3">
            <div className="p-2 bg-surface-soft border-round-2xl border-1 border-subtle">
              <i className="pi pi-globe text-brand-primary text-2xl" aria-hidden="true"></i>
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

        <section className="landing-hero" data-testid="landing-hero" aria-labelledby="landing-hero-title">
          <div className="landing-hero-copy">
            <span className="landing-hero-eyebrow">
              <i className="pi pi-send" aria-hidden="true"></i>
              {t("landing.hero_eyebrow")}
            </span>
            <h1 id="landing-hero-title" className="landing-hero-title">
              {t("landing.hero_title")}
            </h1>
            <p className="landing-hero-subtitle">{t("landing.subtitle")}</p>
            <div className="landing-hero-actions">
              <CivicButton
                type="button"
                label={t("landing.primary_cta")}
                icon="pi pi-plus"
                glow
                onClick={() => navigate("/register?mode=create")}
                data-testid="landing-create-city"
              />
              <CivicButton
                type="button"
                label={t("landing.secondary_cta")}
                icon="pi pi-map-marker"
                variant="secondary"
                onClick={scrollToDirectory}
                data-testid="landing-find-city"
              />
            </div>
          </div>
          <div className="landing-hero-art">
            <img
              className="landing-hero-illustration"
              data-testid="landing-hero-illustration"
              src="/illustrations/city-hero.svg"
              alt={t("landing.hero_illustration_alt")}
              width={1600}
              height={1000}
            />
          </div>
        </section>

        <section className="landing-section landing-directory" id="landing-directory" data-testid="landing-directory">
          <CivicCard title={t("landing.cities_title")}>
            <div className="mb-1">
              <label className="u-eyebrow" htmlFor="landing-search-input">
                {t("landing.search_label")}
              </label>
            </div>
            <div className="landing-search">
              <i className="pi pi-search landing-search-icon" data-testid="landing-search-icon" aria-hidden="true" />
              <InputText
                id="landing-search-input"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder={t("landing.search_placeholder")}
                data-testid="landing-search-input"
              />
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
              <div
                className="flex flex-column align-items-center text-center py-6 px-3 animate-fade-up civic-empty-state"
                data-testid="landing-empty-state"
              >
                <img
                  className="state-illustration"
                  data-testid="landing-empty-illustration"
                  src="/illustrations/city-empty.svg"
                  alt={t("landing.empty_illustration_alt")}
                  width={1200}
                  height={1200}
                />
                <h3 className="text-2xl font-black text-main m-0 tracking-tighter mb-2 civic-empty-state-title">
                  {t("landing.empty_title")}
                </h3>
                <p className="text-secondary text-base font-medium max-w-24rem mx-auto leading-relaxed mb-5 civic-empty-state-description">
                  {t("landing.empty_desc")}
                </p>
                <CivicButton
                  type="button"
                  label={t("landing.create_city_cta")}
                  onClick={() => navigate("/register?mode=create")}
                  glow
                />
              </div>
            ) : (
              <div className="landing-city-grid" data-testid="landing-city-list">
                {visibleCities.map((city) => (
                  <article className="landing-city-card" key={city.id} data-testid={`landing-city-card-${city.id}`}>
                    <div className="flex justify-content-between align-items-start gap-2">
                      <div className="min-w-0">
                        <div className="landing-city-name">{city.name}</div>
                        <div className="landing-city-location">{locationLabel(city)}</div>
                      </div>
                      <CivicBadge
                        label={t("landing.member_count", { count: city.memberCount ?? 0 })}
                        type="category"
                        data-testid={`landing-city-members-${city.id}`}
                      />
                    </div>
                    {city.description && <p className="landing-city-desc">{city.description}</p>}
                    <div className="landing-city-cta">
                      <CivicButton
                        type="button"
                        label={t("landing.join_city")}
                        icon="pi pi-arrow-right"
                        className="w-full"
                        onClick={() => navigate(`/register?communityId=${city.id}`)}
                        data-testid={`landing-join-${city.id}`}
                      />
                    </div>
                  </article>
                ))}
              </div>
            )}
          </CivicCard>
        </section>

        <section className="landing-section" data-testid="landing-how-it-works" aria-labelledby="landing-how-title">
          <div className="landing-section-head">
            <h2 id="landing-how-title" className="landing-section-title">
              {t("landing.how_title")}
            </h2>
            <p className="landing-section-subtitle">{t("landing.how_subtitle")}</p>
          </div>
          <div className="landing-steps">
            {steps.map((step, index) => (
              <article className="landing-step" data-testid="landing-step" key={step.title}>
                <span className="landing-step-index" aria-hidden="true">
                  {index + 1}
                </span>
                <div className="landing-step-body">
                  <h3 className="landing-step-title">
                    <i className={`pi ${step.icon} mr-2 text-brand-primary`} aria-hidden="true"></i>
                    {step.title}
                  </h3>
                  <p className="landing-step-desc">{step.desc}</p>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="landing-trust-strip" data-testid="landing-trust-strip" aria-labelledby="landing-trust-title">
          <div>
            <h2 id="landing-trust-title" className="landing-trust-title">
              {t("landing.trust_title")}
            </h2>
            <p className="landing-trust-desc">{t("landing.trust_desc")}</p>
            <p className="text-xs text-muted mt-3 mb-0">
              <Link to="/backlog" className="text-brand-primary font-bold no-underline hover:underline">
                {t("public_backlog.title")}
              </Link>
            </p>
          </div>
          <ul className="landing-trust-points m-0 p-0 list-none">
            {trustPoints.map((point) => (
              <li className="landing-trust-point" key={point}>
                <i className="pi pi-check-circle" aria-hidden="true"></i>
                <span>{point}</span>
              </li>
            ))}
          </ul>
        </section>
      </main>
    </Layout>
  );
}
