package terrails.xnetgasses;

import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.gas.ISidedGasHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import terrails.xnetgasses.gas.GasChannelSettings;
import terrails.xnetgasses.gas.GasConnectorSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Utils {

    private static Map<String, GasConnectorSettings.GasMode> connectorModeCache;
    private static Map<String, GasChannelSettings.ChannelMode> channelModeCache;

    @Nonnull
    public static GasConnectorSettings.GasMode getConnectorModeFrom(String s) {
        if (connectorModeCache == null) {
            connectorModeCache = new HashMap<>();
            for (GasConnectorSettings.GasMode mode : GasConnectorSettings.GasMode.values()) {
                connectorModeCache.put(mode.name(), mode);
            }
        }
        return connectorModeCache.get(s);
    }

    @Nonnull
    public static GasChannelSettings.ChannelMode getChannelModeFrom(String s) {
        if (channelModeCache == null) {
            channelModeCache = new HashMap<>();
            for (GasChannelSettings.ChannelMode mode : GasChannelSettings.ChannelMode.values()) {
                channelModeCache.put(mode.name(), mode);
            }
        }
        return channelModeCache.get(s);
    }

    @Nonnull
    public static Optional<IGasHandler> getGasHandlerFor(@Nullable ICapabilityProvider provider, @Nullable Direction direction) {
        if (provider == null) {
            return Optional.empty();
        } else if (Capabilities.GAS_HANDLER_CAPABILITY != null && provider.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, direction).isPresent()) {
            return Optional.of(provider.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, direction)
                    .orElseThrow(() -> new IllegalArgumentException("IGasHandler is 'null' even though it said that its present")));
        } else if (direction != null && provider instanceof ISidedGasHandler && ((ISidedGasHandler) provider).getGasTankCount(direction) >= 1) {
            return Optional.of((IGasHandler) provider);
        } else if (!(provider instanceof ISidedGasHandler) && provider instanceof IGasHandler && ((IGasHandler) provider).getGasTankCount() >= 1) {
            return Optional.of((IGasHandler) provider);
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public static GasStack insertGas(IGasHandler handler, GasStack stack, @Nullable Direction direction, Action action) {
        if (direction != null && handler instanceof ISidedGasHandler) {
            return ((ISidedGasHandler) handler).insertGas(stack, direction, action);
        } else return handler.insertGas(stack, action);
    }

    @Nonnull
    public static GasStack extractGas(IGasHandler handler, long amount, @Nullable Direction direction, Action action) {
        if (direction != null && handler instanceof ISidedGasHandler) {
            return ((ISidedGasHandler) handler).extractGas(amount, direction, action);
        } else return handler.extractGas(amount, action);
    }

    public static long getGasCount(@Nonnull IGasHandler handler, @Nullable GasStack matcher, @Nullable Direction direction) {
        int count = 0;
        if (direction != null && handler instanceof ISidedGasHandler) {
            for (int i = 0; i < ((ISidedGasHandler) handler).getGasTankCount(direction); i++) {
                GasStack stack = ((ISidedGasHandler) handler).getGasInTank(i, direction);
                if (!stack.isEmpty() && (matcher == null || matcher.equals(stack))) {
                    count += stack.getAmount();
                }
            }
        } else {
            for (int i = 0; i < handler.getGasTankCount(); i++) {
                GasStack stack = handler.getGasInTank(i);
                if (!stack.isEmpty() && (matcher == null || matcher.equals(stack))) {
                    count += stack.getAmount();
                }
            }
        }
        return count;
    }
}
