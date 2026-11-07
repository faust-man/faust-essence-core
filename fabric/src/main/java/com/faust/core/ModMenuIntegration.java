package com.faust.core;
import com.faust.core.client.CoreConfigScreen;
import com.terraformersmc.modmenu.api.*;
public final class ModMenuIntegration implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory(){return CoreConfigScreen::new;}
}
