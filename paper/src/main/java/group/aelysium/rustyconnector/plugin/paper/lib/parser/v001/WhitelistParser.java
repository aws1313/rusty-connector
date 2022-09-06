package group.aelysium.rustyconnector.plugin.paper.lib.parser.v001;

import com.google.gson.Gson;
import group.aelysium.rustyconnector.core.lib.generic.firewall.Whitelist;
import group.aelysium.rustyconnector.core.lib.generic.firewall.WhitelistPlayer;
import group.aelysium.rustyconnector.plugin.paper.PaperRustyConnector;
import group.aelysium.rustyconnector.plugin.paper.lib.generic.Config;
import ninja.leaping.configurate.ConfigurationNode;

import java.io.File;
import java.util.List;

public class WhitelistParser {
    public static boolean parse(String configName) {
        PaperRustyConnector plugin = PaperRustyConnector.getInstance();

        try {
            plugin.logger().log("-----------| Loading whitelist: "+configName+"...");

            Config whitelistConfig = new Config(new File(plugin.getDataFolder(), "whitelists/"+configName+".yml"), "paper_whitelist_template.yml");
            if(!whitelistConfig.register()) throw new RuntimeException("Unable to register "+configName+".yml");

            plugin.logger().log("-----------| Finished!");

            plugin.logger().log("-----------| Getting whitelist criteria...");
            ConfigurationNode configData = whitelistConfig.getData();
            boolean usePlayers = configData.getNode("use-players").getBoolean();
            boolean usePermission = configData.getNode("use-permission").getBoolean();
            boolean useCountry = configData.getNode("use-country").getBoolean();

            Whitelist whitelist = new Whitelist(configName, usePlayers, usePermission, useCountry);

            plugin.logger().log("-----------| Processing whitelist criteria...");
            if(usePlayers) WhitelistParser.parsePlayers(configData, whitelist);
            if(useCountry) WhitelistParser.parseCountries(configData, whitelist);

            plugin.getVirtualServer().registerWhitelist(whitelist);

            plugin.logger().log("-----------| Finished loading server whitelist!");
            return true;
        } catch (NullPointerException e) {
            plugin.logger().log("Unable to register the whitelist: "+configName);
            plugin.logger().error("One of the data types provided in this family's config is invalid and not what was expected!",e);
        } catch (Exception e) {
            plugin.logger().error("Unable to register the whitelist: "+configName, e);
        }
        return false;
    }

    public static void parsePlayers(ConfigurationNode configData, Whitelist whitelist) {
        PaperRustyConnector plugin = PaperRustyConnector.getInstance();

        plugin.logger().log("-------------| Getting players...");

        try {
            List<Object> players = (List<Object>) configData.getNode("players").getValue();
            Gson gson = new Gson();
            players.forEach(entry -> {
                String json = gson.toJson(entry);
                WhitelistPlayer player = gson.fromJson(json, WhitelistPlayer.class);

                whitelist.registerPlayer(player);
            });

            plugin.logger().log("-------------| Finished!");
        } catch (Exception e) {
            throw new RuntimeException("There was an issue registering the players for the whitelist! Is your config properly formatted?");
        }
    }

    public static void parseCountries(ConfigurationNode configData, Whitelist whitelist) {
        PaperRustyConnector plugin = PaperRustyConnector.getInstance();

        plugin.logger().log("-------------| Getting players...");

        try {
            List<String> countries = (List<String>) configData.getNode("countries").getValue();
            countries.forEach(whitelist::registerCountry);

            plugin.logger().log("-------------| Finished!");
        } catch (Exception e) {
            throw new RuntimeException("There was an issue registering the players for the whitelist! Is your config properly formatted?");
        }
    }
}
