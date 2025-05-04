package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.CommonParameters;
import net.momirealms.craftengine.core.util.Factory;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class FallingBlockCondition<CTX extends Context> implements Condition<CTX> {

    @Override
    public Key type() {
        return CommonConditions.FALLING_BLOCK;
    }

    @Override
    public boolean test(CTX ctx) {
        return ctx.getOptionalParameter(CommonParameters.FALLING_BLOCK).orElse(false);
    }

    public static class FactoryImpl<CTX extends Context> implements Factory<Condition<CTX>> {

        @Override
        public Condition<CTX> create(Map<String, Object> arguments) {
            return new FallingBlockCondition<>();
        }
    }
}
