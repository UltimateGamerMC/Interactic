package interactic;

import interactic.util.InteracticNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ItemFilterScreen extends AbstractContainerScreen<ItemFilterScreenHandler> {

    private static final Identifier TEXTURE = InteracticInit.id("textures/gui/item_filter.png");

    public boolean blockMode = true;

    private Button blockButton;
    private Button allowButton;

    public ItemFilterScreen(ItemFilterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 178);
        this.inventoryLabelY = 69420;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.blockButton = this.addRenderableWidget(
                Button.builder(Component.literal("Block"), b -> sendModeRequest(true))
                        .bounds(this.leftPos + 43, this.topPos + 78, 60, 12)
                        .build()
        );
        this.allowButton = this.addRenderableWidget(
                Button.builder(Component.literal("Allow"), b -> sendModeRequest(false))
                        .bounds(this.leftPos + 108, this.topPos + 78, 60, 12)
                        .build()
        );
    }

    private static void sendModeRequest(boolean mode) {
        InteracticNetworking.CHANNEL.clientHandle().send(new InteracticNetworking.FilterModeRequest(mode));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, "Mode", this.leftPos + 8, this.topPos + 80, 0x404040, false);
        if (this.blockButton != null) {
            this.blockButton.active = !this.blockMode;
            this.allowButton.active = this.blockMode;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if (!this.blockMode) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo + 7, yo + 19, 0.0F, 178.0F, 162, 54, 256, 256);
        }
    }
}
