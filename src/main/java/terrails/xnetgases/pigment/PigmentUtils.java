package terrails.xnetgases.pigment;

import mekanism.api.Action;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class PigmentUtils {

    private static Map<String, PigmentConnectorSettings.PigmentMode> connectorModeCache;
    private static Map<String, PigmentChannelSettings.ChannelMode> channelModeCache;

    @Nonnull
    public static PigmentConnectorSettings.PigmentMode getConnectorModeFrom(String s) {
        if (connectorModeCache == null) {
            connectorModeCache = new HashMap<>();
            for (PigmentConnectorSettings.PigmentMode mode : PigmentConnectorSettings.PigmentMode.values()) {
                connectorModeCache.put(mode.name(), mode);
            }
        }
        return connectorModeCache.get(s);
    }

    @Nonnull
    public static PigmentChannelSettings.ChannelMode getChannelModeFrom(String s) {
        if (channelModeCache == null) {
            channelModeCache = new HashMap<>();
            for (PigmentChannelSettings.ChannelMode mode : PigmentChannelSettings.ChannelMode.values()) {
                channelModeCache.put(mode.name(), mode);
            }
        }
        return channelModeCache.get(s);
    }

    @Nonnull
    public static Optional<IPigmentHandler> getPigmentHandlerFor(@Nullable ICapabilityProvider provider, @Nullable Direction direction) {
        if (provider == null) {
            return Optional.empty();
        } else if (Capabilities.PIGMENT_HANDLER_CAPABILITY != null && provider.getCapability(Capabilities.PIGMENT_HANDLER_CAPABILITY, direction).isPresent()) {
            return Optional.of(provider.getCapability(Capabilities.PIGMENT_HANDLER_CAPABILITY, direction)
                    .orElseThrow(() -> new IllegalArgumentException("IPigmentHandler is 'null' even though it said that its present")));
        } else if (direction != null && provider instanceof IPigmentHandler.ISidedPigmentHandler && ((IPigmentHandler.ISidedPigmentHandler) provider).getTanks(direction) >= 1) {
            return Optional.of((IPigmentHandler) provider);
        } else if (!(provider instanceof IPigmentHandler.ISidedPigmentHandler) && provider instanceof IPigmentHandler && ((IPigmentHandler) provider).getTanks() >= 1) {
            return Optional.of((IPigmentHandler) provider);
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public static PigmentStack insertPigment(IPigmentHandler handler, PigmentStack stack, @Nullable Direction direction, Action action) {
        if (handler instanceof IPigmentHandler.ISidedPigmentHandler) {
            return ((IPigmentHandler.ISidedPigmentHandler) handler).insertChemical(stack, direction, action);
        } else return handler.insertChemical(stack, action);
    }

    @Nonnull
    public static PigmentStack extractPigment(IPigmentHandler handler, long amount, @Nullable Direction direction, Action action) {
        if (handler instanceof IPigmentHandler.ISidedPigmentHandler) {
            return ((IPigmentHandler.ISidedPigmentHandler) handler).extractChemical(amount, direction, action);
        } else return handler.extractChemical(amount, action);
    }

    public static List<Pigment> getPigmentInTank(@Nonnull IPigmentHandler handler, @Nullable Direction direction) {
        List<Pigment> pigments = new ArrayList<>();
        if (handler instanceof IPigmentHandler.ISidedPigmentHandler) {
            for (int i = 0; i < ((IPigmentHandler.ISidedPigmentHandler) handler).getTanks(direction); i++) {
                pigments.add(((IPigmentHandler.ISidedPigmentHandler) handler).getChemicalInTank(i, direction).getType());
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                pigments.add(handler.getChemicalInTank(i).getType());
            }
        }
        return pigments;
    }

    public static List<Pigment> getPigmentInTank(@Nonnull IPigmentHandler handler) {
        return getPigmentInTank(handler, null);
    }

    public static long getPigmentCount(@Nonnull IPigmentHandler handler, @Nullable Direction direction, @Nullable Predicate<PigmentStack> filter) {
        long count = 0;
        if (handler instanceof IPigmentHandler.ISidedPigmentHandler) {
            for (int i = 0; i < ((IPigmentHandler.ISidedPigmentHandler) handler).getTanks(direction); i++) {
                PigmentStack stack = ((IPigmentHandler.ISidedPigmentHandler) handler).getChemicalInTank(i, direction);
                if (!stack.isEmpty() && (filter == null || filter.test(stack))) {
                    count += stack.getAmount();
                }
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                PigmentStack stack = handler.getChemicalInTank(i);
                if (!stack.isEmpty() && (filter == null || filter.test(stack))) {
                    count += stack.getAmount();
                }
            }
        }
        return count;
    }

    public static long getPigmentCount(@Nonnull IPigmentHandler handler, @Nullable Direction direction, @Nullable Pigment filter) {
        return getPigmentCount(handler, direction, (stack) -> filter == null || stack.getType() == filter);
    }

    public static long getPigmentCount(@Nonnull IPigmentHandler handler, @Nullable Direction direction, @Nullable PigmentStack filter) {
        return getPigmentCount(handler, direction, (stack) -> filter == null || stack.equals(filter));
    }

    public static long getPigmentCount(@Nonnull IPigmentHandler handler, @Nullable Direction direction) {
        return getPigmentCount(handler, direction, (Predicate<PigmentStack>) null);
    }

    public static long getPigmentCount(@Nonnull IPigmentHandler handler) {
        return getPigmentCount(handler, null);
    }

}
