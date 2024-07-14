package com.massivecraft.factions.event;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Relation;
import org.bukkit.event.Cancellable;

public class FactionWarEvent extends FactionPlayerEvent implements Cancellable {
    private final Faction targetFaction;
    private boolean cancelled;

    public FactionWarEvent(FPlayer caller, Faction sender, Faction targetFaction) {
        super(sender, caller);

        this.targetFaction = targetFaction;
    }

    public Faction getTargetFaction() {
        return targetFaction;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
