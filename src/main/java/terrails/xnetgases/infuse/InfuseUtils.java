package terrails.xnetgases.infuse;

import mekanism.api.Action;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class InfuseUtils {

    private static Map<String, InfuseConnectorSettings.InfuseMode> connectorModeCache;
    private static Map<String, InfuseChannelSettings.ChannelMode> channelModeCache;

    @Nonnull
    public static InfuseConnectorSettings.InfuseMode getConnectorModeFrom(String s) {
        if (connectorModeCache == null) {
            connectorModeCache = new HashMap<>();
            for (InfuseConnectorSettings.InfuseMode mode : InfuseConnectorSettings.InfuseMode.values()) {
                connectorModeCache.put(mode.name(), mode);
            }
        }
        return connectorModeCache.get(s);
    }

    @Nonnull
    public static InfuseChannelSettings.ChannelMode getChannelModeFrom(String s) {
        if (channelModeCache == null) {
            channelModeCache = new HashMap<>();
            for (InfuseChannelSettings.ChannelMode mode : InfuseChannelSettings.ChannelMode.values()) {
                channelModeCache.put(mode.name(), mode);
            }
        }
        return channelModeCache.get(s);
    }

    @Nonnull
    public static Optional<IInfusionHandler> getInfuseHandlerFor(@Nullable ICapabilityProvider provider, @Nullable Direction direction) {
        if (provider == null) {
            return Optional.empty();
        } else if (Capabilities.INFUSION_HANDLER_CAPABILITY != null && provider.getCapability(Capabilities.INFUSION_HANDLER_CAPABILITY, direction).isPresent()) {
            return Optional.of(provider.getCapability(Capabilities.INFUSION_HANDLER_CAPABILITY, direction)
                    .orElseThrow(() -> new IllegalArgumentException("IInfusionHandler is 'null' even though it said that its present")));
        } else if (direction != null && provider instanceof IInfusionHandler.ISidedInfusionHandler && ((IInfusionHandler.ISidedInfusionHandler) provider).getTanks(direction) >= 1) {
            return Optional.of((IInfusionHandler) provider);
        } else if (!(provider instanceof IInfusionHandler.ISidedInfusionHandler) && provider instanceof IInfusionHandler && ((IInfusionHandler) provider).getTanks() >= 1) {
            return Optional.of((IInfusionHandler) provider);
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public static InfusionStack insertInfuse(IInfusionHandler handler, InfusionStack stack, @Nullable Direction direction, Action action) {
        if (handler instanceof IInfusionHandler.ISidedInfusionHandler) {
            return ((IInfusionHandler.ISidedInfusionHandler) handler).insertChemical(stack, direction, action);
        } else return handler.insertChemical(stack, action);
    }

    @Nonnull
    public static InfusionStack extractInfuse(IInfusionHandler handler, long amount, @Nullable Direction direction, Action action) {
        if (handler instanceof IInfusionHandler.ISidedInfusionHandler) {
            return ((IInfusionHandler.ISidedInfusionHandler) handler).extractChemical(amount, direction, action);
        } else return handler.extractChemical(amount, action);
    }

    public static List<InfuseType> getInfuseInTank(@Nonnull IInfusionHandler handler, @Nullable Direction direction) {
        List<InfuseType> infuses = new ArrayList<>();
        if (handler instanceof IInfusionHandler.ISidedInfusionHandler) {
            for (int i = 0; i < ((IInfusionHandler.ISidedInfusionHandler) handler).getTanks(direction); i++) {
                infuses.add(((IInfusionHandler.ISidedInfusionHandler) handler).getChemicalInTank(i, direction).getType());
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                infuses.add(handler.getChemicalInTank(i).getType());
            }
        }
        return infuses;
    }

    public static List<InfuseType> getInfuseInTank(@Nonnull IInfusionHandler handler) {
        return getInfuseInTank(handler, null);
    }

    public static long getInfuseCount(@Nonnull IInfusionHandler handler, @Nullable Direction direction, @Nullable Predicate<InfusionStack> filter) {
        long count = 0;
        if (handler instanceof IInfusionHandler.ISidedInfusionHandler) {
            for (int i = 0; i < ((IInfusionHandler.ISidedInfusionHandler) handler).getTanks(direction); i++) {
                InfusionStack stack = ((IInfusionHandler.ISidedInfusionHandler) handler).getChemicalInTank(i, direction);
                if (!stack.isEmpty() && (filter == null || filter.test(stack))) {
                    count += stack.getAmount();
                }
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                InfusionStack stack = handler.getChemicalInTank(i);
                if (!stack.isEmpty() && (filter == null || filter.test(stack))) {
                    count += stack.getAmount();
                }
            }
        }
        return count;
    }

    public static long getInfuseCount(@Nonnull IInfusionHandler handler, @Nullable Direction direction, @Nullable InfuseType filter) {
        return getInfuseCount(handler, direction, (stack) -> filter == null || stack.getType() == filter);
    }

    public static long getInfuseCount(@Nonnull IInfusionHandler handler, @Nullable Direction direction, @Nullable InfusionStack filter) {
        return getInfuseCount(handler, direction, (stack) -> filter == null || stack.equals(filter));
    }

    public static long getInfuseCount(@Nonnull IInfusionHandler handler, @Nullable Direction direction) {
        return getInfuseCount(handler, direction, (Predicate<InfusionStack>) null);
    }

    public static long getInfuseCount(@Nonnull IInfusionHandler handler) {
        return getInfuseCount(handler, null);
    }

}
