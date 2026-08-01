package cz.nekara.rpg.modules;

import cz.nekara.rpg.configuration.PluginConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleRegistry {
    private final Map<String, NekaraModule> modules = new LinkedHashMap<>();

    public void register(NekaraModule module) {
        modules.put(module.id(), module);
    }

    public void applyConfig(PluginConfig config) {
        for (NekaraModule module : modules.values()) {
            boolean shouldBeEnabled = config.isModuleEnabled(module.id());
            if (shouldBeEnabled && !module.isEnabled()) {
                module.enable();
            } else if (!shouldBeEnabled && module.isEnabled()) {
                module.disable();
            } else if (shouldBeEnabled) {
                module.reload();
            }
        }
    }

    public void disableAll() {
        for (NekaraModule module : modules.values()) {
            if (module.isEnabled()) {
                module.disable();
            }
        }
    }

    public boolean isEnabled(String id) {
        NekaraModule module = modules.get(id);
        return module != null && module.isEnabled();
    }

    public List<String> enabledModuleIds() {
        List<String> ids = new ArrayList<>();
        for (NekaraModule module : modules.values()) {
            if (module.isEnabled()) {
                ids.add(module.id());
            }
        }
        return ids;
    }
}
