package server.motion.socket.v1.controller;

import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import jakarta.inject.Inject;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import server.motion.model.MotionMessage;
import server.motion.service.PlayerMotionService;
import server.motion.socket.model.PlayerMotionListSubscriber;

@Deprecated // use CommunicationSocket instead
@Slf4j
@ServerWebSocket("/v1/player-motion/{map}/{playerName}/")
public class PlayerMotionSocket {

    private final WebSocketBroadcaster broadcaster;

    @Inject PlayerMotionService playerMotionService;

    @Inject PlayerMotionListSubscriber subscriber;

    public PlayerMotionSocket(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @OnOpen
    public Publisher<String> onOpen(String map, String playerName, WebSocketSession session) {
        return broadcaster.broadcast(
                String.format("[%s] Joined %s!", playerName, map), isValid(playerName));
    }

    @OnMessage
    public void onMessage(
            String playerName, String map, MotionMessage message, WebSocketSession session) {
        if (message.getUpdate()) {
            playerMotionService.updatePlayerMotion(playerName, message.getMotion()).subscribe();
        }

        playerMotionService
                .getPlayersNearMe(message.getMotion(), playerName)
                .doOnSuccess(
                        motionList -> {
                            session.send(motionList).subscribe(subscriber);
                        })
                .subscribe();
    }

    @OnClose
    public Publisher<String> onClose(String playerName, String map, WebSocketSession session) {
        playerMotionService.disconnectPlayer(playerName);
        return broadcaster.broadcast(String.format("[%s] Leaving %s!", playerName, map));
    }

    private Predicate<WebSocketSession> isValid(String playerName) {
        // we will report to player every time they call update about other players nearby
        return s ->
                playerName.equalsIgnoreCase(
                        s.getUriVariables().get("playerName", String.class, null));
    }
}
