import { Suspense, lazy, useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import { useAuthStore } from "./store/useAuthStore";
import { useSettingsStore } from "./store/useSettingsStore";
import { useCommunityStore } from "./store/useCommunityStore";
import { AuthGuard } from "./components/AuthGuard";
import { ChunkErrorBoundary } from "./components/ChunkErrorBoundary";

// P2-16: Code-splitting
const Dashboard = lazy(() => import("./views/Dashboard").then(m => ({ default: m.Dashboard })));
const ReportSignal = lazy(() => import("./views/ReportSignal").then(m => ({ default: m.ReportSignal })));
const SignalDetail = lazy(() => import("./views/SignalDetail").then(m => ({ default: m.SignalDetail })));
const MySignals = lazy(() => import("./views/MySignals").then(m => ({ default: m.MySignals })));
const Settings = lazy(() => import("./views/Settings").then(m => ({ default: m.Settings })));
const HelpCenter = lazy(() => import("./views/HelpCenter").then(m => ({ default: m.HelpCenter })));
const Register = lazy(() => import("./views/Register").then(m => ({ default: m.Register })));
const Verify = lazy(() => import("./views/Verify").then(m => ({ default: m.Verify })));
const PublicBacklog = lazy(() => import("./views/PublicBacklog").then(m => ({ default: m.PublicBacklog })));
const CommunityMergeReview = lazy(() => import("./views/CommunityMergeReview").then(m => ({ default: m.CommunityMergeReview })));
const WeeklyDigestView = lazy(() => import("./views/WeeklyDigestView").then(m => ({ default: m.WeeklyDigestView })));
const Login = lazy(() => import("./views/Login").then(m => ({ default: m.Login })));
const Moderation = lazy(() => import("./views/Moderation").then(m => ({ default: m.Moderation })));
const Communities = lazy(() => import("./views/Communities").then(m => ({ default: m.Communities })));
const Landing = lazy(() => import("./views/Landing").then(m => ({ default: m.Landing })));
const Onboarding = lazy(() => import("./views/Onboarding").then(m => ({ default: m.Onboarding })));
const CommunityThreads = lazy(() => import("./views/CommunityThreads").then(m => ({ default: m.CommunityThreads })));
const CommunityBlog = lazy(() => import("./views/CommunityBlog").then(m => ({ default: m.CommunityBlog })));
const CommunityFeed = lazy(() => import("./views/CommunityFeed").then(m => ({ default: m.CommunityFeed })));
const CommunityMap = lazy(() => import("./views/CommunityMap").then(m => ({ default: m.CommunityMap })));
const CommunityProposals = lazy(() => import("./views/CommunityProposals").then(m => ({ default: m.CommunityProposals })));
const CommunityDecisions = lazy(() => import("./views/CommunityDecisions").then(m => ({ default: m.CommunityDecisions })));
const CommunityProjects = lazy(() => import("./views/CommunityProjects").then(m => ({ default: m.CommunityProjects })));
const CommunityGovernance = lazy(() => import("./views/CommunityGovernance").then(m => ({ default: m.CommunityGovernance })));
const CommunityTrustMetrics = lazy(() => import("./views/CommunityTrustMetrics").then(m => ({ default: m.CommunityTrustMetrics })));
const CommunityOpenData = lazy(() => import("./views/CommunityOpenData").then(m => ({ default: m.CommunityOpenData })));
const CommunityRooms = lazy(() => import("./views/CommunityRooms").then(m => ({ default: m.CommunityRooms })));
const CommunityActivities = lazy(() => import("./views/CommunityActivities").then(m => ({ default: m.CommunityActivities })));
const CommunityResources = lazy(() => import("./views/CommunityResources").then(m => ({ default: m.CommunityResources })));
const CommunityIntegrations = lazy(() => import("./views/CommunityIntegrations").then(m => ({ default: m.CommunityIntegrations })));
const NotFound = lazy(() => import("./views/NotFound").then(m => ({ default: m.NotFound })));
const Unauthorized = lazy(() => import("./views/Unauthorized").then(m => ({ default: m.Unauthorized })));

const LoadingFallback = () => (
  <div className="app-loading" role="status" aria-live="polite" data-testid="app-loading">
    <div className="app-loading-brand">
      <span className="app-loading-dot" aria-hidden="true" />
      <span>Open Civic Signal OS</span>
    </div>
    <div className="app-loading-track" aria-hidden="true">
      <span />
    </div>
    <span className="sr-only">Loading…</span>
  </div>
);

/**
 * The root decides between the community feed and the public front door.
 *
 * <p>A logged-in visitor lands on the one feed where reports, announcements and conversations live:
 * reading, opining and reporting are the same route. The analytics dashboard moved to "/dashboard"
 * for staff and advanced users, so the citizen no longer has to choose between two homes.
 */
function HomeRoute() {
  const { isLoggedIn, isHydrated } = useAuthStore();
  const { memberships, membershipsLoadedAt, setMemberships } = useCommunityStore();

  // The layout also refreshes memberships, but the root has to know whether this is a new account
  // before it can decide between the feed and the location onboarding. Waiting for the first load
  // avoids sending an existing member to onboarding just because the store is still empty.
  useEffect(() => {
    if (!isLoggedIn || membershipsLoadedAt !== null) return;
    let cancelled = false;
    import("./api/axios")
      .then(({ default: api }) => api.get("communities/my"))
      .then(({ data }) => {
        if (!cancelled) setMemberships(data ?? []);
      })
      .catch(() => {
        if (!cancelled) setMemberships([]);
      });
    return () => {
      cancelled = true;
    };
  }, [isLoggedIn, membershipsLoadedAt, setMemberships]);

  if (!isHydrated || (isLoggedIn && membershipsLoadedAt === null)) {
    return <LoadingFallback />;
  }
  if (isLoggedIn && memberships.length === 0) {
    return <Navigate to="/onboarding" replace />;
  }
  return isLoggedIn ? <CommunityFeed /> : <Landing />;
}

export function App() {
  const { isLoggedIn } = useAuthStore();
  const { theme, language, textScale, interfaceMode, setInterfaceMode } = useSettingsStore();

  useEffect(() => {
    const html = document.documentElement;
    html.classList.remove('light-theme', 'dark-theme', 'contrast-theme');
    html.classList.add(`${theme}-theme`);
    // Mirrored as a data attribute so the CSS supports [data-theme] as well as
    // the legacy classes; dropping both makes prefers-color-scheme decide.
    html.dataset.theme = theme;
  }, [theme]);

  // The lang attribute drives screen-reader pronunciation; it must follow the language the
  // person actually sees, not the one the browser was installed with.
  useEffect(() => {
    document.documentElement.lang = language;
  }, [language]);

  // The comfortable-reading switch changes the root font size; every rem-based size follows.
  useEffect(() => {
    document.documentElement.dataset.textScale = textScale;
  }, [textScale]);

  // Warm the chunks behind the actions a reader is most likely to take next, so the first click on
  // Reportar, a case or Mi Actividad does not wait for a network round trip. Public visitors do not
  // need authenticated views.
  useEffect(() => {
    if (!isLoggedIn) return;
    const timer = window.setTimeout(() => {
      void import("./views/ReportSignal");
      void import("./views/SignalDetail");
      void import("./views/MySignals");
      void import("./views/CommunityRooms");
    }, 1500);
    return () => window.clearTimeout(timer);
  }, [isLoggedIn]);

  useEffect(() => {
    const html = document.documentElement;
    html.classList.remove('interface-mode-simple', 'interface-mode-advanced');
    html.classList.add(`interface-mode-${interfaceMode}`);
  }, [interfaceMode]);

  useEffect(() => {
    let cancelled = false;
    const syncInterfaceMode = async () => {
      if (!isLoggedIn) {
        return;
      }
      try {
        const module = await import("./api/axios");
        const response = await module.default.get("auth/me");
        const backendMode = response.data?.interfaceMode;
        if (!cancelled && (backendMode === "SIMPLE" || backendMode === "ADVANCED")) {
          setInterfaceMode(backendMode.toLowerCase());
        }
      } catch {
        // Keep local preference if server sync is unavailable.
      }
    };
    syncInterfaceMode();
    return () => {
      cancelled = true;
    };
  }, [isLoggedIn, setInterfaceMode]);

  return (
    <BrowserRouter>
      <Toaster 
        position="bottom-right"
        containerClassName="app-toaster"
        containerStyle={{ zIndex: 99999 }}
        gutter={12}
        toastOptions={{
          duration: 5000,
          className: 'glass-panel',
          style: {
            background: 'var(--bg-nav)',
            backdropFilter: 'blur(20px)',
            border: '1px solid var(--border-subtle)',
            color: 'var(--text-main)',
            borderRadius: 'var(--radius-paper-sm)',
            padding: '16px 24px',
            fontSize: '14px',
            fontWeight: '600',
            boxShadow: 'var(--shadow-edge), var(--shadow-premium)',
            maxWidth: '400px',
          },
          success: {
            iconTheme: {
              primary: 'var(--status-resolved)',
              secondary: '#fff',
            },
            style: {
              borderLeft: '4px solid var(--status-resolved)',
            }
          },
          error: {
            iconTheme: {
              primary: 'var(--status-rejected)',
              secondary: '#fff',
            },
            style: {
              borderLeft: '4px solid var(--status-rejected)',
            }
          },
        }}
      />
      <ChunkErrorBoundary>
        <Suspense fallback={<LoadingFallback />}>
          <Routes>
            <Route path="/login" element={isLoggedIn ? <Navigate to="/" /> : <Login />} />
            <Route path="/register" element={isLoggedIn ? <Navigate to="/" /> : <Register />} />
            <Route path="/verify" element={<Verify />} />
            {/*
              The backlog is readable without an account. The API already serves it publicly, so
              requiring a login to look at it asked people to register before they could see
              whether registering was worth it. Outside the AuthGuard on purpose.
            */}
            <Route path="/backlog" element={<PublicBacklog />} />
            <Route path="/" element={<HomeRoute />} />
            
            <Route element={<AuthGuard />}>
              <Route path="/onboarding" element={<Onboarding />} />
              <Route path="/report" element={<ReportSignal />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/signal/:id" element={<SignalDetail />} />
              <Route path="/mine" element={<MySignals />} />
              <Route path="/settings" element={<Settings />} />
              <Route path="/help" element={<HelpCenter />} />
              <Route path="/communities" element={<Communities />} />
              <Route path="/communities/threads" element={<CommunityThreads />} />
              <Route path="/communities/blog" element={<CommunityBlog />} />
              <Route path="/communities/feed" element={<CommunityFeed />} />
              <Route path="/communities/map" element={<CommunityMap />} />
              <Route path="/communities/proposals" element={<CommunityProposals />} />
              <Route path="/communities/decisions" element={<CommunityDecisions />} />
              <Route path="/communities/projects" element={<CommunityProjects />} />
              <Route path="/communities/governance" element={<CommunityGovernance />} />
              <Route path="/communities/trust" element={<CommunityTrustMetrics />} />
              <Route path="/communities/merge-review" element={<CommunityMergeReview />} />
              <Route path="/communities/weekly-digest" element={<WeeklyDigestView />} />
              <Route path="/communities/open-data" element={<CommunityOpenData />} />
  <Route path="/communities/rooms" element={<CommunityRooms />} />
  <Route path="/communities/activities" element={<CommunityActivities />} />
  <Route path="/communities/resources" element={<CommunityResources />} />
  <Route path="/communities/integrations" element={<CommunityIntegrations />} />
            </Route>

            <Route element={<AuthGuard />}>
              <Route path="/moderation" element={<Moderation />} />
            </Route>
            
            <Route path="/unauthorized" element={<Unauthorized />} />
            <Route path="/forbidden" element={<Unauthorized />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Suspense>
      </ChunkErrorBoundary>
    </BrowserRouter>
  );
}
