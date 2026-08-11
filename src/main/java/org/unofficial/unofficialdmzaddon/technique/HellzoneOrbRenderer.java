package org.unofficial.unofficialdmzaddon.technique;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class HellzoneOrbRenderer extends EntityRenderer<HellzoneOrbEntity> {
    public HellzoneOrbRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public void render(HellzoneOrbEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float pulse = 0.32F + (float)Math.sin((entity.tickCount + partial) * .25F) * .04F;
        Vec3 top = new Vec3(0,pulse,0), bottom = new Vec3(0,-pulse,0), east = new Vec3(pulse,0,0), west = new Vec3(-pulse,0,0), north = new Vec3(0,0,-pulse), south = new Vec3(0,0,pulse);
        VertexConsumer c = buffers.getBuffer(RenderType.lightning()); Matrix4f m = pose.last().pose();
        Vec3[] ring = {east,north,west,south};
        for (int i=0;i<4;i++) { Vec3 a=ring[i], b=ring[(i+1)%4]; TriBeamRenderer.tri(c,m,top,a,b,255,239,70,235); TriBeamRenderer.tri(c,m,b,a,bottom,247,170,34,225); }
        super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(HellzoneOrbEntity entity) { return InventoryMenu.BLOCK_ATLAS; }
}
