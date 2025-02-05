package server.socket.service;

import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import server.combat.service.PlayerCombatService;
import server.motion.dto.PlayerMotion;
import server.session.SessionParamHelper;
import server.socket.model.MessageType;
import server.socket.model.SocketMessage;
import server.socket.producer.UpdateProducer;
import server.socket.service.integrations.attributes.StatsSocketIntegration;
import server.socket.service.integrations.items.ItemSocketIntegration;

@Slf4j
@Singleton
public class SocketProcessOutgoingService {
    // this service determines what happens with the outgoing message - specifically where it gets
    // sent

    @Inject UpdateProducer updateProducer;

    @Inject ItemSocketIntegration itemSocketIntegration;

    @Inject StatsSocketIntegration attributeSocketIntegration;

    @Inject PlayerCombatService combatService;

    Map<String, BiConsumer<SocketMessage, WebSocketSession>> functionMap;

    public SocketProcessOutgoingService() {
        this.functionMap =
                new HashMap<>(
                        Map.of(
                                MessageType.PLAYER_MOTION.getType(), this::handlePlayerMotionUpdate,
                                MessageType.CREATE_MOB.getType(), this::handleCreateMob,
                                MessageType.MOB_MOTION.getType(), this::handleMobMotionUpdate,
                                MessageType.PICKUP_ITEM.getType(), this::handlePickupItem,
                                MessageType.DROP_ITEM.getType(), this::handleDropItem,
                                MessageType.FETCH_INVENTORY.getType(), this::handleFetchInventory,
                                MessageType.FETCH_EQUIPPED.getType(), this::handleFetchEquipped,
                                MessageType.EQUIP_ITEM.getType(), this::handleEquipItem,
                                MessageType.UN_EQUIP_ITEM.getType(), this::handleUnEquipItem,
                                MessageType.FETCH_STATS.getType(), this::handleFetchStats));
        // map.of supports up to 10 items
        this.functionMap.put(MessageType.TRY_ATTACK.getType(), this::handleTryAttack);
        this.functionMap.put(MessageType.SET_SESSION_ID.getType(), this::setSessionId);
    }

    public void processMessage(SocketMessage socketMessage, WebSocketSession session) {
        String updateType = socketMessage.getUpdateType();

        if (updateType == null) {
            throw new InvalidParameterException("message type missing");
        }

        if (functionMap.containsKey(updateType)) {
            functionMap.get(updateType).accept(socketMessage, session);
        } else {
            log.error("Did not recognise update type, {}", updateType);
        }
    }

    // update motion for player
    private void handlePlayerMotionUpdate(SocketMessage message, WebSocketSession session) {
        PlayerMotion motion = message.getPlayerMotion();

        Map<String, String> validateFields =
                Map.of(
                        "Player name",
                        motion.getPlayerName(),
                        "b",
                        "i",
                        "Map",
                        motion.getMotion().getMap(),
                        "X co-ordinate",
                        motion.getMotion().getX().toString(),
                        "Y co-ordinate",
                        motion.getMotion().getY().toString(),
                        "Z co-ordinate",
                        motion.getMotion().getZ().toString());

        for (Map.Entry<String, String> entry : validateFields.entrySet()) {
            if (!validate(entry.getKey(), entry.getValue())) {
                return;
            }
        }

        updateProducer.sendPlayerMotionUpdate(message.getPlayerMotion());
    }

    // update motion for monster
    private void handleMobMotionUpdate(SocketMessage message, WebSocketSession session) {
        updateProducer.sendMobMotionUpdate(message.getMonster());
    }

    private void handleCreateMob(SocketMessage message, WebSocketSession session) {
        SessionParamHelper.addTrackingMobs(session, Set.of(message.getMobId()));
        updateProducer.sendCreateMob(message.getMonster());
    }

    // handle inventory interaction
    private void handlePickupItem(SocketMessage message, WebSocketSession session) {
        itemSocketIntegration.handlePickupItem(message.getInventoryRequest(), session);
    }

    private void handleDropItem(SocketMessage message, WebSocketSession session) {
        itemSocketIntegration.handleDropItem(message.getInventoryRequest(), session);
    }

    private void handleFetchInventory(SocketMessage message, WebSocketSession session) {
        itemSocketIntegration.handleFetchInventory(message.getInventoryRequest(), session);
    }

    private void handleFetchEquipped(SocketMessage message, WebSocketSession session) {
        itemSocketIntegration.handleFetchEquipped(message.getInventoryRequest(), session);
    }

    private void handleEquipItem(SocketMessage message, WebSocketSession session) {
        itemSocketIntegration.handleEquipItem(message.getInventoryRequest(), session);
    }

    private void handleUnEquipItem(SocketMessage message, WebSocketSession session) {
        itemSocketIntegration.handleUnEquipItem(message.getInventoryRequest(), session);
    }

    private void handleFetchStats(SocketMessage message, WebSocketSession session) {
        String actorId = message.getPlayerName().isBlank() ? message.getMobInstanceId() : message.getPlayerName();
        attributeSocketIntegration.handleFetchStats(actorId, session);
    }

    private void handleTryAttack(SocketMessage message, WebSocketSession session) {
        combatService.requestAttack(session, message.getCombatRequest());
    }

    private void setSessionId(SocketMessage message, WebSocketSession session) {
        String serverName = message.getServerName() == null || message.getServerName().isBlank() ? null : message.getServerName();
        String playerName = message.getPlayerName() == null || message.getPlayerName().isBlank() ? null : message.getPlayerName();

        SessionParamHelper.setServerName(session, serverName);
        SessionParamHelper.setPlayerName(session, playerName);
    }

    private boolean validate(String value, String name) {
        if (!isValid(value)) {
            log.error("{} is not valid in player motion!", name);
            return false;
        }
        return true;
    }

    private boolean isValid(String data) {
        return data != null && !data.isBlank();
    }
}
