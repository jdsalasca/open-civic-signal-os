package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.CommunityIntegration;
import org.opencivic.signalos.domain.CommunityIntegrationChannel;
import org.opencivic.signalos.service.CommunityIntegrationConnector;
import org.opencivic.signalos.service.MessagingCommunityIntegrationConnector;

/**
 * The messaging relay reaches people's phones, so the failure modes matter more than the happy path.
 *
 * <p>The bot token belongs to the community rather than the platform: a platform-owned bot would make
 * every message come from the platform, and a resident replying to it would reach nobody who can act.
 * A missing token is therefore a clear refusal, not a silent no-op.
 */
class MessagingConnectorTest {

    private HttpServer server;
    private String baseUri;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();

    private MessagingCommunityIntegrationConnector connector;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        baseUri = "http://127.0.0.1:" + server.getAddress().getPort();
        // Point both providers at the stub. Without this the only way to exercise the connector is to
        // call the real providers, which a test must never do.
        connector = new MessagingCommunityIntegrationConnector(baseUri, baseUri);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldSupportBothMessagingChannels() {
        assertThat(connector.supports(CommunityIntegrationChannel.WHATSAPP)).isTrue();
        assertThat(connector.supports(CommunityIntegrationChannel.TELEGRAM)).isTrue();
        assertThat(connector.supports(CommunityIntegrationChannel.WEBHOOK)).isFalse();
        assertThat(connector.supports(CommunityIntegrationChannel.EMAIL_DIGEST)).isFalse();
    }

    @Test
    void aMissingBotTokenShouldBeRefusedWithTheReason() {
        CommunityIntegration integration = integration(CommunityIntegrationChannel.TELEGRAM, "chat-1", null);

        CommunityIntegrationConnector.DeliveryResult result = connector.deliver(integration, "Hello");

        assertThat(result.delivered()).isFalse();
        // The reason names why the community must supply its own bot.
        assertThat(result.error()).contains("community must supply its own bot");
        assertThat(result.error()).contains("replies reach someone who can act");
    }

    @Test
    void aMissingChatIdShouldBeRefused() {
        CommunityIntegration integration = integration(CommunityIntegrationChannel.WHATSAPP, "  ", "token");

        CommunityIntegrationConnector.DeliveryResult result = connector.deliver(integration, "Hello");

        assertThat(result.delivered()).isFalse();
        assertThat(result.error()).contains("No chat or group id");
    }

    @Test
    void anEmptyMessageShouldBeRefused() {
        CommunityIntegration integration = integration(CommunityIntegrationChannel.TELEGRAM, "chat-1", "token");

        CommunityIntegrationConnector.DeliveryResult result = connector.deliver(integration, "   ");

        assertThat(result.delivered()).isFalse();
        assertThat(result.error()).contains("Refusing to send an empty message");
    }

    @Test
    void telegramShouldPutTheTokenInThePathAndSendJson() {
        CommunityIntegration integration = integration(CommunityIntegrationChannel.TELEGRAM, "chat-1", "bot-token");

        CommunityIntegrationConnector.DeliveryResult result = connector.deliver(integration, "Water main break");

        assertThat(result.delivered()).isTrue();
        assertThat(lastPath.get()).contains("/botbot-token/sendMessage");
        assertThat(lastBody.get()).contains("\"chat_id\":\"chat-1\"");
        assertThat(lastBody.get()).contains("Water main break");
    }

    @Test
    void whatsappShouldPutTheTokenInTheHeaderNotThePath() {
        CommunityIntegration integration = integration(CommunityIntegrationChannel.WHATSAPP, "34600000000", "wa-token");

        CommunityIntegrationConnector.DeliveryResult result = connector.deliver(integration, "Water main break");

        assertThat(result.delivered()).isTrue();
        // The token goes in the header rather than the path, which is better than Telegram's shape.
        assertThat(lastAuth.get()).isEqualTo("Bearer wa-token");
        assertThat(lastPath.get()).doesNotContain("wa-token");
        assertThat(lastBody.get()).contains("messaging_product=whatsapp");
    }

    @Test
    void aLongMessageShouldBeTruncatedRatherThanRejected() {
        CommunityIntegration integration = integration(CommunityIntegrationChannel.TELEGRAM, "chat-1", "token");
        String longMessage = "x".repeat(5000);

        CommunityIntegrationConnector.DeliveryResult result = connector.deliver(integration, longMessage);

        assertThat(result.delivered()).isTrue();
        // Truncated with a marker, so a reader knows the message was cut rather than ending oddly.
        assertThat(lastBody.get()).contains("...");
        assertThat(lastBody.get().length()).isLessThan(5000);
    }

    @Test
    void aProviderErrorShouldCarryTheProvidersReason() {
        server.removeContext("/");
        server.createContext("/", exchange -> {
            byte[] response = "{\"error\":\"chat not found\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        CommunityIntegration integration = integration(CommunityIntegrationChannel.TELEGRAM, "chat-1", "token");
        CommunityIntegrationConnector.DeliveryResult result = connector.deliver(integration, "Hello");

        assertThat(result.delivered()).isFalse();
        assertThat(result.statusCode()).isEqualTo(400);
        // The provider's body is the only place the reason exists.
        assertThat(result.error()).contains("chat not found");
    }

    @Test
    void anUnreachableProviderShouldFailRatherThanThrow() {
        // A connector pointed at a port nothing is listening on, which is what a provider outage
        // looks like from here.
        MessagingCommunityIntegrationConnector deadConnector =
            new MessagingCommunityIntegrationConnector("http://127.0.0.1:1", "http://127.0.0.1:1");
        CommunityIntegration integration = integration(CommunityIntegrationChannel.TELEGRAM, "chat-1", "token");

        CommunityIntegrationConnector.DeliveryResult result = deadConnector.deliver(integration, "Hello");

        assertThat(result.delivered()).isFalse();
        assertThat(result.error()).isNotBlank();
    }

    private CommunityIntegration integration(
        CommunityIntegrationChannel channel, String chatId, String token
    ) {
        CommunityIntegration integration = new CommunityIntegration();
        integration.setChannel(channel);
        integration.setName("Test relay");
        integration.setTargetUri(chatId);
        integration.setSecretHash(token);
        integration.setEnabled(true);
        return integration;
    }
}