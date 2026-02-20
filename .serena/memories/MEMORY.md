# UnofficialDMZAddon Project Memory

## Project Purpose
Unofficial addon for DragonMineZ (DMZ) Minecraft mod. Adds Ultra Instinct Omen transformation for Saiyans with auto-dodge and strike boost mechanics.

## Tech Stack
- Java, Minecraft Forge mod (1.20.x era)
- Build: `gradlew build` from project root
- DMZ source is in `dragonminez/` subdirectory (used as reference/dependency)

## Key Files
- `src/main/java/org/unofficial/unofficialdmzaddon/dmz/UltraInstinctOmenCombatHandler.java` — combat events (dodge + strike boost)
- `src/main/java/org/unofficial/unofficialdmzaddon/dmz/DMZRuntimeAccess.java` — bridge to DMZ API
- `src/main/java/org/unofficial/unofficialdmzaddon/UnofficialDMZAddon.java` — mod entry point

## Critical Pattern: Damage Cancellation
- Use `LivingAttackEvent` (NOT `LivingHurtEvent`) to fully cancel a hit — no hurt animation, no invulnerability frames, no damage.
- `LivingHurtEvent` fires AFTER the hit is registered; cancelling it still shows the hurt flash.
- DMZ's own Ki Barrier uses `LivingAttackEvent` for the same reason (`ForgeCommonEvents.onLivingAttack`).
- DMZ's `CombatEvent.onLivingHurt` runs at `EventPriority.HIGH`; our handlers run at `LOWEST`.
