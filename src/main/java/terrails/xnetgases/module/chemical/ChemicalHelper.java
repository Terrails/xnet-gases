package terrails.xnetgases.module.chemical;

import mekanism.api.Action;
import mekanism.api.chemical.*;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.tier.ChemicalTankTier;
import mekanism.common.util.ChemicalUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class ChemicalHelper {

    public static IChemicalHandler<?, ?> getChemicalHandler(@Nullable ItemStack stack, ChemicalType type) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        IChemicalHandler<?, ?> handler;
        switch (type) {
            case GAS -> {
                handler = Capabilities.GAS.getCapability(stack);
                if (handler == null) {
                    handler = ContainerType.GAS.getAttachmentIfPresent(stack);
                }
            }
            case INFUSION -> {
                handler = Capabilities.INFUSION.getCapability(stack);
                if (handler == null) {
                    handler = ContainerType.INFUSION.getAttachmentIfPresent(stack);
                }
            }
            case PIGMENT -> {
                handler = Capabilities.PIGMENT.getCapability(stack);
                if (handler == null) {
                    handler = ContainerType.PIGMENT.getAttachmentIfPresent(stack);
                }
            }
            case SLURRY -> {
                handler = Capabilities.SLURRY.getCapability(stack);
                if (handler == null) {
                    handler = ContainerType.SLURRY.getAttachmentIfPresent(stack);
                }
            }
            default -> throw new IncompatibleClassChangeError();
        }

        return handler;
    }

    public static IChemicalHandler<?, ?> getChemicalHandler(@Nullable Level level, @Nullable BlockEntity be, @Nullable Direction direction, ChemicalType type) {
        if (be == null) {
            return null;
        }

        return switch (type) {
            case GAS -> Capabilities.GAS.getCapabilityIfLoaded(level, be.getBlockPos(), null, be, direction);
            case INFUSION -> Capabilities.INFUSION.getCapabilityIfLoaded(level, be.getBlockPos(), null, be, direction);
            case PIGMENT -> Capabilities.PIGMENT.getCapabilityIfLoaded(level, be.getBlockPos(), null, be, direction);
            case SLURRY -> Capabilities.SLURRY.getCapabilityIfLoaded(level, be.getBlockPos(), null, be, direction);
            default -> throw new IncompatibleClassChangeError();
        };
    }

    public static IChemicalHandler<?, ?> getChemicalHandler(@Nullable Level level, @NotNull BlockPos pos, @Nullable Direction direction, ChemicalType type) {
        return switch (type) {
            case GAS -> Capabilities.GAS.getCapabilityIfLoaded(level, pos, direction);
            case INFUSION -> Capabilities.INFUSION.getCapabilityIfLoaded(level, pos, direction);
            case PIGMENT -> Capabilities.PIGMENT.getCapabilityIfLoaded(level, pos, direction);
            case SLURRY -> Capabilities.SLURRY.getCapabilityIfLoaded(level, pos, direction);
            default -> throw new IncompatibleClassChangeError();
        };
    }

    public static boolean isChemicalHandler(ItemStack stack) {
        for (ChemicalType type : ChemicalType.values()) {
            if (getChemicalHandler(stack, type) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isChemicalHandler(@Nullable Level level, @Nullable BlockEntity be, @Nullable Direction direction) {
        for (ChemicalType type : ChemicalType.values()) {
            if (getChemicalHandler(level, be, direction, type) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isChemicalHandler(@Nullable Level level, @NotNull BlockPos pos, @Nullable Direction direction) {
        for (ChemicalType type : ChemicalType.values()) {
            if (getChemicalHandler(level, pos, direction, type) != null) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> ChemicalStack<CHEMICAL> insert(@NotNull IChemicalHandler<CHEMICAL, STACK> handler, STACK stack, @Nullable Direction direction, Action action) {
        if (handler instanceof ISidedChemicalHandler<CHEMICAL, STACK> sidedHandler) {
            return sidedHandler.insertChemical(stack, direction, action);
        } else {
            return handler.insertChemical(stack, action);
        }
    }

    @NotNull
    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> ChemicalStack<CHEMICAL> extract(@NotNull IChemicalHandler<CHEMICAL, STACK> handler, long amount, @Nullable Direction direction, Action action) {
        if (handler instanceof ISidedChemicalHandler<CHEMICAL, STACK> sidedHandler) {
            return sidedHandler.extractChemical(amount, direction, action);
        } else {
            return handler.extractChemical(amount, action);
        }
    }

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> Set<CHEMICAL> chemicalsInTank(@NotNull IChemicalHandler<CHEMICAL, STACK> handler, @Nullable Direction direction) {
        Set<CHEMICAL> chemicals = new HashSet<>();
        if (handler instanceof ISidedChemicalHandler<CHEMICAL, STACK> sidedHandler) {
            for (int i = 0; i < sidedHandler.getTanks(direction); i++) {
                CHEMICAL chemical = sidedHandler.getChemicalInTank(i, direction).getType();
                if (!chemical.isEmptyType()) {
                    chemicals.add(chemical);
                }
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                CHEMICAL chemical = handler.getChemicalInTank(i).getType();
                if (!chemical.isEmptyType()) {
                    chemicals.add(chemical);
                }
            }
        }
        return chemicals;
    }

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> Set<CHEMICAL> chemicalsInTank(@NotNull IChemicalHandler<CHEMICAL, STACK> handler) {
        return chemicalsInTank(handler, null);
    }

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> long amountInTank(@NotNull IChemicalHandler<CHEMICAL, STACK> handler, @Nullable Direction direction, @Nullable Predicate<STACK> filter) {
        long count = 0;
        if (handler instanceof ISidedChemicalHandler<CHEMICAL, STACK> sidedHandler) {
            for (int i = 0; i < sidedHandler.getTanks(direction); i++) {
                STACK stack = sidedHandler.getChemicalInTank(i, direction);
                if (!stack.isEmpty() && (filter == null || filter.test(stack))) {
                    count += stack.getAmount();
                }
            }
        } else {
            for (int i = 0; i < handler.getTanks(); i++) {
                STACK stack = handler.getChemicalInTank(i);
                if (!stack.isEmpty() && (filter == null || filter.test(stack))) {
                    count += stack.getAmount();
                }
            }
        }
        return count;
    }

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> long amountInTank(@NotNull IChemicalHandler<CHEMICAL, STACK> handler, @Nullable Direction direction, @Nullable STACK filter) {
        Predicate<STACK> predicate;
        if (filter == null) {
            predicate = null;
        } else {
            predicate = (stack) -> stack.isTypeEqual(filter);
        }

        return amountInTank(handler, direction, predicate);
    }

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> long amountInTank(@NotNull IChemicalHandler<CHEMICAL, STACK> handler, @Nullable Direction direction, @Nullable CHEMICAL filter) {
        Predicate<STACK> predicate;
        if (filter == null) {
            predicate = null;
        } else {
            predicate = (stack) -> stack.getType() == filter;
        }

        return amountInTank(handler, direction, predicate);
    }

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> long amountInTank(@NotNull IChemicalHandler<CHEMICAL, STACK> handler, @Nullable Direction direction) {
        return amountInTank(handler, direction, (Predicate<STACK>) null);
    }

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> long amountInTank(@NotNull IChemicalHandler<CHEMICAL, STACK> handler) {
        return amountInTank(handler, null);
    }

    /**
     * Turns any valid ItemStack with a chemical capability into a creative tank containing the chemical
     */
    public static ItemStack normalizeStack(ItemStack stack, ChemicalType type) {
        if (stack == null || type == null) {
            return ItemStack.EMPTY;
        }

        IChemicalHandler<?, ?> handler = getChemicalHandler(stack, type);
        if (handler == null || handler.getTanks() <= 0) {
            return ItemStack.EMPTY;
        }

        ChemicalStack<?> chemicalStack = handler.getChemicalInTank(0);
        if (chemicalStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Chemical<?> chemical = chemicalStack.getType();
        return ChemicalUtil.getFullChemicalTank(ChemicalTankTier.CREATIVE, chemical);
    }

}
