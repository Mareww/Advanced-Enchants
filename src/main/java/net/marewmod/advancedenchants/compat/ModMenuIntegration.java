package net.marewmod.advancedenchants.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.marewmod.advancedenchants.AdvancedEnchantsConfigScreen;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AdvancedEnchantsConfigScreen::new;
    }
}
