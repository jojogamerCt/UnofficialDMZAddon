package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

/**
 * Sanitizes saved Alien character data on player login.
 *
 * <p>Because character fields like hairId, eyesType and noseType are
 * persisted in NBT, a player who created their character before the
 * AlienRaceInstaller set the correct defaults will keep stale values
 * (e.g. hairId=1, eyesType=1, noseType=1) that show wrong options in
 * the customisation UI and renderer.
 *
 * <p>This handler clamps those fields to the only valid values for the
 * Alien race every time the player logs in.
 */
public class AlienCharacterSanitizer {

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        StatsProvider.get(StatsCapability.INSTANCE, event.getEntity()).ifPresent(stats -> {
            Character character = stats.getCharacter();
            if (!SpecialRaceFormsDefinitions.ALIEN_RACE.equals(character.getRaceName())) {
                return;
            }

            boolean dirty = false;

            // Aliens are bald — hairId must always be 0.
            if (character.getHairId() != 0) {
                character.setHairId(0);
                dirty = true;
            }

            // Only 1 eye type (0) exists.
            if (character.getEyesType() != 0) {
                character.setEyesType(0);
                dirty = true;
            }

            // Only 1 nose type (0) exists.
            if (character.getNoseType() != 0) {
                character.setNoseType(0);
                dirty = true;
            }

            // Migrate stale body colour: if it still holds the old default (#8A8A8A)
            // replace it with Jiren's correct pale grey.
            String body = character.getBodyColor();
            if ("#8A8A8A".equalsIgnoreCase(body) || body == null || body.isEmpty()) {
                character.setBodyColor("#D4CECC");
                dirty = true;
            }

            // Migrate stale eye colours: if they still hold old defaults
            // (#FFFFFF white / #4A90D9 blue / #000000 full black), replace
            // them with the Alien default eye colour.
            String eye1 = character.getEye1Color();
            String eye2 = character.getEye2Color();
            if ("#FFFFFF".equalsIgnoreCase(eye1) || "#4A90D9".equalsIgnoreCase(eye1)
                    || "#000000".equalsIgnoreCase(eye1)
                    || eye1 == null || eye1.isEmpty()) {
                character.setEye1Color(AlienRaceInstaller.ALIEN_DEFAULT_EYE_COLOR);
                dirty = true;
            }
            if ("#FFFFFF".equalsIgnoreCase(eye2) || "#4A90D9".equalsIgnoreCase(eye2)
                    || "#000000".equalsIgnoreCase(eye2)
                    || eye2 == null || eye2.isEmpty()) {
                character.setEye2Color(AlienRaceInstaller.ALIEN_DEFAULT_EYE_COLOR);
                dirty = true;
            }

            if (dirty) {
                UnofficialDMZAddon.LOGGER.info(
                        "[Unofficial DMZ Addon] Sanitized stale Alien character fields for {}",
                        event.getEntity().getName().getString());
            }
        });
    }
}
