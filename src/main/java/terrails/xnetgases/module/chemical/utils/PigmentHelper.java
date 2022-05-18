package terrails.xnetgases.module.chemical.utils;

import mekanism.api.Action;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class PigmentHelper {

    @Nonnull
    public static Optional<IPigmentHandler> handler(@Nullable ICapabilityProvider provider, @Nullable Direction direction) {
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
    public static PigmentStack insert(IPigmentHandler handler, PigmentStack stack, @Nullable Direction direction, Action action) {
        if (handler instanceof IPigmentHandler.ISidedPigmentHandler) {
            return ((IPigmentHandler.ISidedPigmentHandler) handler).insertChemical(stack, direction, action);
        } else return handler.insertChemical(stack, action);
    }

    @Nonnull
    public static PigmentStack extract(IPigmentHandler handler, long amount, @Nullable Direction direction, Action action) {
        if (handler instanceof IPigmentHandler.ISidedPigmentHandler) {
            return ((IPigmentHandler.ISidedPigmentHandler) handler).extractChemical(amount, direction, action);
        } else return handler.extractChemical(amount, action);
    }

    public static List<Pigment> chemicalInTank(@Nonnull IPigmentHandler handler, @Nullable Direction direction) {
        List<Pigment> pigments = new ArrayList<>();
        if (handler instanceof IPigmentHandler.ISidedPigmentHandler) {
            for (int i = 0; i < ((IPigmentHandler.ISidedPigmentHandler) handler).getTanks(direction); i++) {
                Pigment pigment = ((IPigmentHandler.ISidedPigmentHandler) handler).getChemicalInTank(i, direction).getType();
                if (!pigment.isEmptyType()) pigments.add(pigment);
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                Pigment pigment = handler.getChemicalInTank(i).getType();
                if (!pigment.isEmptyType()) pigments.add(pigment);
            }
        }
        return pigments;
    }

    public static List<Pigment> chemicalInTank(@Nonnull IPigmentHandler handler) {
        return chemicalInTank(handler, null);
    }

    public static long amountInTank(@Nonnull IPigmentHandler handler, @Nullable Direction direction, @Nullable Predicate<PigmentStack> filter) {
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

    public static long amountInTank(@Nonnull IPigmentHandler handler, @Nullable Direction direction, @Nullable Pigment filter) {
        return amountInTank(handler, direction, (stack) -> filter == null || stack.getType() == filter);
    }

    public static long amountInTank(@Nonnull IPigmentHandler handler, @Nullable Direction direction, @Nullable PigmentStack filter) {
        return amountInTank(handler, direction, (stack) -> filter == null || stack.isTypeEqual(filter));
    }

    public static long amountInTank(@Nonnull IPigmentHandler handler, @Nullable Direction direction) {
        return amountInTank(handler, direction, (Predicate<PigmentStack>) null);
    }

    public static long amountInTank(@Nonnull IPigmentHandler handler) {
        return amountInTank(handler, null);
    }

}
