package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.shared.block.BlockBehavior;

import java.util.List;
import java.util.Map;

public class OnLiquidBlockBehavior extends AbstractCanSurviveBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final boolean onWater;
    private final boolean onLava;
    private final boolean stackable;

    public OnLiquidBlockBehavior(CustomBlock block, int delay, boolean stackable, boolean onWater, boolean onLava) {
        super(block, delay);
        this.onWater = onWater;
        this.onLava = onLava;
        this.stackable = stackable;
    }

    public boolean onWater() {
        return this.onWater;
    }

    public boolean onLava() {
        return this.onLava;
    }

    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            List<String> liquidTypes = MiscUtils.getAsStringList(arguments.getOrDefault("liquid-type", List.of("water")));
            boolean stackable = (boolean) arguments.getOrDefault("stackable", false);
            int delay = ResourceConfigUtils.getAsInt(arguments.getOrDefault("delay", 0), "delay");
            return new OnLiquidBlockBehavior(block, delay, stackable, liquidTypes.contains("water"), liquidTypes.contains("lava"));
        }
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws ReflectiveOperationException {
        int y = FastNMS.INSTANCE.field$Vec3i$y(blockPos);
        int x = FastNMS.INSTANCE.field$Vec3i$x(blockPos);
        int z = FastNMS.INSTANCE.field$Vec3i$z(blockPos);
        Object belowPos = FastNMS.INSTANCE.constructor$BlockPos(x, y - 1, z);
        Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, belowPos);
        return mayPlaceOn(belowState, world, belowPos);
    }

    protected boolean mayPlaceOn(Object belowState, Object world, Object belowPos) throws ReflectiveOperationException {
        if (this.stackable) {
            int id = BlockStateUtils.blockStateToId(belowState);
            if (!BlockStateUtils.isVanillaBlock(id)) {
                ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockStateUnsafe(id);
                if (immutableBlockState.owner().value() == super.customBlock) {
                    return true;
                }
            }
        }
        Object fluidState = Reflections.method$Level$getFluidState.invoke(world, belowPos);
        Object fluidStateAbove = Reflections.method$Level$getFluidState.invoke(world, LocationUtils.above(belowPos));
        if (Reflections.method$FluidState$getType.invoke(fluidStateAbove) != Reflections.instance$Fluids$EMPTY) {
            return false;
        }
        if (this.onWater && (Reflections.method$FluidState$getType.invoke(fluidState) == Reflections.instance$Fluids$WATER || Reflections.field$StateHolder$owner.get(belowState) == Reflections.instance$Blocks$ICE)) {
            return true;
        }
        if (this.onLava && Reflections.method$FluidState$getType.invoke(fluidState) == Reflections.instance$Fluids$LAVA) {
            return true;
        }
        return false;
    }
}
