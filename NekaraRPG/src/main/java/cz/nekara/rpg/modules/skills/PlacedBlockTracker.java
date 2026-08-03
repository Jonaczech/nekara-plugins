package cz.nekara.rpg.modules.skills;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

final class PlacedBlockTracker implements Listener {
    private static final String STORAGE_KEY = "skills_player_placed_v1";

    private final JavaPlugin plugin;
    private final NamespacedKey key;
    private boolean enabled;

    PlacedBlockTracker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, STORAGE_KEY);
    }

    void enable() {
        if (enabled) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
    }

    void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        HandlerList.unregisterAll(this);
    }

    boolean isPlayerPlaced(Block block) {
        return read(block.getChunk()).contains(encode(block));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordPlacement(BlockPlaceEvent event) {
        mark(event.getBlockPlaced());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordMultiPlacement(BlockMultiPlaceEvent event) {
        event.getReplacedBlockStates().forEach(state -> mark(state.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void removeBrokenMarker(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material original = block.getType();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (enabled && block.getType() != original) {
                unmark(block);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void removeExplosionMarkers(BlockExplodeEvent event) {
        event.blockList().forEach(this::unmark);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void removeExplosionMarkers(EntityExplodeEvent event) {
        event.blockList().forEach(this::unmark);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void moveExtendedMarkers(BlockPistonExtendEvent event) {
        moveMarkers(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void moveRetractedMarkers(BlockPistonRetractEvent event) {
        moveMarkers(event.getBlocks(), event.getDirection());
    }

    private void moveMarkers(List<Block> blocks, BlockFace direction) {
        List<Move> moves = blocks.stream()
            .filter(this::isPlayerPlaced)
            .map(block -> new Move(block, block.getRelative(direction)))
            .toList();
        if (moves.isEmpty()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!enabled) {
                return;
            }
            for (Move move : moves) {
                unmark(move.source());
            }
            for (Move move : moves) {
                mark(move.destination());
            }
        });
    }

    private void mark(Block block) {
        Set<Integer> positions = read(block.getChunk());
        if (positions.add(encode(block))) {
            write(block.getChunk(), positions);
        }
    }

    private void unmark(Block block) {
        Set<Integer> positions = read(block.getChunk());
        if (positions.remove(encode(block))) {
            write(block.getChunk(), positions);
        }
    }

    private Set<Integer> read(Chunk chunk) {
        int[] stored = chunk.getPersistentDataContainer().get(key, PersistentDataType.INTEGER_ARRAY);
        Set<Integer> positions = new HashSet<>();
        if (stored != null) {
            Arrays.stream(stored).forEach(positions::add);
        }
        return positions;
    }

    private void write(Chunk chunk, Set<Integer> positions) {
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        if (positions.isEmpty()) {
            container.remove(key);
            return;
        }
        int[] stored = positions.stream().mapToInt(Integer::intValue).sorted().toArray();
        container.set(key, PersistentDataType.INTEGER_ARRAY, stored);
    }

    private static int encode(Block block) {
        return PlacedBlockPositionCodec.encode(
            block.getX(), block.getY(), block.getZ(), block.getWorld().getMinHeight());
    }

    private record Move(Block source, Block destination) {
    }
}
