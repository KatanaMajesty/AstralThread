package com.astralsmp.modules;

import java.util.HashMap;
import java.util.Map;

public class DiscordCooldownManager {

    private final Map<String, Long> cooldowns = new HashMap<>();
    public static final int DEFAULT_COOLDOWN = 120;

    public void setCooldown(String discordID, long cooldown) {
        if (cooldown < 1) cooldowns.remove(discordID);
        else cooldowns.put(discordID, cooldown);
    }

    public long getCooldown(String discordID) {
        return cooldowns.getOrDefault(discordID, 0L);
    }

}
