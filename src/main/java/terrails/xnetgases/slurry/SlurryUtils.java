package terrails.xnetgases.slurry;

import mekanism.api.Action;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.ISlurryHandler.ISidedSlurryHandler;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class SlurryUtils {

    private static Map<String, SlurryConnectorSettings.SlurryMode> connectorModeCache;
    private static Map<String, SlurryChannelSettings.ChannelMode> channelModeCache;

    @Nonnull
    public static SlurryConnectorSettings.SlurryMode getConnectorModeFrom(String s) {
        if (connectorModeCache == null) {
            connectorModeCache = new HashMap<>();
            for (SlurryConnectorSettings.SlurryMode mode : SlurryConnectorSettings.SlurryMode.values()) {
                connectorModeCache.put(mode.name(), mode);
            }
        }
        return connectorModeCache.get(s);
    }

    @Nonnull
    public static SlurryChannelSettings.ChannelMode getChannelModeFrom(String s) {
        if (channelModeCache == null) {
            channelModeCache = new HashMap<>();
            for (SlurryChannelSettings.ChannelMode mode : SlurryChannelSettings.ChannelMode.values()) {
                channelModeCache.put(mode.name(), mode);
            }
        }
        return channelModeCache.get(s);
    }

    @Nonnull
    public static Optional<ISlurryHandler> getSlurryHandlerFor(@Nullable ICapabilityProvider provider, @Nullable Direction direction) {
        if (provider == null) {
            return Optional.empty();
        } else if (Capabilities.SLURRY_HANDLER_CAPABILITY != null && provider.getCapability(Capabilities.SLURRY_HANDLER_CAPABILITY, direction).isPresent()) {
            return Optional.of(provider.getCapability(Capabilities.SLURRY_HANDLER_CAPABILITY, direction)
                    .orElseThrow(() -> new IllegalArgumentException("ISlurryHandler is 'null' even though it said that its present")));
        } else if (direction != null && provider instanceof ISidedSlurryHandler && ((ISidedSlurryHandler) provider).getTanks(direction) >= 1) {
            return Optional.of((ISlurryHandler) provider);
        } else if (!(provider instanceof ISidedSlurryHandler) && provider instanceof ISlurryHandler && ((ISlurryHandler) provider).getTanks() >= 1) {
            return Optional.of((ISlurryHandler) provider);
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public static SlurryStack insertSlurry(ISlurryHandler handler, SlurryStack stack, @Nullable Direction direction, Action action) {
        if (direction != null && handler instanceof ISidedSlurryHandler) {
            return ((ISidedSlurryHandler) handler).insertChemical(stack, direction, action);
        } else return handler.insertChemical(stack, action);
    }

    @Nonnull
    public static SlurryStack extractSlurry(ISlurryHandler handler, long amount, @Nullable Direction direction, Action action) {
        if (direction != null && handler instanceof ISidedSlurryHandler) {
            return ((ISidedSlurryHandler) handler).extractChemical(amount, direction, action);
        } else return handler.extractChemical(amount, action);
    }

    public static long getSlurryCount(@Nonnull ISlurryHandler handler, @Nullable Direction direction, @Nullable Predicate<SlurryStack> filter) {
        long count = 0;
        if (direction != null && handler instanceof ISidedSlurryHandler) {
            for (int i = 0; i < ((ISidedSlurryHandler) handler).getTanks(direction); i++) {
                SlurryStack stack = ((ISidedSlurryHandler) handler).getChemicalInTank(i, direction);
                if (!stack.isEmpty() && (filter == null || filter.test(stack))) {
                    count += stack.getAmount();
                }
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                SlurryStack stack = handler.getChemicalInTank(i);
                if (!stack.isEmpty() && (filter == null || filter.test(stack))) {
                    count += stack.getAmount();
                }
            }
        }
        return count;
    }

    public static long getSlurryCount(@Nonnull ISlurryHandler handler, @Nullable Direction direction, @Nullable Slurry filter) {
        return getSlurryCount(handler, direction, (stack) -> filter == null || stack.getType() == filter);
    }

    public static long getSlurryCount(@Nonnull ISlurryHandler handler, @Nullable Direction direction, @Nullable SlurryStack filter) {
        return getSlurryCount(handler, direction, (stack) -> filter == null || stack.equals(filter));
    }

    public static long getSlurryCount(@Nonnull ISlurryHandler handler, @Nullable Direction direction) {
        return getSlurryCount(handler, direction, (Predicate<SlurryStack>) null);
    }

    public static long getSlurryCount(@Nonnull ISlurryHandler handler) {
        return getSlurryCount(handler, null);
    }

}
