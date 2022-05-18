package terrails.xnetgases.module.chemical.utils;

import mekanism.api.Action;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class GasHelper {

    @Nonnull
    public static Optional<IGasHandler> handler(@Nullable ICapabilityProvider provider, @Nullable Direction direction) {
        if (provider == null) {
            return Optional.empty();
        } else if (Capabilities.GAS_HANDLER_CAPABILITY != null && provider.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, direction).isPresent()) {
            return Optional.of(provider.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, direction)
                    .orElseThrow(() -> new IllegalArgumentException("IGasHandler is 'null' even though it said that its present")));
        } else if (direction != null && provider instanceof IGasHandler.ISidedGasHandler && ((IGasHandler.ISidedGasHandler) provider).getTanks(direction) >= 1) {
            return Optional.of((IGasHandler) provider);
        } else if (!(provider instanceof IGasHandler.ISidedGasHandler) && provider instanceof IGasHandler && ((IGasHandler) provider).getTanks() >= 1) {
            return Optional.of((IGasHandler) provider);
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public static GasStack insert(IGasHandler handler, GasStack stack, @Nullable Direction direction, Action action) {
        if (handler instanceof IGasHandler.ISidedGasHandler) {
            return ((IGasHandler.ISidedGasHandler) handler).insertChemical(stack, direction, action);
        } else return handler.insertChemical(stack, action);
    }

    @Nonnull
    public static GasStack extract(IGasHandler handler, long amount, @Nullable Direction direction, Action action) {
        if (handler instanceof IGasHandler.ISidedGasHandler) {
            return ((IGasHandler.ISidedGasHandler) handler).extractChemical(amount, direction, action);
        } else return handler.extractChemical(amount, action);
    }

    public static List<Gas> chemicalInTank(@Nonnull IGasHandler handler, @Nullable Direction direction) {
        List<Gas> gases = new ArrayList<>();
        if (handler instanceof IGasHandler.ISidedGasHandler) {
            for (int i = 0; i < ((IGasHandler.ISidedGasHandler) handler).getTanks(direction); i++) {
                Gas gas = ((IGasHandler.ISidedGasHandler) handler).getChemicalInTank(i, direction).getType();
                if (!gas.isEmptyType()) gases.add(gas);
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                Gas gas = handler.getChemicalInTank(i).getType();
                if (!gas.isEmptyType()) gases.add(gas);
            }
        }
        return gases;
    }

    public static List<Gas> chemicalInTank(@Nonnull IGasHandler handler) {
        return chemicalInTank(handler, null);
    }

    public static long amountInTank(@Nonnull IGasHandler handler, @Nullable Direction direction, @Nullable Predicate<GasStack> filter) {
        long count = 0;
        if (handler instanceof IGasHandler.ISidedGasHandler) {
            for (int i = 0; i < ((IGasHandler.ISidedGasHandler) handler).getTanks(direction); i++) {
                GasStack stack = ((IGasHandler.ISidedGasHandler) handler).getChemicalInTank(i, direction);
                if (!stack.isEmpty() && (filter == null || filter.test(stack))) {
                    count += stack.getAmount();
                }
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                GasStack stack = handler.getChemicalInTank(i);
                if (!stack.isEmpty() && (filter == null || filter.test(stack))) {
                    count += stack.getAmount();
                }
            }
        }
        return count;
    }

    public static long amountInTank(@Nonnull IGasHandler handler, @Nullable Direction direction, @Nullable Gas filter) {
        return amountInTank(handler, direction, (stack) -> filter == null || stack.getType() == filter);
    }

    public static long amountInTank(@Nonnull IGasHandler handler, @Nullable Direction direction, @Nullable GasStack filter) {
        return amountInTank(handler, direction, (stack) -> filter == null || stack.isTypeEqual(filter));
    }

    public static long amountInTank(@Nonnull IGasHandler handler, @Nullable Direction direction) {
        return amountInTank(handler, direction, (Predicate<GasStack>) null);
    }

    public static long amountInTank(@Nonnull IGasHandler handler) {
        return amountInTank(handler, null);
    }

}
