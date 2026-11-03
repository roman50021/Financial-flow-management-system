package com.example.ffms.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LiveUpdateService {

    private static final long RECONNECT_DELAY_MILLIS = 1_000L;

    private final List<SseEmitter> clients = new CopyOnWriteArrayList<>();
    private final AtomicLong eventSequence = new AtomicLong();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        clients.add(emitter);
        emitter.onCompletion(() -> clients.remove(emitter));
        emitter.onTimeout(() -> remove(emitter));
        emitter.onError(error -> clients.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("ready")
                    .reconnectTime(RECONNECT_DELAY_MILLIS));
        } catch (Exception error) {
            remove(emitter);
        }
        return emitter;
    }

    public void publish(String resource) {
        String eventId = Long.toString(eventSequence.incrementAndGet());
        for (SseEmitter client : clients) {
            try {
                synchronized (client) {
                    client.send(SseEmitter.event()
                            .id(eventId)
                            .name("data-change")
                            .data(resource)
                            .reconnectTime(RECONNECT_DELAY_MILLIS));
                }
            } catch (Exception error) {
                remove(client);
            }
        }
    }

    @Scheduled(initialDelay = 15_000L, fixedRate = 15_000L)
    public void keepConnectionsAlive() {
        for (SseEmitter client : clients) {
            try {
                synchronized (client) {
                    client.send(SseEmitter.event().comment("keep-alive"));
                }
            } catch (Exception error) {
                remove(client);
            }
        }
    }

    private void remove(SseEmitter emitter) {
        clients.remove(emitter);
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // The HTTP client may already be disconnected; cleanup must never break a mutation request.
        }
    }
}
