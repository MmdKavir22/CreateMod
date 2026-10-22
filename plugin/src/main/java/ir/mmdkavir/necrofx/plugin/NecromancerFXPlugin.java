package ir.mmdkavir.necrofx.plugin;

import ir.mmdkavir.necrofx.api.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class NecromancerFXPlugin extends JavaPlugin implements Listener, NecromancerFXAPI {
 private final Map<String,MobDefinition> definitions=new ConcurrentHashMap<>(); private final Map<UUID,String> tracked=new ConcurrentHashMap<>(); private final Set<UUID> clients=ConcurrentHashMap.newKeySet();
 @Override public void onEnable(){ saveDefaultConfig(); loadDefinitions(); getServer().getPluginManager().registerEvents(this,this); getServer().getServicesManager().register(NecromancerFXAPI.class,this,this,ServicePriority.Normal); Objects.requireNonNull(getCommand("necromancerfx")).setExecutor((sender,cmd,label,args)->{ if(!sender.hasPermission("necromancerfx.command")){sender.sendMessage("§cNo permission.");return true;} String sub=args.length==0?"help":args[0].toLowerCase(Locale.ROOT); switch(sub){case "status"->sender.sendMessage("§5NecromancerFX §7v"+getDescription().getVersion()+" | definitions: "+definitions.size()+ (sender instanceof Player p?" | client mod: "+isClientModAvailable(p.getUniqueId()):"")); case "mobs"->{sender.sendMessage("§5Registered mobs:");definitions.keySet().forEach(id->sender.sendMessage(" §7- §f"+id));} case "reload"->{if(!sender.hasPermission("necromancerfx.admin")){sender.sendMessage("§cNo permission.");return true;}reloadConfig();loadDefinitions();sender.sendMessage("§aConfig reloaded.");} default->{sender.sendMessage("§5NecromancerFX: /necromancerfx status|mobs|reload");}} return true;}); getLogger().info("NecromancerFX enabled; protocol v1"); }
 private void loadDefinitions(){definitions.clear(); ConfigurationSection root=getConfig().getConfigurationSection("mobs");if(root==null)return;for(String id:root.getKeys(false)){ConfigurationSection s=root.getConfigurationSection(id);if(s==null)continue;Map<String,String>a=new HashMap<>();ConfigurationSection as=s.getConfigurationSection("animations");if(as!=null)for(String k:as.getKeys(false))a.put(k,as.getString(k,k));try{definitions.put(id,new MobDefinition(id,s.getString("display-name",id),s.getString("fallback-entity","minecraft:zombie"),s.getString("model",id),s.getString("texture",id),(float)s.getDouble("scale",1),a,s.getInt("summon-duration-ticks",40)));}catch(IllegalArgumentException e){getLogger().warning("Invalid mob definition "+id+": "+e.getMessage());}}}
 @EventHandler public void onDeath(EntityDeathEvent e){tracked.remove(e.getEntity().getUniqueId());}
 @EventHandler public void onQuit(PlayerQuitEvent e){clients.remove(e.getPlayer().getUniqueId());}
 @Override public boolean isClientModAvailable(UUID id){return clients.contains(id);}
 @Override public Optional<String> getCustomMobId(UUID id){return Optional.ofNullable(tracked.get(id));}
 @Override public boolean registerMobDefinition(MobDefinition d){if(d==null||(!definitions.containsKey(d.id())&&definitions.size()>=getConfig().getInt("limits.max-definitions",128)))return false;definitions.put(d.id(),d);return true;}
 @Override public boolean playAnimation(UUID id,String animationId){return tracked.containsKey(id);}
 @Override public boolean playSummonEffect(UUID playerId,MobDefinition d){return Bukkit.getPlayer(playerId)!=null&&d!=null;}
 @Override public void syncMob(UUID id){Entity e=Bukkit.getEntity(id);if(e!=null&&tracked.containsKey(id)){} }
 @Override public void unregisterMob(UUID id){tracked.remove(id);}
 @Override public int protocolVersion(){return 1;}
}
