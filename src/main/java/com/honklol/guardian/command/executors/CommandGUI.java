package com.honklol.guardian.command.executors;

import static com.honklol.guardian.extras.gui.generators.Primary.createGUI;
import com.honklol.guardian.command.CommandBase;
import com.honklol.guardian.util.Permission;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandGUI extends CommandBase {
    private static final String NAME = "Guardian GUI";
    private static final String COMMAND = "gui";
    private static final String USAGE = "guardian gui";
    private static final Permission PERMISSION = Permission.SYSTEM_GUI;
    private static final String[] HELP = {
            ChatColor.GRAY + "Use: " + ChatColor.AQUA + "/guardian gui" + ChatColor.GRAY + " to interactively configure Guardian",
    };

    public CommandGUI() {
        super(NAME, COMMAND, USAGE, HELP, PERMISSION);
    }

    @Override
    protected void execute(CommandSender cs, String[] args) {
        if (cs instanceof Player) {
            Player player = (Player) cs;
            player.openInventory(createGUI(player));
        } else {
            cs.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
        }
    }
}