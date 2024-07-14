package com.massivecraft.factions.util.timer.type;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.util.timer.GlobalTimer;
import com.massivecraft.factions.zcore.file.CustomFile;
import org.bukkit.event.Listener;

import java.util.concurrent.TimeUnit;


public class WarTimer extends GlobalTimer implements Listener {

    public WarTimer() {
        super("WAR", TimeUnit.MINUTES.toMillis(Conf.warPeriodTimeMinutes));
    }

    @Override
    public void load(CustomFile config) {
        setPaused(config.getConfig().getBoolean(this.name + ".paused"));
        setRemaining(config.getConfig().getLong(this.name + ".time"), false);
    }

    @Override
    public void save(CustomFile config) {
        config.getConfig().set(this.name + ".paused", isPaused());
        config.getConfig().set(this.name + ".time", getRemaining());
    }
}
