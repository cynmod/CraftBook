package com.sk89q.craftbook.mech;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.sk89q.craftbook.LocalPlayer;
import com.sk89q.craftbook.bukkit.BaseBukkitPlugin;
import com.sk89q.craftbook.bukkit.MechanismsPlugin;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ItemID;

/**
 * Snow fall mechanism. Builds up/tramples snow
 *
 * @author Me4502
 */
public class Snow implements Listener {

    final MechanismsPlugin plugin;

    public Snow(MechanismsPlugin plugin) {

        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSnowballHit(ProjectileHitEvent event) {

        if(event.getEntity() instanceof Snowball) {
            if(event.getEntity().getShooter() instanceof Player) {
                LocalPlayer player = plugin.wrap((Player) event.getEntity().getShooter());
                if (!player.hasPermission("craftbook.mech.snow.place")) return;
                try {
                    Block block = event.getEntity().getLocation().getBlock();
                    incrementData(block.getRelative(0, 1, 0));
                } catch (Exception ignored) {
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (!plugin.getLocalConfiguration().snowSettings.placeSnow) return;
        if (event == null || event.getAction() == null) return;
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        if (event.getPlayer() == null || event.getClickedBlock() == null || event.getClickedBlock().getTypeId() == 0)
            return;

        if (!plugin.canBuildInArea(event.getClickedBlock().getLocation(), event.getPlayer()))
            return;

        LocalPlayer player = plugin.wrap(event.getPlayer());
        if (!player.hasPermission("craftbook.mech.snow.place")) return;
        try {
            if (event.getPlayer().getItemInHand().getTypeId() == ItemID.SNOWBALL
                    && event.getClickedBlock().getTypeId() == 78) {
                if (event.getClickedBlock().getData() < (byte) 7) {
                    incrementData(event.getClickedBlock());
                }
            } else if (event.getPlayer().getItemInHand().getTypeId() == ItemID.SNOWBALL && event.getPlayer().getWorld().getBlockAt(event.getClickedBlock().getLocation().add(0, 1, 0)).getTypeId() == 0) {
                incrementData(event.getPlayer().getWorld().getBlockAt(event.getClickedBlock().getLocation().add(0, 1, 0)));
            }
        } catch (Exception ignored) {
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {

        if (!plugin.getLocalConfiguration().snowSettings.trample) return;

        if(event.getFrom().distanceSquared(event.getTo()) < 0.1)
            return;
        LocalPlayer player = plugin.wrap(event.getPlayer());
        if (!player.hasPermission("craftbook.mech.snow.trample")) return;

        if (plugin.getLocalConfiguration().snowSettings.jumpTrample && event.getPlayer().getVelocity().getY() >= 0D)
            return;
        if (BaseBukkitPlugin.random.nextInt(20) == 0) {
            Block b = event.getPlayer().getWorld().getBlockAt(event.getPlayer().getLocation());
            if (b.getTypeId() == 78) {
                if (!plugin.canBuildInArea(event.getPlayer().getLocation(), event.getPlayer()))
                    return;
                lowerData(b);
            }

            b = event.getPlayer().getWorld().getBlockAt(event.getPlayer().getLocation().subtract(0, 1, 0));
            if (b.getTypeId() == 78) {
                if (!plugin.canBuildInArea(event.getPlayer().getLocation(), event.getPlayer()))
                    return;
                lowerData(b);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockForm(final BlockFormEvent event) {

        if (!plugin.getLocalConfiguration().snowSettings.enable) return;
        if (event.getNewState().getTypeId() == BlockID.SNOW) {
            Block block = event.getBlock();

            if (block.getTypeId() != BlockID.SNOW_BLOCK && block.getTypeId() != BlockID.SNOW) {
                Location blockLoc = block.getLocation().subtract(0, 1, 0);
                if (block.getWorld().getBlockAt(blockLoc).getTypeId() == BlockID.SNOW_BLOCK
                        && !plugin.getLocalConfiguration().snowSettings.piling
                        || block.getWorld().getBlockAt(blockLoc).getTypeId() == BlockID.SNOW)
                    return;
                long delay = BaseBukkitPlugin.random.nextInt(100) + 60;
                if (plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,
                        new MakeSnow(block.getLocation()), delay * 20L) == -1) {
                    plugin.getLogger().log(Level.SEVERE, "[CraftBookMechanisms] Snow Mechanic failed to schedule!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(final BlockPhysicsEvent event) {

        if (!plugin.getLocalConfiguration().snowSettings.enable) return;
        if (event.getBlock().getTypeId() == BlockID.SNOW) {
            Block block = event.getBlock();

            disperse(event.getBlock(), true);

            if(event.getBlock().getWorld().hasStorm()) {

                if (block.getTypeId() != BlockID.SNOW_BLOCK && block.getTypeId() != BlockID.SNOW) {
                    Location blockLoc = block.getLocation().subtract(0, 1, 0);
                    if (block.getWorld().getBlockAt(blockLoc).getTypeId() == BlockID.SNOW_BLOCK
                            && !plugin.getLocalConfiguration().snowSettings.piling
                            || block.getWorld().getBlockAt(blockLoc).getTypeId() == BlockID.SNOW)
                        return;
                    long delay = BaseBukkitPlugin.random.nextInt(100) + 60;
                    if (plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,
                            new MakeSnow(block.getLocation()), delay * 20L) == -1) {
                        plugin.getLogger().log(Level.SEVERE, "[CraftBookMechanisms] Snow Mechanic failed to schedule!");
                    }
                }
            }
        }
    }

    public class MakeSnow implements Runnable {

        final Location event;

        public MakeSnow(Location event) {

            this.event = event;
        }

        @Override
        public void run() {

            if (event.getWorld().hasStorm()) {
                if (event.getBlock().getData() > (byte) 7) return;
                if (event.subtract(0, 1, 0).getBlock().getTypeId() == 0) return;

                event.add(0, 1, 0);
                if (!(event.getBlock().getTypeId() == 78) && !(event.getBlock().getTypeId() == 80)) return;
                incrementData(event.getBlock());
                long delay = BaseBukkitPlugin.random.nextInt(100) + 60;
                if (plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new MakeSnow(event),
                        delay * 20L) == -1) {
                    plugin.getLogger().log(Level.SEVERE, "[CraftBookMechanisms] Snow Mechanic failed to schedule!");
                }
            } else {
                long delay = BaseBukkitPlugin.random.nextInt(100) + 600;
                if (plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new MakeSnow(event),
                        delay * 20L) == -1) {
                    plugin.getLogger().log(Level.SEVERE, "[CraftBookMechanisms] Snow Mechanic failed to schedule!");
                }
            }
        }
    }

    public void lowerData(Block block) {

        byte newData = (byte) (block.getData() - 1);
        if (newData < 1) {
            block.setTypeId(0);
            newData = 0;
        }
        if (block.getTypeId() == BlockID.SNOW_BLOCK) {
            block.setTypeId(BlockID.SNOW);
            newData = (byte) 7;
        }
        if (newData > (byte) 7) {
            newData = (byte) 0;
        }
        setBlockDataWithNotify(block, newData);
    }

    public boolean disperse(Block block, boolean remove) {
        if(block.getRelative(0, -1, 0).getTypeId() == 0)
            disperse(block.getRelative(0, -1, 0), remove);
        if(block.getRelative(0, -1, 0).getTypeId() == BlockID.SNOW && block.getRelative(0, -1, 0).getData() < 0x7)
            incrementData(block.getRelative(0, -1, 0));
        if(block.getRelative(0, -1, 0).getTypeId() == BlockID.SNOW || block.getRelative(0, -1, 0).getTypeId() == BlockID.AIR) {
            if(block.getRelative(0, -1, 0).getData() < block.getData()) {
                incrementData(block.getRelative(0, -1, 0));
                if(remove)
                    lowerData(block);
                return true;
            }
        }
        if(block.getRelative(1, 0, 0).getTypeId() == BlockID.SNOW || block.getRelative(1, 0, 0).getTypeId() == BlockID.AIR) {
            if(block.getRelative(1, 0, 0).getData() < block.getData()) {
                incrementData(block.getRelative(1, 0, 0));
                if(remove)
                    lowerData(block);
                return true;
            }
        }
        if(block.getRelative(-1, 0, 0).getTypeId() == BlockID.SNOW || block.getRelative(-1, 0, 0).getTypeId() == BlockID.AIR) {
            if(block.getRelative(-1, 0, 0).getData() < block.getData()) {
                incrementData(block.getRelative(-1, 0, 0));
                if(remove)
                    lowerData(block);
                return true;
            }
        }
        if(block.getRelative(0, 0, 1).getTypeId() == BlockID.SNOW || block.getRelative(0, 0, 1).getTypeId() == BlockID.AIR) {
            if(block.getRelative(0, 0, 1).getData() < block.getData()) {
                incrementData(block.getRelative(0, 0, 1));
                if(remove)
                    lowerData(block);
                return true;
            }
        }
        if(block.getRelative(0, 0, -1).getTypeId() == BlockID.SNOW || block.getRelative(0, 0, -1).getTypeId() == BlockID.AIR) {
            if(block.getRelative(0, 0, -1).getData() < block.getData()) {
                incrementData(block.getRelative(0, 0, -1));
                if(remove)
                    lowerData(block);
                return true;
            }
        }

        return false;
    }

    public void incrementData(Block block) {

        if(block.getTypeId() == 0 && block.getRelative(0, -1, 0).getTypeId() == 0) {
            incrementData(block.getRelative(0, -1, 0));
            return;
        }

        if(block.getTypeId() == 0 && BlockType.canPassThrough(block.getRelative(0, -1, 0).getTypeId()) && block.getRelative(0, -1, 0).getData() < 0x7) {
            incrementData(block.getRelative(0, -1, 0));
            return;
        }

        if(plugin.getLocalConfiguration().snowSettings.realistic) {
            if(disperse(block, false))
                return;
        }

        byte newData = 0;
        if(block.getTypeId() != BlockID.SNOW)
            block.setTypeId(BlockID.SNOW, false);
        else
            newData = (byte) (block.getData() + 1);
        if (newData > (byte) 7 && plugin.getLocalConfiguration().snowSettings.piling) {
            block.setTypeId(BlockID.SNOW_BLOCK, false);
            newData = (byte) 0;
        } else if (newData > 7) {
            newData = 7;
        }
        setBlockDataWithNotify(block, newData);
    }

    public void setBlockDataWithNotify(Block block, byte data) {

        block.setTypeIdAndData(block.getTypeId(), data, false);
        for (Player p : block.getWorld().getPlayers()) {
            try {
                if (p.getLocation().distanceSquared(block.getLocation()) < plugin.getServer().getViewDistance() * 16 *
                        plugin.getServer().getViewDistance() * 16) {
                    p.sendBlockChange(block.getLocation(), block.getTypeId(), data);
                }
            }
            catch(Exception e){}
        }
    }
}