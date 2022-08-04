package terrails.xnetgases.module.chemical.utils;

import mekanism.api.Action;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SlurryHelper {

    @Nonnull
    public static Optional<ISlurryHandler> handler(@Nullable ICapabilityProvider provider, @Nullable Direction direction) {
        if (provider == null) {
            return Optional.empty();
        } else if (Capabilities.SLURRY_HANDLER != null && provider.getCapability(Capabilities.SLURRY_HANDLER, direction).isPresent()) {
            return Optional.of(provider.getCapability(Capabilities.SLURRY_HANDLER, direction)
                    .orElseThrow(() -> new IllegalArgumentException("ISlurryHandler is 'null' even though it said that its present")));
        } else if (direction != null && provider instanceof ISlurryHandler.ISidedSlurryHandler && ((ISlurryHandler.ISidedSlurryHandler) provider).getTanks(direction) >= 1) {
            return Optional.of((ISlurryHandler) provider);
        } else if (!(provider instanceof ISlurryHandler.ISidedSlurryHandler) && provider instanceof ISlurryHandler && ((ISlurryHandler) provider).getTanks() >= 1) {
            return Optional.of((ISlurryHandler) provider);
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public static SlurryStack insert(ISlurryHandler handler, SlurryStack stack, @Nullable Direction direction, Action action) {
        if (handler instanceof ISlurryHandler.ISidedSlurryHandler) {
            return ((ISlurryHandler.ISidedSlurryHandler) handler).insertChemical(stack, direction, action);
        } else return handler.insertChemical(stack, action);
    }

    @Nonnull
    public static SlurryStack extract(ISlurryHandler handler, long amount, @Nullable Direction direction, Action action) {
        if (handler instanceof ISlurryHandler.ISidedSlurryHandler) {
            return ((ISlurryHandler.ISidedSlurryHandler) handler).extractChemical(amount, direction, action);
        } else {
            return handler.extractChemical(amount, action);
        }
    }

    public static List<Slurry> chemicalInTank(@Nonnull ISlurryHandler handler, @Nullable Direction direction) {
        List<Slurry> slurries = new ArrayList<>();
        if (handler instanceof ISlurryHandler.ISidedSlurryHandler) {
            for (int i = 0; i < ((ISlurryHandler.ISidedSlurryHandler) handler).getTanks(direction); i++) {
                Slurry slurry = ((ISlurryHandler.ISidedSlurryHandler) handler).getChemicalInTank(i, direction).getType();
                if (!slurry.isEmptyType()) slurries.add(slurry);
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                Slurry slurry = handler.getChemicalInTank(i).getType();
                if (!slurry.isEmptyType()) slurries.add(slurry);
            }
        }
        return slurries;
    }

    public static List<Slurry> chemicalInTank(@Nonnull ISlurryHandler handler) {
        return chemicalInTank(handler, null);
    }

    public static long amountInTank(@Nonnull ISlurryHandler handler, @Nullable Direction direction, @Nullable Predicate<SlurryStack> filter) {
        long count = 0;
        if (handler instanceof ISlurryHandler.ISidedSlurryHandler) {
            for (int i = 0; i < ((ISlurryHandler.ISidedSlurryHandler) handler).getTanks(direction); i++) {
                SlurryStack stack = ((ISlurryHandler.ISidedSlurryHandler) handler).getChemicalInTank(i, direction);
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

    public static long amountInTank(@Nonnull ISlurryHandler handler, @Nullable Direction direction, @Nullable Slurry filter) {
        return amountInTank(handler, direction, (stack) -> filter == null || stack.getType() == filter);
    }

    public static long amountInTank(@Nonnull ISlurryHandler handler, @Nullable Direction direction, @Nullable SlurryStack filter) {
        return amountInTank(handler, direction, (stack) -> filter == null || stack.isTypeEqual(filter));
    }

    public static long amountInTank(@Nonnull ISlurryHandler handler, @Nullable Direction direction) {
        return amountInTank(handler, direction, (Predicate<SlurryStack>) null);
    }

    public static long amountInTank(@Nonnull ISlurryHandler handler) {
        return amountInTank(handler, null);
    }

}
