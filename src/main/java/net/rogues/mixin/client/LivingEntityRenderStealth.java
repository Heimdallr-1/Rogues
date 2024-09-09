package net.rogues.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.rogues.effect.Effects;
import net.spell_engine.api.effect.Synchronized;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRenderStealth<T extends Entity> extends EntityRenderer<T> {
    protected LivingEntityRenderStealth(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Unique
    private ClientPlayerEntity localPlayer() {
        return MinecraftClient.getInstance().player;
    }

    @Unique
    private boolean hasStealthEffect(LivingEntity entity) {
        // Checking the regular way (hasStealthEffect(entity)) does not work
        // probably due to some threading or tick order related weirdness.
        var effects = ((Synchronized.Provider)entity).SpellEngine_syncedStatusEffects();
        for (var effect : effects) {
            if (effect.effect() == Effects.STEALTH.effect) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean visibleForLocalPlayer(LivingEntity entity) {
        return entity == localPlayer() || !entity.isInvisibleTo(localPlayer());
    }

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void getRenderLayer_HEAD_Stealth(LivingEntity entity, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderLayer> cir) {
        if (hasStealthEffect(entity) && visibleForLocalPlayer(entity)) {
            Identifier identifier = ((EntityRenderer<T>)this).getTexture((T)entity);
            var layer = RenderLayer.getItemEntityTranslucentCull(identifier);
            cir.setReturnValue(layer);
            cir.cancel();
        }
    }

//    @WrapOperation(
//            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V")
//    )
//    private void modelRender_WRAP_STEALTH(
//            // Mixin parameters
//            EntityModel instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int light, int overlayIndex, float red, float green, float blue, float alpha, Operation<Void> original,
//            // Context parameters
//            LivingEntity entity, float f, float g, MatrixStack contextMatrixStack, VertexConsumerProvider contextVertexConsumerProvider, int contextLight
//    ) {
//        if (hasStealthEffect(entity) && visibleForLocalPlayer(entity)) {
//            original.call(instance, matrixStack, vertexConsumer, light, overlayIndex, red, green, blue, 0.15F);
//        } else {
//            original.call(instance, matrixStack, vertexConsumer, light, overlayIndex, red, green, blue, alpha);
//        }
//    }

    @WrapWithCondition(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V")
    )
    private boolean featureRenderer_WRAP_CONDITION_STEALTH(FeatureRenderer instance, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T t, float a, float b, float c, float d, float e, float f) {
        var entity = (LivingEntity) t;
        if (hasStealthEffect(entity)) {
            if (visibleForLocalPlayer(entity)) {
                // Render held items only
                return instance instanceof HeldItemFeatureRenderer<?, ?>;
            } else {
                return false;
            }
        }
        return true;
    }
}
