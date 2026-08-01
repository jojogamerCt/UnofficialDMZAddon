package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


/** Universal form baseline plus distinct thematic specialties for every form family. */
public final class FormSpecialBuffHandler {
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
            if(key.isBlank())return;
            float regen=0.0015f;
            if(key.contains("god"))regen=0.010f;else if(key.contains("blue"))regen=0.006f;else if(key.contains("ui")||key.contains("ultrainstinct"))regen=0.004f;else if(key.contains("metal"))regen=0.008f;
            data.getResources().addEnergy(data.getMaxEnergy()*regen);
        });
    }

    private static String formKey(String group,String form){return ((group==null?"":group)+"."+(form==null?"":form)).toLowerCase();}
}