package server.socket.service.integrations.items;

import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import server.items.equippable.service.EquipItemService;
import server.items.inventory.model.ItemInstanceIds;
import server.items.inventory.model.response.GenericInventoryData;
import server.items.inventory.service.InventoryService;
import server.session.SessionParamHelper;
import server.socket.model.SocketResponse;
import server.socket.model.SocketResponseSubscriber;
import server.socket.model.SocketResponseType;
import server.socket.producer.UpdateProducer;

@Slf4j
@Singleton
public class ItemSocketIntegration {
    // some functions will require to be processed serially, like picking up items
    // we should validate request, such as inventory has space and provide result before we can
    // pickup item
    // here we can have access to session, so we can provide results back before publishing changes

    @Inject InventoryService inventoryService;

    @Inject SocketResponseSubscriber socketResponseSubscriber;

    @Inject UpdateProducer updateProducer;

    @Inject EquipItemService equipItemService;

    public void handleDropItem(GenericInventoryData request, WebSocketSession session) {
        inventoryService
                .dropItem(
                        request.getCharacterName(),
                        request.getItemInstanceId(),
                        request.getLocation())
                .doOnError(
                        e -> {
                            log.warn(
                                    "Failed to pickup item, {}",
                                    e.getMessage()); // could be inventory full
                            SocketResponse response =
                                    SocketResponse.builder()
                                            .messageType(
                                                    SocketResponseType.INVENTORY_ERROR.getType())
                                            .error(e.getMessage())
                                            .build();
                            session.send(response).subscribe(socketResponseSubscriber);
                        })
                .doOnSuccess(
                        droppedItem -> {
                            updateProducer.addItemToMap(droppedItem);
                            sendInventoryToPlayer(session, request.getCharacterName());
                        })
                .subscribe();
    }

    public void handlePickupItem(GenericInventoryData request, WebSocketSession session) {
        inventoryService
                .pickupItem(request)
                .doOnError(
                        e -> {
                            log.warn(
                                    "Failed to pickup item, {}",
                                    e.getMessage()); // could be inventory full
                            SocketResponse response =
                                    SocketResponse.builder()
                                            .messageType(
                                                    SocketResponseType.INVENTORY_ERROR.getType())
                                            .error(e.getMessage())
                                            .build();
                            session.send(response).subscribe(socketResponseSubscriber);
                        })
                .doOnSuccess(
                        inventory -> {
                            updateProducer.removeItemFromMap(request.getItemInstanceId());

                            GenericInventoryData inventoryData = new GenericInventoryData();
                            inventoryData.setInventory(inventory);
                            SocketResponse response =
                                    SocketResponse.builder()
                                            .messageType(
                                                    SocketResponseType.INVENTORY_UPDATE.getType())
                                            .inventoryData(inventoryData)
                                            .build();
                            session.send(response).subscribe(socketResponseSubscriber);
                        })
                .subscribe();
    }

    public void handleFetchInventory(GenericInventoryData request, WebSocketSession session) {
        sendInventoryToPlayer(session, request.getCharacterName());
    }

    public void handleFetchEquipped(GenericInventoryData request, WebSocketSession session) {
        equipItemService
                .getEquippedItems(request.getCharacterName())
                .doOnSuccess(
                        equippedItems -> {
                            SessionParamHelper.setEquippedItems(session, equippedItems);
                            if (equippedItems.isEmpty()) {
                                return;
                            }
                            GenericInventoryData inventoryData = new GenericInventoryData();
                            inventoryData.setCharacterName(equippedItems.get(0).getCharacterName());
                            inventoryData.setEquippedItems(equippedItems);
                            SocketResponse response =
                                    SocketResponse.builder()
                                            .messageType(
                                                    SocketResponseType.ADD_EQUIP_ITEM.getType())
                                            .inventoryData(inventoryData)
                                            .build();
                            session.send(response).subscribe(socketResponseSubscriber);
                        })
                .subscribe();
    }

    public void handleEquipItem(GenericInventoryData request, WebSocketSession session) {
        equipItemService
                .equipItem(request.getItemInstanceId(), request.getCharacterName())
                .doOnError(e -> log.error("Failed to equip item, {}", e.getMessage()))
                .doOnSuccess(
                        equippedItems -> {
                            SessionParamHelper.addToEquippedItems(session, equippedItems);
                            sendInventoryToPlayer(session, request.getCharacterName());

                            GenericInventoryData equipData = new GenericInventoryData();
                            equipData.setEquippedItems(List.of(equippedItems));
                            equipData.setCharacterName(equippedItems.getCharacterName());
                            SocketResponse res =
                                    SocketResponse.builder()
                                            .inventoryData(equipData)
                                            .messageType(
                                                    SocketResponseType.ADD_EQUIP_ITEM.getType())
                                            .build();
                            session.send(res)
                                    .subscribe(socketResponseSubscriber); // notify this player
                            updateProducer.notifyEquipItems(
                                    List.of(equippedItems)); // notify other players
                        })
                .subscribe();
    }

    public void handleUnEquipItem(GenericInventoryData request, WebSocketSession session) {
        equipItemService
                .unEquipItem(request.getItemInstanceId(), request.getCharacterName())
                .doOnError(e -> log.error("Failed to un-equip item, {}", e.getMessage()))
                .doOnSuccess(
                        unequippedItemInstanceId -> {
                            SessionParamHelper.removeFromEquippedItems(
                                    session, unequippedItemInstanceId);
                            sendInventoryToPlayer(session, request.getCharacterName());

                            GenericInventoryData equipData = new GenericInventoryData();
                            equipData.setItemInstanceIds(List.of(unequippedItemInstanceId));
                            equipData.setCharacterName(request.getCharacterName());

                            SocketResponse res =
                                    SocketResponse.builder()
                                            .inventoryData(equipData)
                                            .itemInstanceIds(Set.of(unequippedItemInstanceId))
                                            .messageType(
                                                    SocketResponseType.REMOVE_EQUIP_ITEM.getType())
                                            .build();

                            session.send(res).subscribe(socketResponseSubscriber);
                            ItemInstanceIds itemInstanceIds =
                                    ItemInstanceIds.builder()
                                            .itemInstanceIds(List.of(unequippedItemInstanceId))
                                            .playerName(request.getCharacterName())
                                            .build();
                            updateProducer.notifyUnEquipItems(
                                    itemInstanceIds); // notify other players
                        })
                .subscribe();
    }

    private void sendInventoryToPlayer(WebSocketSession session, String playerName) {
        inventoryService
                .getInventory(playerName)
                .doOnError(e -> log.error("Failed to fetch inventory, {}", e.getMessage()))
                .doOnSuccess(
                        inventory -> {
                            GenericInventoryData inventoryData = new GenericInventoryData();
                            inventoryData.setInventory(inventory);

                            SocketResponse res =
                                    SocketResponse.builder()
                                            .inventoryData(inventoryData)
                                            .messageType(
                                                    SocketResponseType.INVENTORY_UPDATE.getType())
                                            .build();

                            session.send(res).subscribe(socketResponseSubscriber);
                        })
                .subscribe();
    }
}
