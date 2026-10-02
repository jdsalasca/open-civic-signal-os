package org.opencivic.signalos.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.opencivic.signalos.web.dto.CommunityRoomEventResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-memory fan-out of community room events to connected browsers.
 *
 * ponytail: single-node in-memory registry, scoped to one JVM. Swap for Redis pub/sub or a
 * STOMP broker when the API runs multi-instance, otherwise events only reach same-node clients.
 */
@Component
public class CommunityRoomEventBroker {
    private static final long STREAM_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<UUID, List<SseEmitter>> emittersByRoom = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID roomId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        emittersByRoom.computeIfAbsent(roomId, key -> java.util.Collections.synchronizedList(new java.util.ArrayList<>()))
            .add(emitter);
        emitter.onCompletion(() -> remove(roomId, emitter));
        emitter.onTimeout(() -> remove(roomId, emitter));
        emitter.onError(throwable -> remove(roomId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"roomId\":\"" + roomId + "\"}"));
        } catch (IOException e) {
            remove(roomId, emitter);
        }
        return emitter;
    }

    public void publish(CommunityRoomEventResponse event) {
        List<SseEmitter> emitters = emittersByRoom.get(event.roomId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("room-message").data(event));
            } catch (IOException e) {
                remove(event.roomId(), emitter);
            }
        }
    }

    private void remove(UUID roomId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByRoom.get(roomId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByRoom.remove(roomId);
        }
    }
}