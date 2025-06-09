package com.massivecraft.factions.cmd;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.war.War;
import com.massivecraft.factions.zcore.util.TL;

import java.util.ArrayList;

public class CmdWarList extends FCommand {

    /**
     * @author Kram
     */

    public CmdWarList() {
        super();
        this.aliases.addAll(Aliases.warlist);
        this.requirements = new CommandRequirements.Builder(Permission.WAR)
                .playerOnly()
                .build();
    }

    @Override
    public void perform(CommandContext context) {
//        int min_connected = FactionsPlugin.getInstance().getConfig().getInt("war.start_conditions.min_connected");
        // Check if their faction exists

        // Can stop if faction at war
        ArrayList<War> warList = FactionsPlugin.getInstance().getWarList();
        if (warList.isEmpty()) {
            context.msg("No wars found.");
            return;
        }
        for (War war : warList) {
            context.msg(war.getMainAttackerTag() + " attacking " + war.getMainDefenderTag() + " time remaining to next phase : "+war.getHumanizedTimeToNextPhase());
        }

    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_WAR_DESCRIPTION;
    }


}
