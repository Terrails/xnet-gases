package terrails.xnetgases.module.pigment;

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
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import terrails.xnetgases.helper.ChemicalChannelSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static terrails.xnetgases.Constants.*;

public class PigmentChannelSettings extends ChemicalChannelSettings {

    public enum ChannelMode {
        PRIORITY,
        DISTRIBUTE
    }

    private ChannelMode channelMode = ChannelMode.DISTRIBUTE;
    private int delay;
    private int roundRobinOffset;

    private List<Pair<SidedConsumer, PigmentConnectorSettings>> pigmentExtractors;
    private List<Pair<SidedConsumer, PigmentConnectorSettings>> pigmentConsumers;

    public PigmentChannelSettings() {
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
        channelMode = PigmentUtils.getChannelModeFrom(data.get("mode").getAsString());
    }

    @Override
    public void readFromNBT(CompoundNBT nbt) {
        channelMode = PigmentChannelSettings.ChannelMode.values()[nbt.getByte("mode")];
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
            for (Pair<SidedConsumer, PigmentConnectorSettings> entry : pigmentExtractors) {
                SidedConsumer consumer = entry.getFirst();
                PigmentConnectorSettings settings = entry.getSecond();
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
                    Optional<IPigmentHandler> optional = PigmentUtils.getPigmentHandlerFor(te, settings.getFacing());
                    if (optional.isPresent()) {
                        IPigmentHandler handler = optional.get();

                        if (checkRedstone(world, settings, extractorPos)) {
                            return;
                        }
                        if (!context.matchColor(settings.getColorsMask())) {
                            return;
                        }

                        PigmentStack extractMatcher = settings.getMatcher();

                        long toExtract = settings.getRate();

                        Integer count = settings.getMinmax();
                        if (count != null) {
                            long amount = PigmentUtils.getPigmentCount(handler, settings.getFacing(), extractMatcher);
                            long canExtract = amount - count;
                            if (canExtract <= 0) {
                                continue;
                            }
                            toExtract = Math.min(toExtract, canExtract);
                        }

                        if (channelMode == PigmentChannelSettings.ChannelMode.PRIORITY) {

                            // Skip current extractor if there is one with the same pigment but has higher priority.
                            if (pigmentExtractors.stream().anyMatch(_entry -> {
                                SidedConsumer _consumer = _entry.getFirst();
                                PigmentConnectorSettings _settings = _entry.getSecond();

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

                                Optional<IPigmentHandler> _optional = PigmentUtils.getPigmentHandlerFor(world.getBlockEntity(_pos), _settings.getFacing());
                                if (_optional.isPresent()) {
                                    IPigmentHandler _handler = _optional.get();

                                    List<Pigment> handlerPigments = PigmentUtils.getPigmentInTank(handler, consumer.getSide());
                                    List<Pigment> _handlerPigments = PigmentUtils.getPigmentInTank(_handler, _consumer.getSide());

                                    if (Collections.disjoint(handlerPigments, _handlerPigments)) {
                                        return false;
                                    }

                                    PigmentStack matcher = settings.getMatcher();
                                    PigmentStack _matcher = _settings.getMatcher();

                                    return (matcher == null || handlerPigments.contains(matcher.getType())) && (_matcher == null || _handlerPigments.contains(_matcher.getType()));
                                }
                                return false;
                            })) {
                                continue;
                            }
                        }

                        List<Pair<SidedConsumer, PigmentConnectorSettings>> inserted = new ArrayList<>();
                        long remaining;
                        do {
                            PigmentStack stack = PigmentUtils.extractPigment(handler, toExtract, settings.getFacing(), Action.SIMULATE);
                            if (stack.isEmpty() || (extractMatcher != null && !extractMatcher.equals(stack)))
                                continue extractorsLoop;
                            toExtract = stack.getAmount();
                            inserted.clear();
                            remaining = insertPigmentSimulate(inserted, context, stack);
                            toExtract -= remaining;
                            if (inserted.isEmpty() || toExtract <= 0) continue extractorsLoop;
                        } while (remaining > 0);

                        if (context.checkAndConsumeRF(Config.controllerOperationRFT.get())) {
                            PigmentStack stack = PigmentUtils.extractPigment(handler, toExtract, settings.getFacing(), Action.EXECUTE);
                            if (stack.isEmpty()) {
                                throw new NullPointerException(handler.getClass().getName() + " misbehaved! handler.extractPigment(" + toExtract + ", Action.SIMULATE) returned null, even though handler.extractPigment(" + toExtract + ", Action.EXECUTE) did not");
                            }
                            insertPigmentReal(context, inserted, stack);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void cleanCache() {
        this.pigmentExtractors = null;
        this.pigmentConsumers = null;
    }

    private long insertPigmentSimulate(@Nonnull List<Pair<SidedConsumer, PigmentConnectorSettings>> inserted, @Nonnull IControllerContext context, @Nonnull PigmentStack stack) {
        World world = context.getControllerWorld();
        long amount = stack.getAmount();
        for (int j = 0; j < pigmentConsumers.size(); j++) {
            int i = (j + roundRobinOffset) % pigmentConsumers.size();
            Pair<SidedConsumer, PigmentConnectorSettings> entry = pigmentConsumers.get(i);
            SidedConsumer consumer = entry.getFirst();
            PigmentConnectorSettings settings = entry.getSecond();

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

                    Optional<IPigmentHandler> optional = PigmentUtils.getPigmentHandlerFor(te, settings.getFacing());
                    if (optional.isPresent()) {
                        IPigmentHandler handler = optional.get();

                        long toInsert = Math.min(settings.getRate(), amount);

                        Integer count = settings.getMinmax();
                        if (count != null) {
                            long a = PigmentUtils.getPigmentCount(handler, settings.getFacing(), settings.getMatcher());
                            long canInsert = count - a;
                            if (canInsert <= 0) {
                                continue;
                            }
                            toInsert = Math.min(toInsert, canInsert);
                        }

                        if (channelMode == PigmentChannelSettings.ChannelMode.PRIORITY) {

                            // Skip current consumer if there is one that accepts the same pigment but has higher priority.
                            if (pigmentConsumers.stream().anyMatch(_entry -> {
                                SidedConsumer _consumer = _entry.getFirst();
                                PigmentConnectorSettings _settings = _entry.getSecond();

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

                                Optional<IPigmentHandler> _optional = PigmentUtils.getPigmentHandlerFor(world.getBlockEntity(_pos), _settings.getFacing());
                                if (_optional.isPresent()) {
                                    IPigmentHandler _handler = _optional.get();

                                    List<Pigment> handlerPigments = PigmentUtils.getPigmentInTank(handler, consumer.getSide());
                                    List<Pigment> _handlerPigments = PigmentUtils.getPigmentInTank(_handler, _consumer.getSide());

                                    if (Collections.disjoint(handlerPigments, _handlerPigments)) {
                                        return false;
                                    }

                                    PigmentStack matcher = settings.getMatcher();
                                    PigmentStack _matcher = _settings.getMatcher();

                                    return (matcher == null || handlerPigments.contains(matcher.getType())) && (_matcher == null || _handlerPigments.contains(_matcher.getType()));
                                }
                                return false;
                            })) {
                                continue;
                            }
                        }

                        PigmentStack copy = stack.copy();
                        copy.setAmount(toInsert);

                        PigmentStack remaining = PigmentUtils.insertPigment(handler, copy, settings.getFacing(), Action.SIMULATE);
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

    private void insertPigmentReal(@Nonnull IControllerContext context, @Nonnull List<Pair<SidedConsumer, PigmentConnectorSettings>> inserted, @Nonnull PigmentStack stack) {
        long amount = stack.getAmount();
        for (Pair<SidedConsumer, PigmentConnectorSettings> pair : inserted) {

            PigmentConnectorSettings settings = pair.getSecond();
            BlockPos consumerPosition = context.findConsumerPosition(pair.getFirst().getConsumerId());

            assert consumerPosition != null;
            BlockPos pos = consumerPosition.relative(pair.getFirst().getSide());
            TileEntity te = context.getControllerWorld().getBlockEntity(pos);

            Optional<IPigmentHandler> optional = PigmentUtils.getPigmentHandlerFor(te, settings.getFacing());
            if (optional.isPresent()) {
                IPigmentHandler handler = optional.get();

                long toInsert = Math.min(settings.getRate(), amount);

                Integer count = settings.getMinmax();
                if (count != null) {
                    long a = PigmentUtils.getPigmentCount(handler, settings.getFacing(), settings.getMatcher());
                    long caninsert = count - a;
                    if (caninsert <= 0) {
                        continue;
                    }
                    toInsert = Math.min(toInsert, caninsert);
                }

                PigmentStack copy = stack.copy();
                copy.setAmount(toInsert);

                PigmentStack remaining = PigmentUtils.insertPigment(handler, copy, settings.getFacing(), Action.EXECUTE);
                if (remaining.isEmpty() || (!remaining.isEmpty() && copy.getAmount() != remaining.getAmount())) {
                    roundRobinOffset = (roundRobinOffset + 1) % pigmentConsumers.size();
                    amount -= (copy.getAmount() - remaining.getAmount());
                    if (amount <= 0) {
                        return;
                    }
                }
            }
        }
    }

    private void updateCache(int channel, IControllerContext context) {
        if (this.pigmentExtractors == null) {
            this.pigmentExtractors = new ArrayList<>();
            this.pigmentConsumers = new ArrayList<>();

            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            Iterator<Map.Entry<SidedConsumer, IConnectorSettings>> iterator = connectors.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<SidedConsumer, IConnectorSettings> entry = iterator.next();
                SidedConsumer consumer = entry.getKey();
                PigmentConnectorSettings settings = (PigmentConnectorSettings) entry.getValue();
                if (settings.getPigmentMode() == PigmentConnectorSettings.PigmentMode.EXT) {
                    this.pigmentExtractors.add(Pair.of(consumer, settings));
                } else {
                    this.pigmentConsumers.add(Pair.of(entry.getKey(), settings));
                }
            }

            connectors = context.getRoutedConnectors(channel);
            iterator = connectors.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<SidedConsumer, IConnectorSettings> entry = iterator.next();
                SidedConsumer consumer = entry.getKey();
                PigmentConnectorSettings settings = (PigmentConnectorSettings) entry.getValue();
                if (settings.getPigmentMode() == PigmentConnectorSettings.PigmentMode.INS) {
                    this.pigmentConsumers.add(Pair.of(consumer, settings));
                }
            }

            this.pigmentConsumers.sort((o1, o2) -> (o2.getSecond()).getPriority().compareTo((o1.getSecond()).getPriority()));
        }
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(XNET_GUI_ELEMENTS, 0, 90, 11, 10);
    }

    @Override
    public void createGui(IEditorGui gui) {
        gui.nl().choices(TAG_MODE, "Pigment distribution mode", this.channelMode, PigmentChannelSettings.ChannelMode.values());
    }

    @Override
    public void update(Map<String, Object> data) {
        this.channelMode = PigmentChannelSettings.ChannelMode.valueOf(((String) data.get(TAG_MODE)).toUpperCase());
    }

}
