package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Implements Alien Blood-Fueled Ki: refund Ki at a health cost, then empower the next hit. */
public final class AlienRacialPassiveHandler {
    private final Map<UUID,Float> previousEnergy=new HashMap<>();
    private final Map<UUID,Float> pendingDamage=new HashMap<>();

    @SubscribeEvent public void onTick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide()||!(event.player instanceof ServerPlayer player))return;
        StatsProvider.get(StatsCapability.INSTANCE,player).ifPresent(data->{
            if(!SpecialRaceFormsDefinitions.ALIEN_RACE.equalsIgnoreCase(data.getCharacter().getRaceName())){previousEnergy.remove(player.getUUID());return;}
            float now=data.getResources().getCurrentEnergy();Float before=previousEnergy.put(player.getUUID(),now);
            if(before==null||now>=before||player.tickCount<40)return;
            float spent=before-now;float refund=spent*0.25f;float healthCost=spent*0.125f;
            data.getResources().addEnergy(refund);previousEnergy.put(player.getUUID(),data.getResources().getCurrentEnergy());
            float safeCost=Math.min(healthCost,Math.max(0,player.getHealth()-1f));
            if(safeCost>0){player.setHealth(player.getHealth()-safeCost);pendingDamage.merge(player.getUUID(),safeCost,Float::sum);}
        });
    }

    @SubscribeEvent public void onHurt(LivingHurtEvent event){
        if(!(event.getSource().getEntity() instanceof ServerPlayer attacker)||event.isCanceled())return;
        StatsProvider.get(StatsCapability.INSTANCE,attacker).ifPresent(data->{
            if(!SpecialRaceFormsDefinitions.ALIEN_RACE.equalsIgnoreCase(data.getCharacter().getRaceName()))return;
            float stored=pendingDamage.getOrDefault(attacker.getUUID(),0f);if(stored<=0)return;
            event.setAmount(event.getAmount()+stored);pendingDamage.remove(attacker.getUUID());
        });
    }
}