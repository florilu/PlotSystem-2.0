import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import util.ListManager;
import util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Florian on 03.06.14.
 */
public class BuyGS extends JavaPlugin{

    public final String PLUGIN_NAME = "§6[§2Florilucraft Plot System§6]§f ";

    private Economy econ;
    private FileConfiguration config = null;

    private File configFile;
    private File plotNames;

    private Utils utils;

    private ArrayList<String> plotNamesList = new ArrayList<>();
    private ArrayList<Plot> plotsFromConfig = new ArrayList<>();

    private ListManager listManager;

    public void onEnable(){

        utils = new Utils();
        listManager = new ListManager(this.getWorldGuard(), this.getLogger());

        setupEconomy();

        this.configFile = new File(getDataFolder(), "config.yml");
        this.plotNames = new File(getDataFolder(), "plotNames.txt");

        if(!configFile.exists()){
            configFile.getParentFile().mkdirs();
            utils.copy(getResource("config.yml"), configFile);
        }
        if(!plotNames.exists()){
            plotNames.getParentFile().mkdirs();
            utils.copy(getResource("plotNames.txt"), plotNames);
        }

        config = new YamlConfiguration();
        loadYamls();

        try{
            BufferedReader reader = new BufferedReader(new FileReader(plotNames));
            String line;
            while((line = reader.readLine()) != null){
                plotNamesList.add(line);
            }

            for(int i = 0; i < plotNamesList.size(); i++){
                this.getLogger().info(plotNamesList.get(i));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        for(int i = 0; i < plotNamesList.size(); i++){
            Plot plot = new Plot(plotNamesList.get(i),
                    config.get("plots." + plotNamesList.get(i) + ".identification").toString(),
                    config.get("plots." + plotNamesList.get(i) + ".costs").toString(),
                    config.get("plots." + plotNamesList.get(i) + ".permission").toString());
            plotsFromConfig.add(plot);

            this.listManager.addListType(plotNamesList.get(i), config.get("plots." + plotNamesList.get(i) + ".identification").toString());
        }

        this.listManager.runIdentification(Bukkit.getWorld("world"));

        this.getLogger().info(PLUGIN_NAME + " (FCPS) started!");
    }

    public void onDisable(){
        this.getLogger().info(PLUGIN_NAME + " (FCPS) stopped!");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String args[]){

        Player player = (Player) sender;

        if(cmd.getName().equalsIgnoreCase("listgs")){
            if(args.length == 1){
                this.listManager.runIdentification(player.getWorld());

                for(int i = 0; i < listManager.getLists().size(); i++){
                    if(args[0].equalsIgnoreCase(listManager.getLists().get(i).plotType)){
                        String list = PLUGIN_NAME;
                        for(int j = 0; j < listManager.getLists().get(i).mainList.size(); j++){
                            list = list + listManager.getLists().get(i).mainList.get(j);
                        }
                        sender.sendMessage(list);
                    }
                }
                return true;
            }else if(args.length > 1){
                sender.sendMessage(PLUGIN_NAME + "zu wenige Parameter!");
                return false;
            }else{
                sender.sendMessage(PLUGIN_NAME + "zu viele Parameter!");
                return false;
            }
        }

        if(cmd.getName().equalsIgnoreCase("buygs")){
            if(args.length == 1){
                String plotID = args[0];
                String plotType = checkPlotType(plotID);
                DefaultDomain domain = new DefaultDomain();
                domain.addPlayer(player.getName());
                if(plotType != null){
                    for(int i = 0; i < plotsFromConfig.size(); i++){
                        Plot plotFC = plotsFromConfig.get(i);
                        if(plotType.equalsIgnoreCase(plotFC.getPlotType())){
                            if(player.hasPermission(plotFC.getPermission())){
                                if(!alreadyHasPlot(player)){
                                    if(!hasOwner(plotID, player)){
                                        if(plotFC.getPlotCost() > 0){
                                            if(econ.getBalance(player.getName()) >= plotFC.getPlotCost()){
                                                EconomyResponse r = econ.withdrawPlayer(player.getName(), plotFC.getPlotCost());
                                                if(r.transactionSuccess()){
                                                    this.getWorldGuard().getRegionManager(player.getWorld()).getRegion(plotID).setOwners(domain);
                                                    saveWorld(player);
                                                    sender.sendMessage(PLUGIN_NAME + "dir wurde erfolgreich das GS " + plotID + " zugewiesen, dein Konto wurde um " + r.amount + econ.currencyNameSingular() + " belastet!");
                                                    return true;
                                                }else{
                                                    sender.sendMessage(PLUGIN_NAME + "leider ist beim Kauf etwas schief gelaufen! " + r.errorMessage);
                                                    return true;
                                                }
                                            }else{
                                                double currentBalance = econ.getBalance(player.getName());
                                                int plotCost = plotFC.getPlotCost();
                                                double need = (currentBalance - plotCost) * -1;
                                                sender.sendMessage(PLUGIN_NAME + "du hast nicht genügend Geld auf dem Konto, du brauchst " + need + econ.currencyNameSingular() + " mehr!");
                                            }
                                        }else{
                                            this.getWorldGuard().getRegionManager(player.getWorld()).getRegion(plotID).setOwners(domain);
                                            saveWorld(player);
                                            sender.sendMessage(PLUGIN_NAME + "dir wurde erfolgreich das GS " + plotID + " zugewiesen!");
                                        }
                                    }else{
                                        sender.sendMessage(PLUGIN_NAME + "dieses Grundstück gehört bereits Jemanden!");
                                        return false;
                                    }
                                }else{
                                    sender.sendMessage(PLUGIN_NAME + "du besitzt bereits ein oder mehrere Grundstücke!");
                                }
                            }else{
                                sender.sendMessage(PLUGIN_NAME + "du hast keine Berechtigung dir dieses Grundstück zu kaufen!");
                                return false;
                            }
                        }
                    }
                }

                return true;
            }else if(args.length > 1){
                sender.sendMessage(PLUGIN_NAME + "zu wenige Parameter!");
                return false;
            }else{
                sender.sendMessage(PLUGIN_NAME + "zu viele Parameter!");
                return false;
            }
        }

        if(cmd.getName().equalsIgnoreCase("sellgs")){
            ProtectedRegion region = lookForOwnedPlot(player);
            DefaultDomain domain = region.getOwners();
            domain.removaAll();
            getWorldGuard().getRegionManager(player.getWorld()).getRegion(region.getId()).setOwners(domain);
            saveWorld(player);
            String plotType = this.checkPlotType(region.getId());
            double plotCost = 0;
            for(int i = 0; i < plotsFromConfig.size(); i++){
                Plot plotFC = plotsFromConfig.get(i);
                if(plotFC.getPlotType().equalsIgnoreCase(checkPlotType(region.getId()))){
                    plotCost = plotFC.getPlotCost();
                    break;
                }
            }
            if(plotCost > 0){
                econ.depositPlayer(player.getName(), plotCost);
                sender.sendMessage(PLUGIN_NAME + "du hast erfolgreich dein " + plotType + " GS für " + plotCost + econ.currencyNameSingular() + " verkauft!");
                return true;
            }else{
                sender.sendMessage(PLUGIN_NAME + "du hast erfolgreich dein" + plotType + " GS abgegeben!");
                return true;
            }
        }

        if(cmd.getName().equalsIgnoreCase("adduser")){
            if(args.length < 1){
                sender.sendMessage(PLUGIN_NAME + "Zu wenige Parameter!");
                return false;
            }else if(args.length > 1){
                sender.sendMessage(PLUGIN_NAME + "Zu viele Parameter!");
                return false;
            }else{
                ProtectedRegion region = lookForOwnedPlot(player);
                DefaultDomain domain = new DefaultDomain();
                domain.addPlayer(args[0]);
                region.setMembers(domain);
                saveWorld(player);
                sender.sendMessage(PLUGIN_NAME + "der User " + args[0] + " kann nun auf deinem GS mit der ID " + region.getId() + " bauen!");
                return true;
            }
        }

        if(cmd.getName().equalsIgnoreCase("removeuser")){
            if(args.length < 1){
                sender.sendMessage(PLUGIN_NAME + "Zu wenige Parameter!");
                return false;
            }else if(args.length > 1){
                sender.sendMessage(PLUGIN_NAME + "Zu viele Parameter!");
                return false;
            }else{
                ProtectedRegion region = lookForOwnedPlot(player);
                DefaultDomain domain = region.getMembers();
                if(domain.contains(args[0])){
                    domain.removePlayer(args[0]);
                    saveWorld(player);
                    sender.sendMessage(PLUGIN_NAME + "Der Spieler " + args[0] + " kann nun nicht mehr auf deinem Grundstück bauen!");
                    return true;
                }else{
                    sender.sendMessage(PLUGIN_NAME + "Der Spieler mit dem Namen " + args[0] + " existiert nicht!");
                    return true;
                }
            }
        }

        if(cmd.getName().equalsIgnoreCase("removeowner")){
            if(args.length < 1){
                sender.sendMessage(PLUGIN_NAME + "Zu wenige Parameter!");
                return false;
            }else if(args.length > 1){
                sender.sendMessage(PLUGIN_NAME + "Zu viele Parameter!");
                return false;
            }else{
                ArrayList<ProtectedRegion> plots = getPlots(player);
                for(int i = 0; i < plots.size(); i++){
                    if(plots.get(i).getId().equalsIgnoreCase(args[0])){
                        DefaultDomain domain = plots.get(i).getOwners();
                        DefaultDomain memberDomain = plots.get(i).getMembers();
                        domain.removaAll();
                        memberDomain.removaAll();
                        saveWorld(player);
                        break;
                    }
                }
                sender.sendMessage(PLUGIN_NAME + "Alle Owner und Member vom Grundstück " + args[0] + " wurden erfolgreich entfernt!");
                return true;
            }
        }

        return false;
    }

    private WorldGuardPlugin getWorldGuard(){
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if(plugin == null || !(plugin instanceof  WorldGuardPlugin)){
            return null;
        }
        return (WorldGuardPlugin) plugin;
    }

    private boolean setupEconomy(){
        if(getServer().getPluginManager().getPlugin("Vault") == null){
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if(rsp == null){
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private void saveWorld(Player player){
        try{
            getWorldGuard().getRegionManager(player.getWorld()).save();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void saveYamls(){
        try{
            config.save(configFile);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void loadYamls(){
        try{
            config.load(configFile);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String checkPlotType(String plotID){
        for(int i = 0; i < this.listManager.getLists().size(); i++){
            String listType = this.listManager.getLists().get(i).getListType();
            String listAlias = this.listManager.getLists().get(i).alias;
            if(plotID.length() > listAlias.length()){
                if(!Character.isLetter(plotID.charAt(listAlias.length()))){
                    if(plotID.contains(listAlias)){
                        return listType;
                    }
                }
            }
        }
        return null;
    }

    private boolean hasOwner(String plotID, Player player){
        ArrayList<ProtectedRegion> protectedRegions = new ArrayList<>();
        Map<String, ProtectedRegion> regionStringMap = this.getWorldGuard().getRegionManager(player.getWorld()).getRegions();
        for(Map.Entry<String, ProtectedRegion> entry : regionStringMap.entrySet()){
            protectedRegions.add(entry.getValue());
        }
        for(int i = 0; i < protectedRegions.size(); i++){
            if(protectedRegions.get(i).getId().equalsIgnoreCase(plotID)){
                DefaultDomain domain = protectedRegions.get(i).getOwners();
                if(domain.size() > 0){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean alreadyHasPlot(Player player){
        ArrayList<ProtectedRegion> plots = getPlots(player);
        int ownedPlots = 0;
        for(int i = 0; i < plots.size(); i++){
            DefaultDomain domain = plots.get(i).getOwners();
            if(domain.size() > 0){
                if(domain.contains(player.getName())){
                    ownedPlots++;
                }
            }
            if(ownedPlots > 0){
                return true;
            }
        }
        return false;
    }

    private ArrayList<ProtectedRegion> getPlots(Player player){
        ArrayList<ProtectedRegion> protectedRegions = new ArrayList<>();
        Map<String, ProtectedRegion> regionStringMap = this.getWorldGuard().getRegionManager(player.getWorld()).getRegions();
        for(Map.Entry<String, ProtectedRegion> entry : regionStringMap.entrySet()){
            protectedRegions.add(entry.getValue());
        }
        return protectedRegions;
    }

    private ProtectedRegion lookForOwnedPlot(Player player){
        ArrayList<ProtectedRegion> plotList = getPlots(player);
        ProtectedRegion ownedPlot = null;
        for(int i = 0; i < plotList.size(); i++){
            if(plotList.get(i).getOwners().contains(player.getName())){
                ownedPlot = plotList.get(i);
                break;
            }
        }
        return ownedPlot;
    }
}
