package me.vermulst.multibreak.multibreak.event;

import me.vermulst.multibreak.multibreak.BreakManager;
import me.vermulst.multibreak.multibreak.MultiBreak;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

public class SpeedChangeEvents implements Listener {

    private final BreakManager breakManager;
    public SpeedChangeEvents(BreakManager breakManager) {
        this.breakManager = breakManager;
    }

    @EventHandler
    public void onPotionEffectChange(EntityPotionEffectEvent e) {
        if (e.getAction() == EntityPotionEffectEvent.Action.CHANGED) return;
        if (!(e.getEntity() instanceof Player p)) return;

        PotionEffectType type = e.getModifiedType();
        if (!type.equals(PotionEffectType.HASTE) &&
                !type.equals(PotionEffectType.MINING_FATIGUE) &&
                !type.equals(PotionEffectType.CONDUIT_POWER)) return;
        MultiBreak multiBreak = breakManager.getMultiBreakOffstate(p);
        if (multiBreak != null) {
            multiBreak.invalidateDestroySpeedCache();
        }
    }
}
