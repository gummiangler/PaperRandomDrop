package io.github.gummiangler.droprandomizer;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.loot.LootContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;
import org.bukkit.attribute.Attribute;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.bukkit.Bukkit.getLootTable;

public final class DropRandomizer extends JavaPlugin implements Listener {

    private final Map<Material, Material> blockMap = new HashMap<>();
    private final Map<EntityType, EntityType> entityMap = new HashMap<>();
    private final Set<EntityType> forbiddenMobs = Set.of(
            EntityType.WANDERING_TRADER,
            EntityType.FOX,
            EntityType.BAT,
            EntityType.ENDER_DRAGON,
            EntityType.SILVERFISH,
            EntityType.GOAT,
            EntityType.FROG,
            EntityType.GIANT,
            EntityType.VEX,
            EntityType.ILLUSIONER,
            EntityType.CREAKING,
            EntityType.OCELOT,
            EntityType.CAMEL,
            EntityType.BEE,
            EntityType.HAPPY_GHAST,
            EntityType.ARMADILLO,
            EntityType.SNIFFER,
            EntityType.WOLF,
            EntityType.ARMOR_STAND,
            EntityType.ALLAY,
            EntityType.AXOLOTL,
            EntityType.ENDERMITE,
            EntityType.TADPOLE,
            EntityType.VILLAGER,
            EntityType.PLAYER,
            EntityType.UNKNOWN
    );

    private File blockMapFile;
    private File mobMapFile;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        blockMapFile = new File(getDataFolder(), "blockmap.yml");
        mobMapFile = new File(getDataFolder(), "mobmap.yml");

        if (!blockMapFile.exists() || !mobMapFile.exists()) {
            generateRandomMapping();
            saveBlockMapping();
            saveMobMapping();
        } else {
            loadBlockMapping();
            loadMobMapping();
        }
    }

    @Override
    public void onDisable() {
        saveBlockMapping();
        saveMobMapping();
    }

    private void generateRandomMapping() {
        List<Material> blocks = new ArrayList<>();
        List<EntityType> entities = new ArrayList<>();

        for (Material m : Material.values()) {
            if (
                    m.isBlock() && m.isItem() &&
                            m != Material.AIR &&
                            !m.name().startsWith("LEGACY_") &&
                            m != Material.BARRIER &&
                            m != Material.STRUCTURE_VOID &&
                            m != Material.COMMAND_BLOCK &&
                            m != Material.CHAIN_COMMAND_BLOCK &&
                            m != Material.REPEATING_COMMAND_BLOCK &&
                            m != Material.JIGSAW &&
                            m != Material.DEBUG_STICK &&
                            m != Material.KNOWLEDGE_BOOK &&
                            m != Material.VOID_AIR &&
                            m != Material.CAVE_AIR
            ) {
                blocks.add(m);
            }
        }

        for (EntityType entityType : EntityType.values()) {
            if (entityType.isAlive() && !forbiddenMobs.contains(entityType)) {
                entities.add(entityType);
            }
        }

        List<Material> shuffledblocks = new ArrayList<>(blocks);
        Collections.shuffle(shuffledblocks);
        for (int i = 0; i < blocks.size(); i++) {
            blockMap.put(blocks.get(i), shuffledblocks.get(i));
        }

        List<EntityType> shuffledmobs = new ArrayList<>(entities);
        Collections.shuffle(shuffledmobs);
        for (int i = 0; i < entities.size(); i++) {
            entityMap.put(entities.get(i), shuffledmobs.get(i));
        }

    }

    private void saveBlockMapping() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<Material, Material> entry : blockMap.entrySet()) {
            config.set(entry.getKey().name(), entry.getValue().name());
        }
        try {
            config.save(blockMapFile);
        } catch (IOException e) {
            getLogger().severe("Fehler beim Speichern der Block-Mapping-Datei: " + e.getMessage());
        }
    }

    private void saveMobMapping() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<EntityType, EntityType> entry : entityMap.entrySet()) {
            config.set(entry.getKey().name(), entry.getValue().name());
        }
        try {
            config.save(mobMapFile);
        } catch (IOException e) {
            getLogger().severe("Fehler beim Speichern der Mob-Mapping-Datei: " + e.getMessage());
        }
    }

    private void loadBlockMapping() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(blockMapFile);
        for (String key : config.getKeys(false)) {
            Material from = Material.getMaterial(key);
            String toString = config.getString(key);
            if (from == null) {
                getLogger().warning("Ungültiges Ausgangs-Material im blockmap.yml: " + key);
                continue;
            }
            if (toString == null) {
                getLogger().warning("Kein Ziel-Material angegeben für: " + key);
                continue;
            }
            Material to = Material.getMaterial(toString);
            if (to == null) {
                getLogger().warning("Ungültiges Ziel-Material im blockmap.yml: " + toString);
                continue;
            }
            blockMap.put(from, to);
        }
    }

    private void loadMobMapping() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(mobMapFile);
        for (String key : config.getKeys(false)) {
            try {
                EntityType from = EntityType.valueOf(key);
                String toString = config.getString(key);
                if (toString == null) {
                    getLogger().warning("Kein Ziel-EntityType angegeben für: " + key);
                    continue;
                }
                EntityType to = EntityType.valueOf(toString);
                entityMap.put(from, to);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Ungültiger EntityType in mobmap.yml: " + key + " oder Ziel " + config.getString(key));
            }
        }
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material original = block.getType();
        Material drop = blockMap.getOrDefault(original, original);
        event.setDropItems(false);

        Collection<ItemStack> drops = block.getDrops(event.getPlayer().getInventory().getItemInMainHand());

        if (drops.isEmpty()) {
            return;
        }

        for (ItemStack stack : drops) {
            int amount = stack.getAmount();
            if (amount <= 0) continue;
            ItemStack replacedStack = new ItemStack(drop, amount);
            block.getWorld().dropItemNaturally(
                    block.getLocation().add(0.5, 0.5, 0.5),
                    replacedStack
            );
        }
    }




    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        EntityType originalType = entity.getType();
        Player killer = event.getEntity().getKiller();

        if (forbiddenMobs.contains(originalType)) {
            return;
        }

        EntityType mappedType = entityMap.getOrDefault(originalType, originalType);

        Entity fakeEntity = EntityCreator.create(mappedType, entity.getLocation());
        if (fakeEntity == null) {
            getLogger().warning("Konnte virtuelle Entity für " + mappedType + " nicht erstellen.");
            return;
        }

        NamespacedKey lootKey = new NamespacedKey("minecraft", "entities/" + mappedType.name().toLowerCase());
        LootTable lootTable = getLootTable(lootKey);
        if (lootTable == null) {
            getLogger().warning("LootTable nicht gefunden für: " + lootKey);
            return;
        }

        LootContext.Builder contextBuilder = new LootContext.Builder(entity.getLocation())
                .lootedEntity(fakeEntity);

        if (killer != null) {
            AttributeInstance luckAttribute = killer.getAttribute(Attribute.LUCK);
            float luck = (luckAttribute != null) ? (float) luckAttribute.getValue() : 0f;
            contextBuilder.killer(killer).luck(luck);
        }

        LootContext context = contextBuilder.build();
        Collection<ItemStack> drops = lootTable.populateLoot(new Random(), context);

        event.getDrops().clear();
        event.getDrops().addAll(drops);

        if (killer != null) {
            for (ItemStack item : drops) {
                if (item != null && item.getType() != Material.AIR) {
                    killer.sendMessage("Drop: " + item.getAmount() + "x " + item.getType());
                }
            }
        }
    }




}

