package terrails.xnetgases.module.chemical.utils;

import mekanism.api.Action;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tier.ChemicalTankTier;
import mekanism.common.util.ChemicalUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import terrails.xnetgases.module.chemical.ChemicalEnums;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ChemicalHelper {

    public static Optional<IChemicalHandler<?, ?>> handler(ICapabilityProvider provider, Direction direction, ChemicalEnums.Type type) {
        IChemicalHandler<?, ?> handler = null;
        switch (type) {
            case GAS -> {
                Optional<IGasHandler> opt = GasHelper.handler(provider, direction);
                if (opt.isPresent()) handler = opt.get();
            }
            case INFUSE -> {
                Optional<IInfusionHandler> opt = InfuseHelper.handler(provider, direction);
                if (opt.isPresent()) handler = opt.get();
            }
            case PIGMENT -> {
                Optional<IPigmentHandler> opt = PigmentHelper.handler(provider, direction);
                if (opt.isPresent()) handler = opt.get();
            }
            case SLURRY -> {
                Optional<ISlurryHandler> opt = SlurryHelper.handler(provider, direction);
                if (opt.isPresent()) handler = opt.get();
            }
        }
        return Optional.ofNullable(handler);
    }

    public static boolean handlerPresent(ICapabilityProvider provider, Direction direction) {
        for (ChemicalEnums.Type type : ChemicalEnums.Type.values()) {
            if (handler(provider, direction, type).isPresent()) return true;
        }
        return false;
    }

    @Nonnull
    public static ChemicalStack<?> insert(IChemicalHandler<?, ?> handler, ChemicalStack<?> stack, @Nullable Direction direction, Action action, ChemicalEnums.Type type) {
        return switch (type) {
            case GAS -> GasHelper.insert((IGasHandler) handler, (GasStack) stack, direction, action);
            case INFUSE -> InfuseHelper.insert((IInfusionHandler) handler, (InfusionStack) stack, direction, action);
            case PIGMENT -> PigmentHelper.insert((IPigmentHandler) handler, (PigmentStack) stack, direction, action);
            case SLURRY -> SlurryHelper.insert((ISlurryHandler) handler, (SlurryStack) stack, direction, action);
        };
    }

    @Nonnull
    public static ChemicalStack<?> extract(IChemicalHandler<?, ?> handler, long amount, @Nullable Direction direction, Action action, ChemicalEnums.Type type) {
        return switch (type) {
            case GAS -> GasHelper.extract((IGasHandler) handler, amount, direction, action);
            case INFUSE -> InfuseHelper.extract((IInfusionHandler) handler, amount, direction, action);
            case PIGMENT -> PigmentHelper.extract((IPigmentHandler) handler, amount, direction, action);
            case SLURRY -> SlurryHelper.extract((ISlurryHandler) handler, amount, direction, action);
        };
    }

    public static List<Chemical<?>> chemicalInTank(@Nonnull IChemicalHandler<?, ?> handler, @Nullable Direction direction, ChemicalEnums.Type type) {
        return switch (type) {
            case GAS -> GasHelper.chemicalInTank((IGasHandler) handler, direction).stream().map(chemical -> (Chemical<?>) chemical).collect(Collectors.toList());
            case INFUSE -> InfuseHelper.chemicalInTank((IInfusionHandler) handler, direction).stream().map(chemical -> (Chemical<?>) chemical).collect(Collectors.toList());
            case PIGMENT -> PigmentHelper.chemicalInTank((IPigmentHandler) handler, direction).stream().map(chemical -> (Chemical<?>) chemical).collect(Collectors.toList());
            case SLURRY -> SlurryHelper.chemicalInTank((ISlurryHandler) handler, direction).stream().map(chemical -> (Chemical<?>) chemical).collect(Collectors.toList());
        };
    }

    public static List<Chemical<?>> chemicalInTank(@Nonnull IChemicalHandler<?, ?> handler, ChemicalEnums.Type type) {
        return chemicalInTank(handler, null, type);
    }

    public static long amountInTank(@Nonnull IChemicalHandler<?, ?> handler, @Nullable Direction direction, @Nullable Predicate<ChemicalStack<?>> filter, ChemicalEnums.Type type) {
        return switch (type) {
            case GAS -> GasHelper.amountInTank((IGasHandler) handler, direction, stack -> filter == null || filter.test(stack));
            case INFUSE -> InfuseHelper.amountInTank((IInfusionHandler) handler, direction, stack -> filter == null || filter.test(stack));
            case PIGMENT -> PigmentHelper.amountInTank((IPigmentHandler) handler, direction, stack -> filter == null || filter.test(stack));
            case SLURRY -> SlurryHelper.amountInTank((ISlurryHandler) handler, direction, stack -> filter == null || filter.test(stack));
        };
    }

    public static long amountInTank(@Nonnull IChemicalHandler<?, ?> handler, @Nullable Direction direction, @Nullable Chemical<?> filter, ChemicalEnums.Type type) {
        return amountInTank(handler, direction, (stack) -> filter == null || stack.getType() == filter, type);
    }

    public static long amountInTank(@Nonnull IChemicalHandler<?, ?> handler, @Nullable Direction direction, @Nullable ChemicalStack<?> filter, ChemicalEnums.Type type) {
        return amountInTank(handler, direction, (stack) -> filter == null || stack.getType() == filter.getType(), type);
    }

    public static long amountInTank(@Nonnull IChemicalHandler<?, ?> handler, @Nullable Direction direction, ChemicalEnums.Type type) {
        return amountInTank(handler, direction, (Predicate<ChemicalStack< ?>>) null, type);
    }

    public static long amountInTank(@Nonnull IChemicalHandler<?, ?> handler, ChemicalEnums.Type type) {
        return amountInTank(handler, null, type);
    }

    /*
        Turns any valid ItemStack with any chemical capability into a creative tank containing the chemical
     */
    public static ItemStack normalizeStack(ItemStack stack, ChemicalEnums.Type type) {
        if (stack == null || type == null) {
            return ItemStack.EMPTY;
        } else if (!stack.sameItem(MekanismBlocks.CREATIVE_CHEMICAL_TANK.getItemStack())) {

            Chemical<?> chemical = null;
            switch (type) {
                case GAS -> {
                    Optional<IGasHandler> opt = stack.getCapability(Capabilities.GAS_HANDLER).resolve();
                    if (opt.isPresent()) {
                        GasStack gas = opt.get().getChemicalInTank(0);
                        if (!gas.isEmpty()) chemical = gas.getRaw();
                    }
                }
                case INFUSE -> {
                    Optional<IInfusionHandler> opt = stack.getCapability(Capabilities.INFUSION_HANDLER).resolve();
                    if (opt.isPresent()) {
                        InfusionStack infuse = opt.get().getChemicalInTank(0);
                        if (!infuse.isEmpty()) chemical = infuse.getRaw();
                    }
                }
                case PIGMENT -> {
                    Optional<IPigmentHandler> opt = stack.getCapability(Capabilities.PIGMENT_HANDLER).resolve();
                    if (opt.isPresent()) {
                        PigmentStack pigment = opt.get().getChemicalInTank(0);
                        if (!pigment.isEmpty()) chemical = pigment.getRaw();
                    }
                }
                case SLURRY -> {
                    Optional<ISlurryHandler> opt = stack.getCapability(Capabilities.SLURRY_HANDLER).resolve();
                    if (opt.isPresent()) {
                        SlurryStack slurry = opt.get().getChemicalInTank(0);
                        if (!slurry.isEmpty()) chemical = slurry.getRaw();
                    }
                }
            }

            if (chemical != null) {
                stack = ChemicalUtil.getFullChemicalTank(ChemicalTankTier.CREATIVE, chemical);
            }
        }
        return stack;
    }
}
