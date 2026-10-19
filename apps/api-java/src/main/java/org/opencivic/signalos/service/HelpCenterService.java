package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.HelpAudience;
import org.opencivic.signalos.domain.HelpSurface;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.HelpCenterResponse;
import org.opencivic.signalos.web.dto.HelpCenterStateResponse;
import org.opencivic.signalos.web.dto.HelpGuideResponse;
import org.opencivic.signalos.web.dto.OnboardingStepResponse;
import org.opencivic.signalos.web.dto.UpdateHelpCenterStateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HelpCenterService {
    private static final List<GuideDefinition> GUIDE_DEFINITIONS = List.of(
        guide(
            "faq-dashboard-overview",
            "ARTICLE",
            HelpSurface.GENERAL,
            List.of(HelpAudience.CITIZEN, HelpAudience.MODERATOR, HelpAudience.REPRESENTATIVE),
            localized("Understand your community home", "Entienda su hogar comunitario"),
            localized("Learn what each core route does before opening extra tools.", "Aprenda que hace cada ruta principal antes de abrir mas herramientas."),
            localized(
                "The dashboard highlights one next action, your active community, and the shortest path into reporting, collaboration, or follow-up.",
                "El tablero resalta una siguiente accion, su comunidad activa y la ruta mas corta hacia reporte, colaboracion o seguimiento."
            ),
            List.of("dashboard", "community", "home", "orientation"),
            localized("Open dashboard", "Abrir tablero"),
            "/",
            false
        ),
        guide(
            "dashboard-start-here",
            "CONTEXTUAL",
            HelpSurface.DASHBOARD,
            List.of(HelpAudience.CITIZEN, HelpAudience.MODERATOR, HelpAudience.REPRESENTATIVE),
            localized("Start with one guided action", "Empiece con una accion guiada"),
            localized("Use the dashboard to choose one next civic action before opening extra tools.", "Use el tablero para elegir una siguiente accion civica antes de abrir mas herramientas."),
            localized(
                "The dashboard is the shortest path into reporting, community coordination, and follow-up. Start there when you need orientation instead of opening multiple routes at once.",
                "El tablero es la ruta mas corta hacia reporte, coordinacion comunitaria y seguimiento. Empiece alli cuando necesite orientacion en lugar de abrir varias rutas a la vez."
            ),
            List.of("dashboard", "next step", "orientation"),
            localized("Open dashboard", "Abrir tablero"),
            "/",
            true
        ),
        guide(
            "report-good-evidence",
            "CONTEXTUAL",
            HelpSurface.REPORT,
            List.of(HelpAudience.CITIZEN, HelpAudience.MODERATOR, HelpAudience.REPRESENTATIVE),
            localized("Report clearly and with evidence", "Reporte con claridad y evidencia"),
            localized("Use plain language, one recognizable location, and only the evidence you can verify.", "Use lenguaje claro, una ubicacion reconocible y solo la evidencia que pueda verificar."),
            localized(
                "A strong report explains what happened, where it happened, how many people are affected, and why the community needs action now.",
                "Un buen reporte explica que paso, donde paso, cuantas personas estan afectadas y por que la comunidad necesita accion ahora."
            ),
            List.of("report", "evidence", "wizard", "intake"),
            localized("Open report flow", "Abrir reporte"),
            "/report",
            true
        ),
        guide(
            "communities-context-switching",
            "CONTEXTUAL",
            HelpSurface.COMMUNITIES,
            List.of(HelpAudience.CITIZEN, HelpAudience.MODERATOR, HelpAudience.REPRESENTATIVE),
            localized("Keep one community context active", "Mantenga una comunidad activa"),
            localized("Switching community changes the governance, privacy, and collaboration rules you see.", "Cambiar de comunidad modifica las reglas de gobernanza, privacidad y colaboracion que usted ve."),
            localized(
                "Before you post, vote, or publish, confirm the active community so your actions stay inside the right civic space.",
                "Antes de publicar, votar o anunciar, confirme la comunidad activa para que sus acciones queden dentro del espacio civico correcto."
            ),
            List.of("communities", "context", "membership"),
            localized("Open communities hub", "Abrir comunidades"),
            "/communities",
            true
        ),
        guide(
            "proposal-decision-rules",
            "CONTEXTUAL",
            HelpSurface.PROPOSALS,
            List.of(HelpAudience.CITIZEN, HelpAudience.MODERATOR, HelpAudience.REPRESENTATIVE),
            localized("Write proposals that can become decisions", "Escriba propuestas que puedan convertirse en decisiones"),
            localized("Structure the problem, solution, cost, and beneficiaries so the community can vote and audit fairly.", "Estructure problema, solucion, costo y beneficiarios para que la comunidad pueda votar y auditar con justicia."),
            localized(
                "Proposal templates are not paperwork. They create the minimum public record needed for deliberation, voting, and decision follow-through.",
                "Las plantillas de propuesta no son papeleo. Crean el registro publico minimo necesario para deliberar, votar y dar seguimiento a la decision."
            ),
            List.of("proposals", "deliberation", "voting"),
            localized("Open proposals", "Abrir propuestas"),
            "/communities/proposals",
            true
        ),
        guide(
            "governance-library-basics",
            "CONTEXTUAL",
            HelpSurface.GOVERNANCE,
            List.of(HelpAudience.MODERATOR, HelpAudience.REPRESENTATIVE),
            localized("Ground decisions in governance records", "Fundamente decisiones en registros de gobernanza"),
            localized("Use the governance library before escalating conflicts or publishing official direction.", "Use la biblioteca de gobernanza antes de escalar conflictos o publicar direccion oficial."),
            localized(
                "Minutes, statutes, agreements, and reports provide the policy basis behind community actions. Link them when decisions or updates need public proof.",
                "Actas, estatutos, acuerdos e informes aportan la base de politica detras de las acciones comunitarias. Vinculelos cuando decisiones o actualizaciones necesiten prueba publica."
            ),
            List.of("governance", "documents", "policy"),
            localized("Open governance library", "Abrir biblioteca de gobernanza"),
            "/communities/governance",
            true
        ),
        guide(
            "projects-execution-follow-through",
            "CONTEXTUAL",
            HelpSurface.PROJECTS,
            List.of(HelpAudience.MODERATOR, HelpAudience.REPRESENTATIVE),
            localized("Turn approved work into visible execution", "Convierta trabajo aprobado en ejecucion visible"),
            localized("Project boards should show owners, due dates, and the next blocker, not just intent.", "Los tableros deben mostrar responsables, fechas y el siguiente bloqueo, no solo intencion."),
            localized(
                "Use project boards after decisions are recorded so the community can see what is moving, what is blocked, and who owns the next step.",
                "Use tableros de proyecto despues de registrar decisiones para que la comunidad vea que avanza, que esta bloqueado y quien posee el siguiente paso."
            ),
            List.of("projects", "execution", "tasks"),
            localized("Open project boards", "Abrir tableros"),
            "/communities/projects",
            true
        ),
        guide(
            "moderation-healthy-dialogue",
            "ARTICLE",
            HelpSurface.GENERAL,
            List.of(HelpAudience.MODERATOR, HelpAudience.REPRESENTATIVE),
            localized("How moderation stays proportional", "Como la moderacion se mantiene proporcional"),
            localized("Moderation should preserve traceability while reducing harm.", "La moderacion debe preservar la trazabilidad mientras reduce dano."),
            localized(
                "Hide or sanction content only with an auditable policy reason. Moderation in this product is meant to keep participation usable, not erase public history.",
                "Oculte o sancione contenido solo con una razon de politica auditable. La moderacion en este producto busca mantener la participacion util, no borrar la historia publica."
            ),
            List.of("moderation", "policy", "safety"),
            localized("Open moderation queue", "Abrir cola de moderacion"),
            "/moderation",
            false
        ),
        guide(
            "public-proof-for-institutions",
            "ARTICLE",
            HelpSurface.GENERAL,
            List.of(HelpAudience.REPRESENTATIVE),
            localized("What institutions should publish first", "Que deben publicar primero las instituciones"),
            localized("Start with one visible update, one decision basis, and one trust metric before opening new channels.", "Empiece con una actualizacion visible, una base de decision y una metrica de confianza antes de abrir nuevos canales."),
            localized(
                "Communities trust the platform when official updates, trust metrics, governance records, and decisions connect to one another in public.",
                "Las comunidades confian en la plataforma cuando las actualizaciones oficiales, metricas de confianza, registros de gobernanza y decisiones se conectan entre si en publico."
            ),
            List.of("institution", "trust", "official updates"),
            localized("Open trust metrics", "Abrir metricas de confianza"),
            "/communities/trust",
            false
        )
    );

    private final UserRepository userRepository;
    private final CommunityMembershipRepository communityMembershipRepository;

    public HelpCenterService(
        UserRepository userRepository,
        CommunityMembershipRepository communityMembershipRepository
    ) {
        this.userRepository = userRepository;
        this.communityMembershipRepository = communityMembershipRepository;
    }

    public HelpCenterResponse getHelpCenter(
        String username,
        UUID communityId,
        String language,
        String surface,
        String query
    ) {
        User user = getUser(username);
        HelpAudience persona = resolveAudience(user, communityId);
        HelpSurface requestedSurface = parseSurface(surface);
        String normalizedLanguage = normalizeLanguage(language);
        String normalizedQuery = normalizeQuery(query);
        Set<String> completedKeys = Set.copyOf(user.getOnboardingCompletedStepKeys());
        Set<String> dismissedKeys = Set.copyOf(user.getDismissedGuideKeys());

        List<OnboardingStepResponse> onboardingSteps = onboardingStepsFor(persona, normalizedLanguage).stream()
            .map(step -> new OnboardingStepResponse(
                step.key(),
                persona.name(),
                localized(step.title(), normalizedLanguage),
                localized(step.description(), normalizedLanguage),
                localized(step.actionLabel(), normalizedLanguage),
                step.actionRoute(),
                completedKeys.contains(step.key()),
                dismissedKeys.contains(step.key())
            ))
            .toList();

        List<HelpGuideResponse> guides = GUIDE_DEFINITIONS.stream()
            .filter(definition -> definition.audiences().contains(persona))
            .filter(definition -> requestedSurface == null
                || definition.surface() == HelpSurface.GENERAL
                || definition.surface() == requestedSurface)
            .filter(definition -> matchesQuery(definition, normalizedQuery))
            .map(definition -> new HelpGuideResponse(
                definition.id(),
                definition.kind(),
                definition.surface().name(),
                persona.name(),
                localized(definition.title(), normalizedLanguage),
                localized(definition.summary(), normalizedLanguage),
                localized(definition.body(), normalizedLanguage),
                definition.tags(),
                localized(definition.actionLabel(), normalizedLanguage),
                definition.actionRoute(),
                definition.dismissible(),
                dismissedKeys.contains(definition.id())
            ))
            .toList();

        return new HelpCenterResponse(
            persona.name(),
            normalizedLanguage,
            requestedSurface == null ? null : requestedSurface.name(),
            normalizedQuery,
            LocalDateTime.now(),
            user.getOnboardingCompletedStepKeys(),
            user.getDismissedGuideKeys(),
            onboardingSteps,
            guides
        );
    }

    @Transactional
    public HelpCenterStateResponse updateState(String username, UpdateHelpCenterStateRequest request) {
        User user = getUser(username);
        user.setOnboardingCompletedStepKeys(sanitizeKeys(request.completedStepKeys()));
        user.setDismissedGuideKeys(sanitizeKeys(request.dismissedGuideKeys()));
        userRepository.save(user);
        return new HelpCenterStateResponse(
            user.getOnboardingCompletedStepKeys(),
            user.getDismissedGuideKeys()
        );
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    private HelpAudience resolveAudience(User user, UUID communityId) {
        if (user.getRoleList().contains("ROLE_SUPER_ADMIN") || user.getRoleList().contains("ROLE_PUBLIC_SERVANT")) {
            return HelpAudience.REPRESENTATIVE;
        }

        if (communityId != null) {
            CommunityMembership membership = communityMembershipRepository.findByUserIdAndCommunityId(user.getId(), communityId)
                .orElse(null);
            if (membership != null) {
                if (membership.getRole() == CommunityRole.PUBLIC_SERVANT_LIAISON) {
                    return HelpAudience.REPRESENTATIVE;
                }
                if (membership.getRole() == CommunityRole.MODERATOR || membership.getRole() == CommunityRole.COORDINATOR) {
                    return HelpAudience.MODERATOR;
                }
            }
        }

        boolean hasModeratorContext = communityMembershipRepository.findByUserId(user.getId()).stream().anyMatch(
            membership -> membership.getRole() == CommunityRole.MODERATOR || membership.getRole() == CommunityRole.COORDINATOR
        );
        boolean hasRepresentativeContext = communityMembershipRepository.findByUserId(user.getId()).stream().anyMatch(
            membership -> membership.getRole() == CommunityRole.PUBLIC_SERVANT_LIAISON
        );

        if (hasRepresentativeContext) {
            return HelpAudience.REPRESENTATIVE;
        }
        if (hasModeratorContext) {
            return HelpAudience.MODERATOR;
        }
        return HelpAudience.CITIZEN;
    }

    private HelpSurface parseSurface(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return HelpSurface.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown help surface.");
        }
    }

    private String normalizeLanguage(String value) {
        return "es".equalsIgnoreCase(value) ? "es" : "en";
    }

    private String normalizeQuery(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> sanitizeKeys(List<String> keys) {
        if (keys == null) {
            return List.of();
        }
        return keys.stream()
            .map(String::trim)
            .filter(key -> !key.isBlank())
            .distinct()
            .sorted()
            .toList();
    }

    private boolean matchesQuery(GuideDefinition definition, String query) {
        if (query == null) {
            return true;
        }
        String haystack = String.join(
            " ",
            localized(definition.title(), "en"),
            localized(definition.title(), "es"),
            localized(definition.summary(), "en"),
            localized(definition.summary(), "es"),
            localized(definition.body(), "en"),
            localized(definition.body(), "es"),
            String.join(" ", definition.tags())
        ).toLowerCase(Locale.ROOT);
        return haystack.contains(query);
    }

    private List<OnboardingStepDefinition> onboardingStepsFor(HelpAudience persona, String language) {
        return switch (persona) {
            case REPRESENTATIVE -> List.of(
                step(
                    "representative-review-trust",
                    localized("Review trust metrics", "Revise metricas de confianza"),
                    localized(
                        "Start by checking what the community can already see about freshness, participation, and execution.",
                        "Empiece revisando lo que la comunidad ya puede ver sobre frescura, participacion y ejecucion."
                    ),
                    localized("Open trust metrics", "Abrir metricas"),
                    "/communities/trust"
                ),
                step(
                    "representative-publish-update",
                    localized("Publish one official update", "Publique una actualizacion oficial"),
                    localized(
                        "Reduce confusion by publishing one visible update tied to a real issue, proposal, or decision.",
                        "Reduzca confusion publicando una actualizacion visible ligada a un problema, propuesta o decision real."
                    ),
                    localized("Open official updates", "Abrir actualizaciones"),
                    "/communities/blog"
                ),
                step(
                    "representative-link-decision",
                    localized("Connect decisions to execution", "Conecte decisiones con ejecucion"),
                    localized(
                        "Use the decision ledger and project boards so the public can trace what was approved and who owns delivery.",
                        "Use el libro de decisiones y los tableros de proyecto para que el publico rastree que se aprobo y quien posee la entrega."
                    ),
                    localized("Open decision ledger", "Abrir decisiones"),
                    "/communities/decisions"
                )
            );
            case MODERATOR -> List.of(
                step(
                    "moderator-confirm-context",
                    localized("Confirm your active community", "Confirme su comunidad activa"),
                    localized(
                        "Moderation and collaboration rules change with community context, so confirm the current space first.",
                        "Las reglas de moderacion y colaboracion cambian con el contexto comunitario, asi que confirme primero el espacio actual."
                    ),
                    localized("Open communities hub", "Abrir comunidades"),
                    "/communities"
                ),
                step(
                    "moderator-guide-dialogue",
                    localized("Guide one high-signal discussion", "Guie una discusion de alta senal"),
                    localized(
                        "Use threads and proposals to redirect members toward evidence and policy-backed reasoning.",
                        "Use hilos y propuestas para redirigir a los miembros hacia evidencia y razonamiento respaldado por politica."
                    ),
                    localized("Open community talks", "Abrir conversaciones"),
                    "/communities/threads"
                ),
                step(
                    "moderator-review-safety",
                    localized("Review the moderation queue", "Revise la cola de moderacion"),
                    localized(
                        "Hide or sanction only when the policy reason is clear and traceable.",
                        "Oculte o sancione solo cuando la razon de politica sea clara y trazable."
                    ),
                    localized("Open moderation queue", "Abrir moderacion"),
                    "/moderation"
                )
            );
            case CITIZEN -> List.of(
                step(
                    "citizen-choose-community",
                    localized("Choose your active community", "Elija su comunidad activa"),
                    localized(
                        "Set the right community first so your reports, votes, and conversations stay in the right civic context.",
                        "Defina primero la comunidad correcta para que sus reportes, votos y conversaciones queden en el contexto civico correcto."
                    ),
                    localized("Open communities hub", "Abrir comunidades"),
                    "/communities"
                ),
                step(
                    "citizen-create-report",
                    localized("Create one clear civic report", "Cree un reporte civico claro"),
                    localized(
                        "Use the guided report flow to describe what happened, where, and why it matters.",
                        "Use el flujo guiado de reporte para describir que paso, donde y por que importa."
                    ),
                    localized("Open report flow", "Abrir reporte"),
                    "/report"
                ),
                step(
                    "citizen-follow-progress",
                    localized("Follow public progress", "Siga el progreso publico"),
                    localized(
                        "Return to your activity, decision ledger, and trust surfaces so you can see what changed after you participated.",
                        "Vuelva a su actividad, al libro de decisiones y a las superficies de confianza para ver que cambio despues de participar."
                    ),
                    localized("Open my activity", "Abrir mi actividad"),
                    "/mine"
                )
            );
        };
    }

    private static GuideDefinition guide(
        String id,
        String kind,
        HelpSurface surface,
        List<HelpAudience> audiences,
        LocalizedText title,
        LocalizedText summary,
        LocalizedText body,
        List<String> tags,
        LocalizedText actionLabel,
        String actionRoute,
        boolean dismissible
    ) {
        return new GuideDefinition(id, kind, surface, audiences, title, summary, body, tags, actionLabel, actionRoute, dismissible);
    }

    private static OnboardingStepDefinition step(
        String key,
        LocalizedText title,
        LocalizedText description,
        LocalizedText actionLabel,
        String actionRoute
    ) {
        return new OnboardingStepDefinition(key, title, description, actionLabel, actionRoute);
    }

    private static LocalizedText localized(String en, String es) {
        return new LocalizedText(en, es);
    }

    private String localized(LocalizedText text, String language) {
        return "es".equalsIgnoreCase(language) ? text.es() : text.en();
    }

    private record LocalizedText(String en, String es) {}

    private record OnboardingStepDefinition(
        String key,
        LocalizedText title,
        LocalizedText description,
        LocalizedText actionLabel,
        String actionRoute
    ) {}

    private record GuideDefinition(
        String id,
        String kind,
        HelpSurface surface,
        List<HelpAudience> audiences,
        LocalizedText title,
        LocalizedText summary,
        LocalizedText body,
        List<String> tags,
        LocalizedText actionLabel,
        String actionRoute,
        boolean dismissible
    ) {}
}
