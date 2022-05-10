package server.items.service;

import server.common.dto.Location;
import server.items.dropped.model.DroppedItem;
import server.items.dto.Item;
import server.items.repository.ItemRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Singleton
public class ItemService {

    @Inject
    ItemRepository itemRepository;


    public DroppedItem dropItem(String itemId, Location location) {
        LocalDateTime now = LocalDateTime.now();
        Item foundItem = itemRepository.findByItemId(itemId);

        String uuid = UUID.randomUUID().toString(); // generate unique ID for the dropped item

        DroppedItem droppedItem = new DroppedItem(uuid, location.getMap(), location, foundItem, now);

        return itemRepository.createDroppedItem(droppedItem);
    }

    public DroppedItem getDroppedItemById(String droppedItemId) {
        return itemRepository.findDroppedItemById(droppedItemId);
    }

    public List<DroppedItem> getItemsInMap(Location location) {
        return itemRepository.getItemsNear(location);
    }

    public void deleteDroppedItem(String droppedItemId) {
        itemRepository.deleteDroppedItem(droppedItemId);
    }

    public Item createItem(Item item) {
        return itemRepository.createItem(item);
    }

    public void clearAllItemData() {
        // this is a function for test purposes
        itemRepository.deleteAllItemData();
    }
}
