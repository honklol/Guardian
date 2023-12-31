/*
 * Guardian for Bukkit and Spigot.
 * Copyright (c) 2012-2015 AntiCheat Team
 * Copyright (c) 2016-2023 Rammelkast
 * Copyright (c) 2023-2023 honklol
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

package com.honklol.guardian.config.holders.yaml;

import java.util.List;
import java.util.UUID;

import com.honklol.guardian.Guardian;
import com.honklol.guardian.config.Configuration;
import com.honklol.guardian.config.providers.Levels;
import com.honklol.guardian.config.ConfigurationFile;
import com.honklol.guardian.util.User;

public class YamlLevelsHolder extends ConfigurationFile implements Levels {

    public static final String FILENAME = "data/levels.yml";

    public YamlLevelsHolder(Guardian plugin, Configuration config) {
        super(plugin, config, FILENAME, false);
    }

    @Override
    public void loadLevelToUser(User user) {
        user.setLevel(getLevel(user.getUUID()));
    }

    @Override
    public void saveLevelFromUser(User user) {
        saveLevelFromUser(user, true);
    }

    private void saveLevelFromUser(User user, boolean remove) {
        saveLevel(user.getUUID(), user.getLevel());
        if (remove) Guardian.getManager().getUserManager().removeUser(user);
    }

    @Override
    public void saveLevelsFromUsers(List<User> users) {
        for (User user : users) {
            saveLevelFromUser(user, false);
        }
    }

    @Override
    public void updateLevelToUser(User user) {
        // This method intentionally left blank.
        return;
    }

    private int getLevel(UUID uuid) {
        ConfigValue<Integer> level = new ConfigValue<Integer>(uuid.toString(), false);
        if (level.hasValue()) {
            return level.getValue();
        } else {
            return 0;
        }
    }

    private void saveLevel(UUID uuid, int level) {
        new ConfigValue<Integer>(uuid.toString(), false).setValue(level);
    }
}
