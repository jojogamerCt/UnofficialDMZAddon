package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Universal form baseline plus distinct thematic specialties for every form family. */
public final class FormSpecialBuffHandler {
    private final Map<UUID,String> announced = new HashMap<>();

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(data -> {
                String form=formKey(data.getCharacter().getActiveFormGroup(),data.getCharacter().getActiveForm());
                if (form.isBlank()) return;
                float bonus=1.05f; // every transformation has a tangible universal combat specialty
                float missing=1f-attacker.getHealth()/attacker.getMaxHealth();
                if (form.contains("rage")) bonus+=0.35f*missing;
                else if (form.contains("ultraego")) bonus+=0.30f*missing;
                else if (form.contains("rose")) bonus+=0.20f;
                else if (form.contains("blue")) bonus+=0.15f;
                else if (form.contains("golden")||form.contains("black")) bonus+=0.12f;
                else if (form.contains("legendary")||form.contains("fullpower")) bonus+=0.10f;
                event.setAmount(event.getAmount()*bonus);
                if (form.contains("rose")) attacker.heal(event.getAmount()*0.03f);
            });
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            StatsProvider.get(StatsCapability.INSTANCE,victim).ifPresent(data->{
                String form=formKey(data.getCharacter().getActiveFormGroup(),data.getCharacter().getActiveForm());
                if(form.isBlank())return;float reduction=0.05f;
                if(form.contains("god"))reduction+=0.10f;
                if(form.contains("giant")||form.contains("oozaru")||form.contains("metal"))reduction+=0.12f;
                event.setAmount(event.getAmount()*(1f-reduction));
            });
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide()||!(event.player instanceof ServerPlayer player)||player.tickCount%20!=0)return;
        StatsProvider.get(StatsCapability.INSTANCE,player).ifPresent(data->{
            String group=data.getCharacter().getActiveFormGroup();String name=data.getCharacter().getActiveForm();String key=formKey(group,name);
            String last=announced.get(player.getUUID());
            if(!key.equals(last)){announced.put(player.getUUID(),key);if(!key.isBlank())player.sendSystemMessage(Component.literal("Form buff: "+describe(key)));}
            if(key.isBlank())return;
            float regen=0.0015f;
            if(key.contains("god"))regen=0.010f;else if(key.contains("blue"))regen=0.006f;else if(key.contains("ui")||key.contains("ultrainstinct"))regen=0.004f;else if(key.contains("metal"))regen=0.008f;
            data.getResources().addEnergy(data.getMaxEnergy()*regen);
        });
    }

    private static String formKey(String group,String form){return ((group==null?"":group)+"."+(form==null?"":form)).toLowerCase();}
    private static String describe(String key){
        String base="+5% damage, 5% damage resistance and passive energy recovery";
        if(key.contains("rage"))return base+"; damage rises by up to 35% as health falls";
        if(key.contains("ultraego"))return base+"; damage rises by up to 30% as health falls";
        if(key.contains("ultrainstinct"))return base+"; Ultra Instinct auto-dodge and precision-strike mechanics active";
        if(key.contains("rose"))return base+"; +20% damage and 3% damage dealt is restored as health";
        if(key.contains("blue"))return base+"; +15% damage, improved energy recovery, compatible with Kaioken";
        if(key.contains("god"))return base+"; +10% additional resistance and strong divine energy recovery";
        if(key.contains("giant")||key.contains("oozaru")||key.contains("metal"))return base+"; +12% additional damage resistance";
        if(key.contains("golden")||key.contains("black"))return base+"; +12% additional damage";
        if(key.contains("legendary")||key.contains("fullpower"))return base+"; +10% additional damage";
        return base+"; native form multipliers and mastery bonuses also apply";
    }
}