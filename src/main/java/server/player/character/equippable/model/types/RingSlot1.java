package server.player.character.equippable.model.types;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import server.items.types.ItemType;
import server.player.character.equippable.model.EquippedItems;

@Data
@NoArgsConstructor
@JsonTypeName("RING")
@EqualsAndHashCode(callSuper=false)
public class RingSlot1 extends EquippedItems {

    public RingSlot1(String characterName, String itemInstanceId) {
        super(characterName, itemInstanceId, ItemType.RING.getType());
    }
}
