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

/** Draws a real triangular Ki volume from its firing point to its moving leading edge. */
public final class TriBeamRenderer extends EntityRenderer<TriBeamEntity> {
    public TriBeamRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public void render(TriBeamEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 current = entity.getPosition(partial);
        Vec3 start = entity.getOrigin().subtract(current);
        Vec3 axis = start.scale(-1);
        if (axis.lengthSqr() < 0.001D) return;
        Vec3 direction = axis.normalize();
        Vec3 helper = Math.abs(direction.y) < 0.9D ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = direction.cross(helper).normalize();
        Vec3 v = direction.cross(u).normalize();
        double width = entity.getVisualWidth();
        Vec3[] back = ring(start, u, v, 0.12D);
        Vec3[] front = ring(Vec3.ZERO, u, v, width);
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = pose.last().pose();
        for (int i = 0; i < 3; i++) {
            int next = (i + 1) % 3;
            quad(consumer, matrix, back[i], front[i], front[next], back[next], 255, 238, 118, 210);
            quad(consumer, matrix, back[next], front[next], front[i], back[i], 255, 255, 232, 150);
        }
        tri(consumer, matrix, front[0], front[1], front[2], 255, 251, 210, 235);
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private static Vec3[] ring(Vec3 center, Vec3 u, Vec3 v, double radius) {
        Vec3[] result = new Vec3[3];
        for (int i = 0; i < 3; i++) {
            double angle = -Math.PI / 2D + i * Math.PI * 2D / 3D;
            result[i] = center.add(u.scale(Math.cos(angle) * radius)).add(v.scale(Math.sin(angle) * radius));
        }
        return result;
    }
    static void vertex(VertexConsumer c, Matrix4f m, Vec3 p, int r, int g, int b, int a) { c.vertex(m, (float)p.x, (float)p.y, (float)p.z).color(r,g,b,a).endVertex(); }
    static void quad(VertexConsumer c, Matrix4f m, Vec3 a, Vec3 b, Vec3 d, Vec3 e, int r, int g, int bl, int al) { vertex(c,m,a,r,g,bl,al); vertex(c,m,b,r,g,bl,al); vertex(c,m,d,r,g,bl,al); vertex(c,m,e,r,g,bl,al); }
    static void tri(VertexConsumer c, Matrix4f m, Vec3 a, Vec3 b, Vec3 d, int r, int g, int bl, int al) { vertex(c,m,a,r,g,bl,al); vertex(c,m,b,r,g,bl,al); vertex(c,m,d,r,g,bl,al); }
    @Override public ResourceLocation getTextureLocation(TriBeamEntity entity) { return InventoryMenu.BLOCK_ATLAS; }
}
