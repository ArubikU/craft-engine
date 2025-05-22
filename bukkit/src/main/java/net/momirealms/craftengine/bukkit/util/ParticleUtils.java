package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.particle.*;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.World;

public final class ParticleUtils {
    private ParticleUtils() {}

    public static Particle getParticle(String particle) {
        try {
            return Particle.valueOf(particle);
        } catch (IllegalArgumentException e) {
            return switch (particle) {
                case "REDSTONE" -> Particle.valueOf("DUST");
                case "VILLAGER_HAPPY", "HAPPY_VILLAGER" -> Particle.valueOf(VersionHelper.isOrAbove1_20_5() ? "HAPPY_VILLAGER" : "VILLAGER_HAPPY");
                case "BUBBLE", "WATER_BUBBLE" -> Particle.valueOf(VersionHelper.isOrAbove1_20_5() ? "BUBBLE" : "WATER_BUBBLE");
                default -> Particle.valueOf(particle);
            };
        }
    }

    public static final Particle HAPPY_VILLAGER = getParticle("HAPPY_VILLAGER");
    public static final Particle BUBBLE = getParticle("BUBBLE");

    public static Object toBukkitParticleData(ParticleData particleData, Context context, World world, double x, double y, double z) {
        return switch (particleData) {
            case BlockStateData data -> BlockStateUtils.fromBlockData(data.blockState().handle());
            case ColorData data -> ColorUtils.toBukkit(data.color());
            case DustData data -> new Particle.DustOptions(ColorUtils.toBukkit(data.color()), data.size());
            case DustTransitionData data -> new Particle.DustTransition(ColorUtils.toBukkit(data.from()), ColorUtils.toBukkit(data.to()), data.size());
            case ItemStackData data -> data.item().getItem();
            case JavaTypeData data -> data.data();
            case VibrationData data -> new Vibration(new Vibration.Destination.BlockDestination(new Location(world, x + data.destinationX().getDouble(context), y + data.destinationY().getDouble(context), y + data.destinationZ().getDouble(context))), data.arrivalTime().getInt(context));
            case TrailData data -> new Particle.Trail(new Location(world, x + data.targetX().getDouble(context), y + data.targetZ().getDouble(context), z + data.targetZ().getDouble(context)), ColorUtils.toBukkit(data.color()), data.duration().getInt(context));
            default -> null;
        };
    }
}
