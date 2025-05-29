package net.momirealms.craftengine.bukkit.item;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class LegacyNetworkItemHandler implements NetworkItemHandler<ItemStack> {

    @Override
    public Optional<Item<ItemStack>> c2s(Item<ItemStack> wrapped, ItemBuildContext context) {
        if (!wrapped.hasTag(NETWORK_ITEM_TAG)) return Optional.empty();
        CompoundTag networkData = (CompoundTag) wrapped.getNBTTag(NETWORK_ITEM_TAG);
        if (networkData == null) return Optional.empty();
        wrapped.removeTag(NETWORK_ITEM_TAG);
        for (Map.Entry<String, Tag> entry : networkData.entrySet()) {
            if (entry.getValue() instanceof CompoundTag tag) {
                NetworkItemHandler.apply(entry.getKey(), tag, wrapped);
            }
        }
        return Optional.of(wrapped);
    }

    @Override
    public Optional<Item<ItemStack>> s2c(Item<ItemStack> wrapped, ItemBuildContext context) {
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        if (optionalCustomItem.isEmpty()) {
            if (!Config.interceptItem()) return Optional.empty();
            return new OtherItem(wrapped).process();
        }
        return Optional.empty();
    }

    public static boolean processCustomName(Item<ItemStack> item, BiConsumer<String, CompoundTag> callback) {
        Optional<String> optionalCustomName = item.customNameJson();
        if (optionalCustomName.isPresent()) {
            String line = optionalCustomName.get();
            Map<String, Component> tokens = CraftEngine.instance().fontManager().matchTags(line);
            if (!tokens.isEmpty()) {
                item.customNameJson(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(line), tokens)));
                callback.accept("display.Name", NetworkItemHandler.pack(Operation.ADD, new StringTag(line)));
                return true;
            }
        }
        return false;
    }

    private static boolean processLore(Item<ItemStack> item, BiConsumer<String, CompoundTag> callback) {
        Optional<List<String>> optionalLore = item.loreJson();
        if (optionalLore.isPresent()) {
            boolean changed = false;
            List<String> lore = optionalLore.get();
            List<String> newLore = new ArrayList<>(lore.size());
            for (String line : lore) {
                Map<String, Component> tokens = CraftEngine.instance().fontManager().matchTags(line);
                if (tokens.isEmpty()) {
                    newLore.add(line);
                } else {
                    newLore.add(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(line), tokens)));
                    changed = true;
                }
            }
            if (changed) {
                item.loreJson(newLore);
                ListTag listTag = new ListTag();
                for (String line : lore) {
                    listTag.add(new StringTag(line));
                }
                callback.accept("display.Lore", NetworkItemHandler.pack(Operation.ADD, listTag));
                return true;
            }
        }
        return false;
    }

    static class OtherItem {
        private final Item<ItemStack> item;
        private boolean globalChanged = false;
        private CompoundTag networkTag;

        public OtherItem(Item<ItemStack> item) {
            this.item = item;
        }

        public Optional<Item<ItemStack>> process() {
            if (processLore(this.item, (s, c) -> networkTag().put(s, c))) {
                this.globalChanged = true;
            }
            if (processCustomName(this.item, (s, c) -> networkTag().put(s, c))) {
                this.globalChanged = true;
            }
            if (this.globalChanged) {
                this.item.setTag(this.networkTag, NETWORK_ITEM_TAG);
                return Optional.of(this.item);
            } else {
                return Optional.empty();
            }
        }

        public CompoundTag networkTag() {
            if (this.networkTag == null) {
                this.networkTag = new CompoundTag();
            }
            return this.networkTag;
        }
    }
}
