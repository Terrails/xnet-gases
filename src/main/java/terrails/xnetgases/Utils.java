package terrails.xnetgases;

import mekanism.api.Action;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.gas.IGasHandler.ISidedGasHandler;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.ISlurryHandler.ISidedSlurryHandler;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import terrails.xnetgases.gas.GasChannelSettings;
import terrails.xnetgases.gas.GasConnectorSettings;
import terrails.xnetgases.logic.XGLogicConnectorSettings;
import terrails.xnetgases.logic.XGSensor;
import terrails.xnetgases.slurry.SlurryChannelSettings;
import terrails.xnetgases.slurry.SlurryConnectorSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Utils {

    private static Map<String, GasConnectorSettings.GasMode> connectorModeCache;
    private static Map<String, GasChannelSettings.ChannelMode> channelModeCache;

    private static Map<String, SlurryConnectorSettings.SlurryMode> slurryConnectorModeCache;
    private static Map<String, SlurryChannelSettings.ChannelMode> slurryChannelModeCache;

    private static Map<String, XGLogicConnectorSettings.LogicMode> logicConnectorModeCache;

    private static Map<String, XGSensor.SensorMode> sensorModeCache;
    private static Map<String, XGSensor.Operator> operatorModeCache;


    @Nonnull
    public static GasConnectorSettings.GasMode getGasConnectorModeFrom(String s) {
        if (connectorModeCache == null) {
            connectorModeCache = new HashMap<>();
            for (GasConnectorSettings.GasMode mode : GasConnectorSettings.GasMode.values()) {
                connectorModeCache.put(mode.name(), mode);
            }
        }
        return connectorModeCache.get(s);
    }

    @Nonnull
    public static GasChannelSettings.ChannelMode getGasChannelModeFrom(String s) {
        if (channelModeCache == null) {
            channelModeCache = new HashMap<>();
            for (GasChannelSettings.ChannelMode mode : GasChannelSettings.ChannelMode.values()) {
                channelModeCache.put(mode.name(), mode);
            }
        }
        return channelModeCache.get(s);
    }

    @Nonnull
    public static SlurryConnectorSettings.SlurryMode getSlurryConnectorModeFrom(String s) {
        if (slurryConnectorModeCache == null) {
            slurryConnectorModeCache = new HashMap<>();
            for (SlurryConnectorSettings.SlurryMode mode : SlurryConnectorSettings.SlurryMode.values()) {
                slurryConnectorModeCache.put(mode.name(), mode);
            }
        }
        return slurryConnectorModeCache.get(s);
    }

    @Nonnull
    public static SlurryChannelSettings.ChannelMode getSlurryChannelModeFrom(String s) {
        if (slurryChannelModeCache == null) {
            slurryChannelModeCache = new HashMap<>();
            for (SlurryChannelSettings.ChannelMode mode : SlurryChannelSettings.ChannelMode.values()) {
                slurryChannelModeCache.put(mode.name(), mode);
            }
        }
        return slurryChannelModeCache.get(s);
    }

    @Nonnull
    public static XGLogicConnectorSettings.LogicMode getLogicConnectorModeFrom(String s) {
        if (logicConnectorModeCache == null) {
            logicConnectorModeCache = new HashMap<>();
            for (XGLogicConnectorSettings.LogicMode mode : XGLogicConnectorSettings.LogicMode.values()) {
                logicConnectorModeCache.put(mode.name(), mode);
            }
        }
        return logicConnectorModeCache.get(s);
    }

    @Nonnull
    public static XGSensor.SensorMode getLogicSensorModeFrom(String s) {
        if (sensorModeCache == null) {
            sensorModeCache = new HashMap<>();
            for (XGSensor.SensorMode mode : XGSensor.SensorMode.values()) {
                sensorModeCache.put(mode.name(), mode);
            }
        }
        return sensorModeCache.get(s);
    }

    @Nonnull
    public static XGSensor.Operator getLogicOperatorModeFrom(String s) {
        if (operatorModeCache == null) {
            operatorModeCache = new HashMap<>();
            for (XGSensor.Operator mode : XGSensor.Operator.values()) {
                operatorModeCache.put(mode.name(), mode);
            }
        }
        return operatorModeCache.get(s);
    }

    @Nonnull
    public static Optional<IGasHandler> getGasHandlerFor(@Nullable ICapabilityProvider provider, @Nullable Direction direction) {
        if (provider == null) {
            return Optional.empty();
        } else if (Capabilities.GAS_HANDLER_CAPABILITY != null && provider.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, direction).isPresent()) {
            return Optional.of(provider.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, direction)
                    .orElseThrow(() -> new IllegalArgumentException("IGasHandler is 'null' even though it said that its present")));
        } else if (direction != null && provider instanceof ISidedGasHandler && ((ISidedGasHandler) provider).getTanks(direction) >= 1) {
            return Optional.of((IGasHandler) provider);
        } else if (!(provider instanceof ISidedGasHandler) && provider instanceof IGasHandler && ((IGasHandler) provider).getTanks() >= 1) {
            return Optional.of((IGasHandler) provider);
        } else {
            return Optional.empty();
        }
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
    public static GasStack insertGas(IGasHandler handler, GasStack stack, @Nullable Direction direction, Action action) {
        if (direction != null && handler instanceof ISidedGasHandler) {
            return ((ISidedGasHandler) handler).insertChemical(stack, direction, action);
        } else return handler.insertChemical(stack, action);
    }

    @Nonnull
    public static GasStack extractGas(IGasHandler handler, long amount, @Nullable Direction direction, Action action) {
        if (direction != null && handler instanceof ISidedGasHandler) {
            return ((ISidedGasHandler) handler).extractChemical(amount, direction, action);
        } else return handler.extractChemical(amount, action);
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

    public static long getGasCount(@Nonnull IGasHandler handler, @Nullable Gas matcher, @Nullable Direction direction) {
        long count = 0;
        if (direction != null && handler instanceof ISidedGasHandler) {
            for (int i = 0; i < ((ISidedGasHandler) handler).getTanks(direction); i++) {
                GasStack stack = ((ISidedGasHandler) handler).getChemicalInTank(i, direction);
                if (!stack.isEmpty() && (matcher == null || matcher == stack.getType())) {
                    count += stack.getAmount();
                }
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                GasStack stack = handler.getChemicalInTank(i);
                if (!stack.isEmpty() && (matcher == null || matcher == stack.getType())) {
                    count += stack.getAmount();
                }
            }
        }
        return count;
    }

    public static long getGasCount(@Nonnull IGasHandler handler, @Nullable GasStack matcher, @Nullable Direction direction) {
        return getGasCount(handler, matcher == null || matcher.isEmpty() ? null : matcher.getType(), direction);
    }

    public static long getSlurryCount(@Nonnull ISlurryHandler handler, @Nullable Slurry matcher, @Nullable Direction direction) {
        long count = 0;
        if (direction != null && handler instanceof ISidedSlurryHandler) {
            for (int i = 0; i < ((ISidedSlurryHandler) handler).getTanks(direction); i++) {
                SlurryStack stack = ((ISidedSlurryHandler) handler).getChemicalInTank(i, direction);
                if (!stack.isEmpty() && (matcher == null || matcher == stack.getType())) {
                    count += stack.getAmount();
                }
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                SlurryStack stack = handler.getChemicalInTank(i);
                if (!stack.isEmpty() && (matcher == null || matcher == stack.getType())) {
                    count += stack.getAmount();
                }
            }
        }

        return count;
    }

    public static long getSlurryCount(@Nonnull ISlurryHandler handler, @Nullable SlurryStack matcher, @Nullable Direction direction) {
        return getSlurryCount(handler, matcher == null || matcher.isEmpty() ? null : matcher.getType(), direction);
    }
}
