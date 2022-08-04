package terrails.xnetgases.module.chemical.utils;

import mekanism.api.Action;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class InfuseHelper {

    @Nonnull
    public static Optional<IInfusionHandler> handler(@Nullable ICapabilityProvider provider, @Nullable Direction direction) {
        if (provider == null) {
            return Optional.empty();
        } else if (Capabilities.INFUSION_HANDLER != null && provider.getCapability(Capabilities.INFUSION_HANDLER, direction).isPresent()) {
            return Optional.of(provider.getCapability(Capabilities.INFUSION_HANDLER, direction)
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
    public static InfusionStack insert(IInfusionHandler handler, InfusionStack stack, @Nullable Direction direction, Action action) {
        if (handler instanceof IInfusionHandler.ISidedInfusionHandler) {
            return ((IInfusionHandler.ISidedInfusionHandler) handler).insertChemical(stack, direction, action);
        } else return handler.insertChemical(stack, action);
    }

    @Nonnull
    public static InfusionStack extract(IInfusionHandler handler, long amount, @Nullable Direction direction, Action action) {
        if (handler instanceof IInfusionHandler.ISidedInfusionHandler) {
            return ((IInfusionHandler.ISidedInfusionHandler) handler).extractChemical(amount, direction, action);
        } else return handler.extractChemical(amount, action);
    }

    public static List<InfuseType> chemicalInTank(@Nonnull IInfusionHandler handler, @Nullable Direction direction) {
        List<InfuseType> infuses = new ArrayList<>();
        if (handler instanceof IInfusionHandler.ISidedInfusionHandler) {
            for (int i = 0; i < ((IInfusionHandler.ISidedInfusionHandler) handler).getTanks(direction); i++) {
                InfuseType infuse = ((IInfusionHandler.ISidedInfusionHandler) handler).getChemicalInTank(i, direction).getType();
                if (!infuse.isEmptyType()) infuses.add(infuse);
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                InfuseType infuse = handler.getChemicalInTank(i).getType();
                if (!infuse.isEmptyType()) infuses.add(infuse);
            }
        }
        return infuses;
    }

    public static List<InfuseType> chemicalInTank(@Nonnull IInfusionHandler handler) {
        return chemicalInTank(handler, null);
    }

    public static long amountInTank(@Nonnull IInfusionHandler handler, @Nullable Direction direction, @Nullable Predicate<InfusionStack> filter) {
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

    public static long amountInTank(@Nonnull IInfusionHandler handler, @Nullable Direction direction, @Nullable InfuseType filter) {
        return amountInTank(handler, direction, (stack) -> filter == null || stack.getType() == filter);
    }

    public static long amountInTank(@Nonnull IInfusionHandler handler, @Nullable Direction direction, @Nullable InfusionStack filter) {
        return amountInTank(handler, direction, (stack) -> filter == null || stack.isTypeEqual(filter));
    }

    public static long amountInTank(@Nonnull IInfusionHandler handler, @Nullable Direction direction) {
        return amountInTank(handler, direction, (Predicate<InfusionStack>) null);
    }

    public static long amountInTank(@Nonnull IInfusionHandler handler) {
        return amountInTank(handler, null);
    }

}
