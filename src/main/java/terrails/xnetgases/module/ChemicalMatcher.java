package terrails.xnetgases.module;

import mekanism.api.chemical.*;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tier.ChemicalTankTier;
import mekanism.common.util.ChemicalUtil;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * If the item is {@link ItemStack#EMPTY} or does not have {@link Capabilities#CHEMICAL}, the filter does nothing and allows all chemicals.
 * If the item has the capability, but {@link ChemicalStack#isEmpty()}, the filter is active, but always false.
 * If the item has the capability and the chemical is not empty, the filter is active for the chemical.
 */
public class ChemicalMatcher implements Predicate<ChemicalStack> {

    public static final ChemicalMatcher EMPTY = new ChemicalMatcher(ItemStack.EMPTY, Predicate.not(ChemicalStack::isEmpty));
    private static final Predicate<ChemicalStack> ALWAYS_FALSE = s -> false;

    public static ChemicalMatcher from(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            return Optional.ofNullable(Capabilities.CHEMICAL.getCapability(stack))
                    .map(handler -> handler.getChemicalInTank(0))
                    .map(chemicalStack -> {
                        if (chemicalStack.isEmpty()) {
                            return new ChemicalMatcher(new ItemStack(MekanismBlocks.CREATIVE_CHEMICAL_TANK), ALWAYS_FALSE);
                        } else {
                            Holder<Chemical> chemical = chemicalStack.getChemicalHolder();
                            return new ChemicalMatcher(ChemicalUtil.getFullChemicalTank(ChemicalTankTier.CREATIVE, chemical), s -> !s.isEmpty() && s.is(chemical));
                        }
                    }).orElse(EMPTY);
        }
        return EMPTY;
    }

    private final ItemStack stack;
    private final Predicate<ChemicalStack> stackPredicate;

    private ChemicalMatcher(ItemStack stack, Predicate<ChemicalStack> stackPredicate) {
        this.stack = stack;
        this.stackPredicate = stackPredicate;
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    public boolean test(ChemicalStack stack) {
        return stackPredicate.test(stack);
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public long amountInTank(IChemicalHandler handler, @Nullable Direction direction) {
        if (handler instanceof IMekanismChemicalHandler mekHandler && !mekHandler.canHandleChemicals()) {
            return 0;
        }

        List<ChemicalStack> stacks = new LinkedList<>();
        if (direction != null && handler instanceof ISidedChemicalHandler sided) {
            for (int i = 0; i < sided.getChemicalTanks(); i++) {
                stacks.add(sided.getChemicalInTank(i, direction));
            }
        } else {
            for (int i = 0; i < handler.getChemicalTanks(); i++) {
                stacks.add(handler.getChemicalInTank(i));
            }
        }

        return stacks.stream().filter(this).mapToLong(ChemicalStack::getAmount).sum();
    }
}