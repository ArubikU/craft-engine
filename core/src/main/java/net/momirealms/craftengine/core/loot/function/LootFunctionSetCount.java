package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.condition.LootConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LootFunctionSetCount<T> extends AbstractLootConditionalFunction<T> {
    public static final Factory<?> FACTORY = new Factory<>();

    private final NumberProvider value;
    private final boolean add;

    public LootFunctionSetCount(List<Condition<LootContext>> conditions, NumberProvider value, boolean add) {
        super(conditions);
        this.value = value;
        this.add = add;
    }

    @Override
    public Key type() {
        return LootFunctions.SET_COUNT;
    }

    @Override
    protected Item<T> applyInternal(Item<T> item, LootContext context) {
        int amount = this.add ? item.count() : 0;
        item.count(amount + this.value.getInt(context));
        return item;
    }

    public static class Factory<A> implements LootFunctionFactory<A> {
        @SuppressWarnings("unchecked")
        @Override
        public LootFunction<A> create(Map<String, Object> arguments) {
            Object value = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("count"), "warning.config.loot_table.function.set_count.missing_count");
            boolean add = (boolean) arguments.getOrDefault("add", false);
            List<Condition<LootContext>> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            return new LootFunctionSetCount<>(conditions, NumberProviders.fromObject(value), add);
        }
    }
}
