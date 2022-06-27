package server.player.character.equippable.model.types;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import server.items.types.ItemType;
import server.player.character.equippable.model.EquippedItems;

@Data
@NoArgsConstructor
@JsonTypeName("SHOULDER")
@EqualsAndHashCode(callSuper=false)
public class ShoulderSlot extends EquippedItems {

    public ShoulderSlot(String characterName, String characterItemId) {
        super(characterName, characterItemId, ItemType.SHOULDER.getType());
    }
}
