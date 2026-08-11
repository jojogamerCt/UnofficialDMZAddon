package org.unofficial.unofficialdmzaddon.technique;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class EnergyBladeRenderer extends EntityRenderer<EnergyBladeEntity> {
    public EnergyBladeRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public void render(EnergyBladeEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(-entity.getYRot())); pose.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        VertexConsumer c=buffers.getBuffer(RenderType.lightning()); Matrix4f m=pose.last().pose();
        Vec3 base=new Vec3(0,0,0), tip=new Vec3(0,0,3.2), l=new Vec3(-.18,0,.18), r=new Vec3(.18,0,.18), u=new Vec3(0,.18,.18), d=new Vec3(0,-.18,.18);
        TriBeamRenderer.tri(c,m,base,l,tip,255,75,224,220); TriBeamRenderer.tri(c,m,base,tip,r,255,188,255,235);
        TriBeamRenderer.tri(c,m,base,u,tip,255,75,224,220); TriBeamRenderer.tri(c,m,base,tip,d,255,188,255,235);
        pose.popPose(); super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(EnergyBladeEntity entity){return InventoryMenu.BLOCK_ATLAS;}
}
