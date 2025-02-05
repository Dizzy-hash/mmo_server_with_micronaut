package server.player.model;

import io.micronaut.core.annotation.Introspected;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Introspected
@Serdeable
public class CreateCharacterRequest {
    // there will be more as per requirements from UE

    @Pattern(message = "Name can only contain letters and numbers", regexp = "^[a-zA-Z0-9]*$")
    @Size(min = 3, max = 25)
    String name;

    @NotNull Map<String, String> appearanceInfo;

    @NotNull String className;
}
