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

package com.honklol.guardian.config.holders.yaml;

import java.util.ArrayList;
import java.util.List;

import com.honklol.guardian.Guardian;
import com.honklol.guardian.config.Configuration;
import com.honklol.guardian.config.providers.Rules;
import com.honklol.guardian.util.rule.Rule;
import com.honklol.guardian.config.ConfigurationFile;

public class YamlRulesHolder extends ConfigurationFile implements Rules {

    public static final String FILENAME = "rules.yml";

    private List<Rule> rules;

    public YamlRulesHolder(Guardian plugin, Configuration config) {
        super(plugin, config, FILENAME);
    }

    @Override
    public List<Rule> getRules() {
        return rules;
    }

    @Override
    public void open() {
        ConfigValue<List<String>> rules = new ConfigValue<List<String>>("rules");

        // Convert rules list to Rules
        this.rules = new ArrayList<Rule>();
        List<String> tempRules = rules.getValue();
        for (int i = 0; i < tempRules.size(); i++) {
            String string = tempRules.get(i);

            if (string.equals("Check_SPIDER < 0 ? Player.KICK : null")) {
                // Default rule, won't ever run so we shouldn't load it; only used as example
                continue;
            }

            Rule rule = Rule.load(string);
            if (rule != null) {
                this.rules.add(rule);
            } else {
                Guardian.getPlugin().getLogger().warning("Couldn't load rule '" + string + "' from config. Improper format used.");
            }
        }
    }
}
