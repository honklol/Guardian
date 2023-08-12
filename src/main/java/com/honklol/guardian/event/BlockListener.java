/*
 * Guardian for Bukkit and Spigot.
 * Copyright (c) 2012-2015 AntiCheat Team
 * Copyright (c) 2016-2022 Rammelkast
 * Copyright (c) 2022-2023 honklol
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.honklol.guardian.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.honklol.guardian.Guardian;
import com.honklol.guardian.check.CheckResult;
import com.honklol.guardian.check.CheckType;
import com.honklol.guardian.check.player.IllegalInteractCheck;

public final class BlockListener extends EventListener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        if (getCheckManager().willCheck(player, CheckType.FAST_PLACE)) {
            final CheckResult result = getBackend().checkFastPlace(player);
            if (result.failed()) {
                event.setCancelled(!silentMode());
                log(result.getMessage(), player, CheckType.FAST_PLACE, result.getSubCheck());
                return;
            } 
        }
        
        if (getCheckManager().willCheck(player, CheckType.ILLEGAL_INTERACT)) {
            final CheckResult result = IllegalInteractCheck.runCheck(player, event);
            if (result.failed()) {
                event.setCancelled(!silentMode());
                log(result.getMessage(), player, CheckType.ILLEGAL_INTERACT, result.getSubCheck());
                return;
            } 
        }
        
        decrease(player);
        getBackend().logBlockPlace(player);
        Guardian.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (getCheckManager().willCheck(player, CheckType.ILLEGAL_INTERACT)) {
            final CheckResult result = IllegalInteractCheck.runCheck(player, event);
            if (result.failed()) {
                event.setCancelled(!silentMode());
                log(result.getMessage(), player, CheckType.ILLEGAL_INTERACT, result.getSubCheck());
               	return;
            }
        }
        
        decrease(player);
    }
}
