package interactic;

import interactic.util.InteracticConfig;
import interactic.util.ServerSideConfigOption;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Vanilla-style options screen for the boolean config options (opened from Mod Menu). */
public class InteracticConfigScreen extends OptionsSubScreen {

    protected InteracticConfigScreen(@Nullable Screen parent) {
        super(parent, net.minecraft.client.Minecraft.getInstance().options, Component.translatable("text.config.interactic.title"));
    }

    @Override
    protected void addOptions() {
        InteracticConfig config = InteracticInit.getConfig();
        for (Field field : InteracticConfig.class.getFields()) {
            if (field.getType() != boolean.class || Modifier.isStatic(field.getModifiers())) continue;
            if (config.clientOnlyMode() && field.isAnnotationPresent(ServerSideConfigOption.class)) continue;
            String key = "text.config.interactic.option." + field.getName();
            Component tooltip = Component.translatable(key + ".tooltip");
            if (field.isAnnotationPresent(InteracticConfig.RestartRequiredOption.class)) {
                tooltip = tooltip.copy().append(Component.literal("\n").append(Component.translatable("text.config.interactic.restart")));
            }
            try {
                this.list.addSmall(OptionInstance.createBoolean(key, OptionInstance.cachedConstantTooltip(tooltip), field.getBoolean(config), value -> {
                    try {
                        field.setBoolean(config, value);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void removed() {
        InteracticInit.getConfig().save();
        super.removed();
    }
}
