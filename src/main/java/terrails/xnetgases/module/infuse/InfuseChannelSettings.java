package terrails.xnetgases.module.infuse;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import mcjty.lib.varia.WorldTools;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.setup.Config;
import mekanism.api.Action;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import terrails.xnetgases.helper.ChemicalChannelSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static terrails.xnetgases.Constants.*;

public class InfuseChannelSettings extends ChemicalChannelSettings {

    public enum ChannelMode {
        PRIORITY,
        DISTRIBUTE
    }

    private ChannelMode channelMode = ChannelMode.DISTRIBUTE;
    private int delay;
    private int roundRobinOffset;

    private List<Pair<SidedConsumer, InfuseConnectorSettings>> infuseExtractors;
    private List<Pair<SidedConsumer, InfuseConnectorSettings>> infuseConsumers;

    public InfuseChannelSettings() {
        this.delay = 0;
        this.roundRobinOffset = 0;
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        object.add("mode", new JsonPrimitive(channelMode.name()));
        return object;
    }

    @Override
    public void readFromJson(JsonObject data) {
        channelMode = InfuseUtils.getChannelModeFrom(data.get("mode").getAsString());
    }

    @Override
    public void readFromNBT(CompoundNBT nbt) {
        channelMode = InfuseChannelSettings.ChannelMode.values()[nbt.getByte("mode")];
        this.delay = nbt.getInt("delay");
        this.roundRobinOffset = nbt.getInt("offset");
    }

    @Override
    public void writeToNBT(CompoundNBT nbt) {
        nbt.putByte("mode", (byte) channelMode.ordinal());
        nbt.putInt("delay", this.delay);
        nbt.putInt("offset", this.roundRobinOffset);
    }

    @Override
    public void tick(int channel, IControllerContext context) {
        --this.delay;
        if (this.delay <= 0) {
            this.delay = 200 * 6;
        }

        if (this.delay % 10 == 0) {
            int d = this.delay / 10;
            updateCache(channel, context);

            World world = context.getControllerWorld();
            extractorsLoop:
            for (Pair<SidedConsumer, InfuseConnectorSettings> entry : infuseExtractors) {
                SidedConsumer consumer = entry.getFirst();
                InfuseConnectorSettings settings = entry.getSecond();
                if (d % settings.getSpeed() != 0) {
                    continue;
                }

                BlockPos extractorPos = context.findConsumerPosition(consumer.getConsumerId());
                if (extractorPos != null) {
                    BlockPos pos = extractorPos.relative(consumer.getSide());
                    if (!WorldTools.isLoaded(world, pos)) {
                        continue;
                    }

                    TileEntity te = world.getBlockEntity(pos);
                    Optional<IInfusionHandler> optional = InfuseUtils.getInfuseHandlerFor(te, settings.getFacing());
                    if (optional.isPresent()) {
                        IInfusionHandler handler = optional.get();

                        if (checkRedstone(world, settings, extractorPos)) {
                            return;
                        }
                        if (!context.matchColor(settings.getColorsMask())) {
                            return;
                        }

                        InfusionStack extractMatcher = settings.getMatcher();

                        long toExtract = settings.getRate();

                        Integer count = settings.getMinmax();
                        if (count != null) {
                            long amount = InfuseUtils.getInfuseCount(handler, settings.getFacing(), extractMatcher);
                            long canExtract = amount - count;
                            if (canExtract <= 0) {
                                continue;
                            }
                            toExtract = Math.min(toExtract, canExtract);
                        }

                        if (channelMode == InfuseChannelSettings.ChannelMode.PRIORITY) {

                            // Skip current extractor if there is one with the same infuse but has higher priority.
                            if (infuseExtractors.stream().anyMatch(_entry -> {
                                SidedConsumer _consumer = _entry.getFirst();
                                InfuseConnectorSettings _settings = _entry.getSecond();

                                if (_settings.getPriority() <= settings.getPriority()) {
                                    return false;
                                }

                                BlockPos _extractorPos = context.findConsumerPosition(_consumer.getConsumerId());
                                if (_extractorPos == null) {
                                    return false;
                                }

                                BlockPos _pos = _extractorPos.relative(_consumer.getSide());
                                if (!WorldTools.isLoaded(world, _pos)) {
                                    return false;
                                }

                                Optional<IInfusionHandler> _optional = InfuseUtils.getInfuseHandlerFor(world.getBlockEntity(_pos), _settings.getFacing());
                                if (_optional.isPresent()) {
                                    IInfusionHandler _handler = _optional.get();

                                    List<InfuseType> handlerInfuses = InfuseUtils.getInfuseInTank(handler, consumer.getSide());
                                    List<InfuseType> _handlerInfuses = InfuseUtils.getInfuseInTank(_handler, _consumer.getSide());

                                    if (Collections.disjoint(handlerInfuses, _handlerInfuses)) {
                                        return false;
                                    }

                                    InfusionStack matcher = settings.getMatcher();
                                    InfusionStack _matcher = _settings.getMatcher();

                                    return (matcher == null || handlerInfuses.contains(matcher.getType())) && (_matcher == null || _handlerInfuses.contains(_matcher.getType()));
                                }
                                return false;
                            })) {
                                continue;
                            }
                        }

                        List<Pair<SidedConsumer, InfuseConnectorSettings>> inserted = new ArrayList<>();
                        long remaining;
                        do {
                            InfusionStack stack = InfuseUtils.extractInfuse(handler, toExtract, settings.getFacing(), Action.SIMULATE);
                            if (stack.isEmpty() || (extractMatcher != null && !extractMatcher.equals(stack)))
                                continue extractorsLoop;
                            toExtract = stack.getAmount();
                            inserted.clear();
                            remaining = insertInfuseSimulate(inserted, context, stack);
                            toExtract -= remaining;
                            if (inserted.isEmpty() || toExtract <= 0) continue extractorsLoop;
                        } while (remaining > 0);

                        if (context.checkAndConsumeRF(Config.controllerOperationRFT.get())) {
                            InfusionStack stack = InfuseUtils.extractInfuse(handler, toExtract, settings.getFacing(), Action.EXECUTE);
                            if (stack.isEmpty()) {
                                throw new NullPointerException(handler.getClass().getName() + " misbehaved! handler.extractInfuse(" + toExtract + ", Action.SIMULATE) returned null, even though handler.extractInfuse(" + toExtract + ", Action.EXECUTE) did not");
                            }
                            insertInfuseReal(context, inserted, stack);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void cleanCache() {
        this.infuseExtractors = null;
        this.infuseConsumers = null;
    }

    private long insertInfuseSimulate(@Nonnull List<Pair<SidedConsumer, InfuseConnectorSettings>> inserted, @Nonnull IControllerContext context, @Nonnull InfusionStack stack) {
        World world = context.getControllerWorld();
        long amount = stack.getAmount();
        for (int j = 0; j < infuseConsumers.size(); j++) {
            int i = (j + roundRobinOffset) % infuseConsumers.size();
            Pair<SidedConsumer, InfuseConnectorSettings> entry = infuseConsumers.get(i);
            SidedConsumer consumer = entry.getFirst();
            InfuseConnectorSettings settings = entry.getSecond();

            if (settings.getMatcher() == null || settings.getMatcher().equals(stack)) {
                BlockPos consumerPos = context.findConsumerPosition(consumer.getConsumerId());
                if (consumerPos != null) {
                    if (!WorldTools.isLoaded(world, consumerPos)) {
                        continue;
                    }
                    if (checkRedstone(world, settings, consumerPos)) {
                        continue;
                    }
                    if (!context.matchColor(settings.getColorsMask())) {
                        continue;
                    }

                    BlockPos pos = consumerPos.relative(consumer.getSide());
                    TileEntity te = world.getBlockEntity(pos);

                    Optional<IInfusionHandler> optional = InfuseUtils.getInfuseHandlerFor(te, settings.getFacing());
                    if (optional.isPresent()) {
                        IInfusionHandler handler = optional.get();

                        long toInsert = Math.min(settings.getRate(), amount);

                        Integer count = settings.getMinmax();
                        if (count != null) {
                            long a = InfuseUtils.getInfuseCount(handler, settings.getFacing(), settings.getMatcher());
                            long canInsert = count - a;
                            if (canInsert <= 0) {
                                continue;
                            }
                            toInsert = Math.min(toInsert, canInsert);
                        }

                        if (channelMode == InfuseChannelSettings.ChannelMode.PRIORITY) {

                            // Skip current consumer if there is one that accepts the same infuse but has higher priority.
                            if (infuseConsumers.stream().anyMatch(_entry -> {
                                SidedConsumer _consumer = _entry.getFirst();
                                InfuseConnectorSettings _settings = _entry.getSecond();

                                if (_settings.getPriority() <= settings.getPriority()) {
                                    return false;
                                }

                                BlockPos _extractorPos = context.findConsumerPosition(_consumer.getConsumerId());
                                if (_extractorPos == null) {
                                    return false;
                                }

                                BlockPos _pos = _extractorPos.relative(_consumer.getSide());
                                if (!WorldTools.isLoaded(world, _pos)) {
                                    return false;
                                }

                                Optional<IInfusionHandler> _optional = InfuseUtils.getInfuseHandlerFor(world.getBlockEntity(_pos), _settings.getFacing());
                                if (_optional.isPresent()) {
                                    IInfusionHandler _handler = _optional.get();

                                    List<InfuseType> handlerInfuses = InfuseUtils.getInfuseInTank(handler, consumer.getSide());
                                    List<InfuseType> _handlerInfuses = InfuseUtils.getInfuseInTank(_handler, _consumer.getSide());

                                    if (Collections.disjoint(handlerInfuses, _handlerInfuses)) {
                                        return false;
                                    }

                                    InfusionStack matcher = settings.getMatcher();
                                    InfusionStack _matcher = _settings.getMatcher();

                                    return (matcher == null || handlerInfuses.contains(matcher.getType())) && (_matcher == null || _handlerInfuses.contains(_matcher.getType()));
                                }
                                return false;
                            })) {
                                continue;
                            }
                        }

                        InfusionStack copy = stack.copy();
                        copy.setAmount(toInsert);

                        InfusionStack remaining = InfuseUtils.insertInfuse(handler, copy, settings.getFacing(), Action.SIMULATE);
                        if (remaining.isEmpty() || (!remaining.isEmpty() && copy.getAmount() != remaining.getAmount())) {
                            inserted.add(entry);
                            amount -= (copy.getAmount() - remaining.getAmount());
                            if (amount <= 0) {
                                return 0;
                            }
                        }
                    }
                }
            }
        }
        return amount;
    }

    private void insertInfuseReal(@Nonnull IControllerContext context, @Nonnull List<Pair<SidedConsumer, InfuseConnectorSettings>> inserted, @Nonnull InfusionStack stack) {
        long amount = stack.getAmount();
        for (Pair<SidedConsumer, InfuseConnectorSettings> pair : inserted) {

            InfuseConnectorSettings settings = pair.getSecond();
            BlockPos consumerPosition = context.findConsumerPosition(pair.getFirst().getConsumerId());

            assert consumerPosition != null;
            BlockPos pos = consumerPosition.relative(pair.getFirst().getSide());
            TileEntity te = context.getControllerWorld().getBlockEntity(pos);

            Optional<IInfusionHandler> optional = InfuseUtils.getInfuseHandlerFor(te, settings.getFacing());
            if (optional.isPresent()) {
                IInfusionHandler handler = optional.get();

                long toInsert = Math.min(settings.getRate(), amount);

                Integer count = settings.getMinmax();
                if (count != null) {
                    long a = InfuseUtils.getInfuseCount(handler, settings.getFacing(), settings.getMatcher());
                    long caninsert = count - a;
                    if (caninsert <= 0) {
                        continue;
                    }
                    toInsert = Math.min(toInsert, caninsert);
                }

                InfusionStack copy = stack.copy();
                copy.setAmount(toInsert);

                InfusionStack remaining = InfuseUtils.insertInfuse(handler, copy, settings.getFacing(), Action.EXECUTE);
                if (remaining.isEmpty() || (!remaining.isEmpty() && copy.getAmount() != remaining.getAmount())) {
                    roundRobinOffset = (roundRobinOffset + 1) % infuseConsumers.size();
                    amount -= (copy.getAmount() - remaining.getAmount());
                    if (amount <= 0) {
                        return;
                    }
                }
            }
        }
    }

    private void updateCache(int channel, IControllerContext context) {
        if (this.infuseExtractors == null) {
            this.infuseExtractors = new ArrayList<>();
            this.infuseConsumers = new ArrayList<>();

            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            Iterator<Map.Entry<SidedConsumer, IConnectorSettings>> iterator = connectors.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<SidedConsumer, IConnectorSettings> entry = iterator.next();
                SidedConsumer consumer = entry.getKey();
                InfuseConnectorSettings settings = (InfuseConnectorSettings) entry.getValue();
                if (settings.getInfuseMode() == InfuseConnectorSettings.InfuseMode.EXT) {
                    this.infuseExtractors.add(Pair.of(consumer, settings));
                } else {
                    this.infuseConsumers.add(Pair.of(entry.getKey(), settings));
                }
            }

            connectors = context.getRoutedConnectors(channel);
            iterator = connectors.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<SidedConsumer, IConnectorSettings> entry = iterator.next();
                SidedConsumer consumer = entry.getKey();
                InfuseConnectorSettings settings = (InfuseConnectorSettings) entry.getValue();
                if (settings.getInfuseMode() == InfuseConnectorSettings.InfuseMode.INS) {
                    this.infuseConsumers.add(Pair.of(consumer, settings));
                }
            }

            this.infuseConsumers.sort((o1, o2) -> (o2.getSecond()).getPriority().compareTo((o1.getSecond()).getPriority()));
        }
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(XNET_GUI_ELEMENTS, 0, 90, 11, 10);
    }

    @Override
    public void createGui(IEditorGui gui) {
        gui.nl().choices(TAG_MODE, "Infuse distribution mode", this.channelMode, InfuseChannelSettings.ChannelMode.values());
    }

    @Override
    public void update(Map<String, Object> data) {
        this.channelMode = InfuseChannelSettings.ChannelMode.valueOf(((String) data.get(TAG_MODE)).toUpperCase());
    }

}
