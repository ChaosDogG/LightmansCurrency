package io.github.lightman314.lightmanscurrency.client.renderer.blockentity;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.lightman314.lightmanscurrency.common.blockentity.AuctionStandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class AuctionStandBlockEntityRenderer implements BlockEntityRenderer<AuctionStandBlockEntity> {


    private final ItemRenderer itemRenderer;
    public AuctionStandBlockEntityRenderer(BlockEntityRendererProvider.Context ignored) { this.itemRenderer = Minecraft.getInstance().getItemRenderer(); }

    @Override
    public void render(@Nonnull AuctionStandBlockEntity blockEntity, float partialTicks, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffer, int lightLevel, int id) {

        ImmutableList<ItemStack> displayItems = AuctionStandBlockEntity.getDisplayItems();
        if(displayItems.size() < 1)
            return;

        pose.pushPose();
        pose.translate(0.5f, 0.75f, 0.5f);
        pose.mulPose(ItemTraderBlockEntityRenderer.getRotation(partialTicks));
        pose.scale(0.4f,0.4f,0.4f);

        if(displayItems.size() < 2)
        {
            //Only render 1 item
            this.itemRenderer.renderStatic(displayItems.get(0),  ItemTransforms.TransformType.FIXED, lightLevel, OverlayTexture.NO_OVERLAY, pose, buffer, id);
        }
        else
        {
            //Render Item 1
            pose.pushPose();
            pose.translate(-0.55f,0f,0f);
            this.itemRenderer.renderStatic(displayItems.get(0),  ItemTransforms.TransformType.FIXED, lightLevel, OverlayTexture.NO_OVERLAY, pose, buffer, id);
            pose.popPose();

            //Render Item 2
            pose.pushPose();
            pose.translate(0.55f, 0f, 0f);
            this.itemRenderer.renderStatic(displayItems.get(1),  ItemTransforms.TransformType.FIXED, lightLevel, OverlayTexture.NO_OVERLAY, pose, buffer, id);
            pose.popPose();
        }
        pose.popPose();

    }

}