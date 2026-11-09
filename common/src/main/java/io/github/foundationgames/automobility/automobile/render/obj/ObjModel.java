package io.github.foundationgames.automobility.automobile.render.obj;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.foundationgames.automobility.automobile.model.ModelDefinition;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class ObjModel extends Model {
    private static final float PIXEL = 1.0f / 16.0f;
    // Animation pivots are expressed in the imported OBJ's raw coordinates;
    // renderToBuffer applies the model-definition transform after these poses.
    private static final Vector3f HARLEY_FRONT_PIVOT = new Vector3f(8.3f * PIXEL, 5.71f * PIXEL, 22.3f * PIXEL);
    private static final Vector3f HARLEY_REAR_PIVOT = new Vector3f(8.3f * PIXEL, 5.71f * PIXEL, -6.22f * PIXEL);

    private final Vector3f translation;
    private final Vector3f rotation;
    private final Vector3f scale;
    private final boolean vehicleParts;

    private final Supplier<BakedObj> obj;
    private float steering;
    private float wheelAngle;

    public ObjModel(EntityRendererProvider.Context ctx,
                     ModelDefinition.RenderMaterial material,
                     ModelLayerLocation layer,
                     Vector3f translation, Vector3f rotation, Vector3f scale) {
        super(material.renderType);
        this.translation = translation;
        this.rotation = rotation;
        this.scale = scale;
        this.vehicleParts = layer.getModel().getPath().equals("automobile/frame/harley");

        this.obj = ObjLoader.INSTANCE.getObj(layer);
    }

    public boolean ownsVehicleParts() {
        return vehicleParts;
    }

    public void setVehicleState(float steering, float wheelAngle) {
        this.steering = steering;
        this.wheelAngle = wheelAngle;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        var obj = this.obj.get();
        if (obj == null) {
            return;
        }

        poseStack.pushPose();

        poseStack.translate(translation.x(), translation.y(), translation.z());
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x()));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y()));
        poseStack.scale(scale.x(), scale.y(), scale.z());
        if (vehicleParts) {
            renderVehicle(obj, poseStack, buffer, packedLight, packedOverlay, color);
        } else {
            for (var part : obj.parts()) {
                renderPart(obj, part, poseStack, buffer, packedLight, packedOverlay, color);
            }
        }

        poseStack.popPose();
    }

    private void renderVehicle(BakedObj obj, PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay, int color) {
        var body = findPart(obj, "body_static");
        var frontWheel = findPart(obj, "wheel_front");
        var rearWheel = findPart(obj, "wheel_rear");
        var steeringAssembly = findPart(obj, "steering");

        if (body == null || frontWheel == null || rearWheel == null || steeringAssembly == null) {
            for (var part : obj.parts()) {
                renderPart(obj, part, poseStack, buffer, packedLight, packedOverlay, color);
            }
            return;
        }

        renderPart(obj, body, poseStack, buffer, packedLight, packedOverlay, color);

        poseStack.pushPose();
        rotateAround(poseStack, HARLEY_FRONT_PIVOT, steering * 27.0f, 0.0f);
        renderPart(obj, steeringAssembly, poseStack, buffer, packedLight, packedOverlay, color);
        poseStack.popPose();

        poseStack.pushPose();
        rotateAround(poseStack, HARLEY_FRONT_PIVOT, steering * 27.0f, wheelAngle);
        renderPart(obj, frontWheel, poseStack, buffer, packedLight, packedOverlay, color);
        poseStack.popPose();

        poseStack.pushPose();
        rotateAround(poseStack, HARLEY_REAR_PIVOT, 0.0f, wheelAngle);
        renderPart(obj, rearWheel, poseStack, buffer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }

    private static void rotateAround(PoseStack poseStack, Vector3f pivot, float yaw, float roll) {
        poseStack.translate(pivot.x(), pivot.y(), pivot.z());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(roll));
        poseStack.translate(-pivot.x(), -pivot.y(), -pivot.z());
    }

    private static BakedObj.Part findPart(BakedObj obj, String name) {
        return obj.parts().stream().filter(part -> part.name().equals(name)).findFirst().orElse(null);
    }

    private static void renderPart(BakedObj obj, BakedObj.Part part, PoseStack poseStack, VertexConsumer buffer,
                                   int packedLight, int packedOverlay, int color) {
        var pose = poseStack.last();
        var vtx = new Vector3f();
        var nml = new Vector3f();
        var uv = new Vector2f();
        int[] idVertex = part.idVertex();
        int[] idNormal = part.idNormal();
        int[] idUv = part.idUv();

        for (int i = 0; i < idVertex.length; i++) {
            int iv = idVertex[i] * 3;
            int in = idNormal[i] * 3;
            int iu = idUv[i] * 2;

            vtx.set(obj.vertex()[iv], obj.vertex()[iv + 1], obj.vertex()[iv + 2]);
            nml.set(obj.normal()[in], obj.normal()[in + 1], obj.normal()[in + 2]);
            uv.set(obj.uv()[iu], 1.0 - obj.uv()[iu + 1]);

            buffer.addVertex(pose, vtx.x(), vtx.y(), vtx.z())
                    .setColor(color)
                    .setUv(uv.x(), uv.y())
                    .setOverlay(packedOverlay)
                    .setLight(packedLight)
                    .setNormal(pose, nml.x(), nml.y(), nml.z());
        }
    }
}
