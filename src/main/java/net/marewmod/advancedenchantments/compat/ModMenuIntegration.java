package net.marewmod.advancedenchantments.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.marewmod.advancedenchantments.AdvancedEnchantmentsConfigScreen;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AdvancedEnchantmentsConfigScreen::new;
    }
}
