package terrails.xnetgases.module.slurry;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import mcjty.lib.varia.LevelTools;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.setup.Config;
import mekanism.api.Action;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import terrails.xnetgases.helper.ChemicalChannelSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static terrails.xnetgases.Constants.*;

public class SlurryChannelSettings extends ChemicalChannelSettings {

    public enum ChannelMode {
        PRIORITY,
        DISTRIBUTE
    }

    private SlurryChannelSettings.ChannelMode channelMode = SlurryChannelSettings.ChannelMode.DISTRIBUTE;
    private int delay;
    private int roundRobinOffset;

    private List<Pair<SidedConsumer, SlurryConnectorSettings>> slurryExtractors;
    private List<Pair<SidedConsumer, SlurryConnectorSettings>> slurryConsumers;

    public SlurryChannelSettings() {
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
        channelMode = SlurryUtils.getChannelModeFrom(data.get("mode").getAsString());
    }

    @Override
    public void readFromNBT(CompoundNBT nbt) {
        channelMode = SlurryChannelSettings.ChannelMode.values()[nbt.getByte("mode")];
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
            for (Pair<SidedConsumer, SlurryConnectorSettings> entry : slurryExtractors) {
                SidedConsumer consumer = entry.getFirst();
                SlurryConnectorSettings settings = entry.getSecond();
                if (d % settings.getSpeed() != 0) {
                    continue;
                }

                BlockPos extractorPos = context.findConsumerPosition(consumer.getConsumerId());
                if (extractorPos != null) {
                    BlockPos pos = extractorPos.relative(consumer.getSide());
                    if (!LevelTools.isLoaded(world, pos)) {
                        continue;
                    }

                    TileEntity te = world.getBlockEntity(pos);
                    Optional<ISlurryHandler> optional = SlurryUtils.getSlurryHandlerFor(te, settings.getFacing());
                    if (optional.isPresent()) {
                        ISlurryHandler handler = optional.get();

                        if (checkRedstone(world, settings, extractorPos)) {
                            return;
                        }
                        if (!context.matchColor(settings.getColorsMask())) {
                            return;
                        }

                        SlurryStack extractMatcher = settings.getMatcher();

                        long toExtract = settings.getRate();

                        Integer count = settings.getMinmax();
                        if (count != null) {
                            long amount = SlurryUtils.getSlurryCount(handler, settings.getFacing(), extractMatcher);
                            long canExtract = amount - count;
                            if (canExtract <= 0) {
                                continue;
                            }
                            toExtract = Math.min(toExtract, canExtract);
                        }

                        if (channelMode == ChannelMode.PRIORITY) {

                            // Skip current extractor if there is one with the same slurry but higher priority.
                            if (slurryExtractors.stream().anyMatch(_entry -> {
                                SidedConsumer _consumer = _entry.getFirst();
                                SlurryConnectorSettings _settings = _entry.getSecond();

                                if (_settings.getPriority() <= settings.getPriority()) {
                                    return false;
                                }

                                BlockPos _extractorPos = context.findConsumerPosition(_consumer.getConsumerId());
                                if (_extractorPos == null) {
                                    return false;
                                }

                                BlockPos _pos = _extractorPos.relative(_consumer.getSide());
                                if (!LevelTools.isLoaded(world, _pos)) {
                                    return false;
                                }

                                Optional<ISlurryHandler> _optional = SlurryUtils.getSlurryHandlerFor(world.getBlockEntity(_pos), _settings.getFacing());
                                if (_optional.isPresent()) {
                                    ISlurryHandler _handler = _optional.get();

                                    List<Slurry> handlerSlurries = SlurryUtils.getSlurryInTank(handler, consumer.getSide());
                                    List<Slurry> _handlerSlurries = SlurryUtils.getSlurryInTank(_handler, _consumer.getSide());

                                    if (Collections.disjoint(handlerSlurries, _handlerSlurries)) {
                                        return false;
                                    }

                                    SlurryStack matcher = settings.getMatcher();
                                    SlurryStack _matcher = _settings.getMatcher();

                                    return (matcher == null || handlerSlurries.contains(matcher.getType())) && (_matcher == null || _handlerSlurries.contains(_matcher.getType()));
                                }
                                return false;
                            })) {
                                continue;
                            }
                        }

                        List<Pair<SidedConsumer, SlurryConnectorSettings>> inserted = new ArrayList<>();
                        long remaining;
                        do {
                            SlurryStack stack = SlurryUtils.extractSlurry(handler, toExtract, settings.getFacing(), Action.SIMULATE);
                            if (stack.isEmpty() || (extractMatcher != null && !extractMatcher.isTypeEqual(stack)))
                                continue extractorsLoop;
                            toExtract = stack.getAmount();
                            inserted.clear();
                            remaining = insertSlurrySimulate(inserted, context, stack);
                            toExtract -= remaining;
                            if (inserted.isEmpty() || toExtract <= 0) continue extractorsLoop;
                        } while (remaining > 0);

                        if (context.checkAndConsumeRF(Config.controllerOperationRFT.get())) {
                            SlurryStack stack = SlurryUtils.extractSlurry(handler, toExtract, settings.getFacing(), Action.EXECUTE);
                            if (stack.isEmpty()) {
                                throw new NullPointerException(handler.getClass().getName() + " misbehaved! handler.extractSlurry(" + toExtract + ", Action.SIMULATE) returned null, even though handler.extractSlurry(" + toExtract + ", Action.EXECUTE) did not");
                            }
                            insertSlurryReal(context, inserted, stack);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void cleanCache() {
        this.slurryExtractors = null;
        this.slurryConsumers = null;
    }

    private long insertSlurrySimulate(@Nonnull List<Pair<SidedConsumer, SlurryConnectorSettings>> inserted, @Nonnull IControllerContext context, @Nonnull SlurryStack stack) {
        World world = context.getControllerWorld();
        long amount = stack.getAmount();
        for (int j = 0; j < slurryConsumers.size(); j++) {
            int i = (j + roundRobinOffset) % slurryConsumers.size();
            Pair<SidedConsumer, SlurryConnectorSettings> entry = slurryConsumers.get(i);
            SidedConsumer consumer = entry.getFirst();
            SlurryConnectorSettings settings = entry.getSecond();

            if (settings.getMatcher() == null || settings.getMatcher().isTypeEqual(stack)) {
                BlockPos consumerPos = context.findConsumerPosition(consumer.getConsumerId());
                if (consumerPos != null) {
                    if (!LevelTools.isLoaded(world, consumerPos)) {
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

                    Optional<ISlurryHandler> optional = SlurryUtils.getSlurryHandlerFor(te, settings.getFacing());
                    if (optional.isPresent()) {
                        ISlurryHandler handler = optional.get();

                        long toInsert = Math.min(settings.getRate(), amount);

                        Integer count = settings.getMinmax();
                        if (count != null) {
                            long a = SlurryUtils.getSlurryCount(handler, settings.getFacing(), settings.getMatcher());
                            long canInsert = count - a;
                            if (canInsert <= 0) {
                                continue;
                            }
                            toInsert = Math.min(toInsert, canInsert);
                        }

                        if (channelMode == ChannelMode.PRIORITY) {

                            // Skip current consumer if there is one that accepts the same gas but has higher priority
                            if (slurryConsumers.stream().anyMatch(_entry -> {
                                SidedConsumer _consumer = _entry.getFirst();
                                SlurryConnectorSettings _settings = _entry.getSecond();

                                if (_settings.getPriority() <= settings.getPriority()) {
                                    return false;
                                }

                                BlockPos _extractorPos = context.findConsumerPosition(_consumer.getConsumerId());
                                if (_extractorPos == null) {
                                    return false;
                                }

                                BlockPos _pos = _extractorPos.relative(_consumer.getSide());
                                if (!LevelTools.isLoaded(world, _pos)) {
                                    return false;
                                }

                                Optional<ISlurryHandler> _optional = SlurryUtils.getSlurryHandlerFor(world.getBlockEntity(_pos), _settings.getFacing());
                                if (_optional.isPresent()) {
                                    ISlurryHandler _handler = _optional.get();

                                    List<Slurry> handlerSlurries = SlurryUtils.getSlurryInTank(handler, consumer.getSide());
                                    List<Slurry> _handlerSlurries = SlurryUtils.getSlurryInTank(_handler, _consumer.getSide());

                                    if (Collections.disjoint(handlerSlurries, _handlerSlurries)) {
                                        return false;
                                    }

                                    SlurryStack matcher = settings.getMatcher();
                                    SlurryStack _matcher = _settings.getMatcher();

                                    return (matcher == null || handlerSlurries.contains(matcher.getType())) && (_matcher == null || _handlerSlurries.contains(_matcher.getType()));
                                }
                                return false;
                            })) {
                                continue;
                            }
                        }

                        SlurryStack copy = stack.copy();
                        copy.setAmount(toInsert);

                        SlurryStack remaining = SlurryUtils.insertSlurry(handler, copy, settings.getFacing(), Action.SIMULATE);
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

    private void insertSlurryReal(@Nonnull IControllerContext context, @Nonnull List<Pair<SidedConsumer, SlurryConnectorSettings>> inserted, @Nonnull SlurryStack stack) {
        long amount = stack.getAmount();
        for (Pair<SidedConsumer, SlurryConnectorSettings> pair : inserted) {

            SlurryConnectorSettings settings = pair.getSecond();
            BlockPos consumerPosition = context.findConsumerPosition(pair.getFirst().getConsumerId());

            assert consumerPosition != null;
            BlockPos pos = consumerPosition.relative(pair.getFirst().getSide());
            TileEntity te = context.getControllerWorld().getBlockEntity(pos);

            Optional<ISlurryHandler> optional = SlurryUtils.getSlurryHandlerFor(te, settings.getFacing());
            if (optional.isPresent()) {
                ISlurryHandler handler = optional.get();

                long toInsert = Math.min(settings.getRate(), amount);

                Integer count = settings.getMinmax();
                if (count != null) {
                    long a = SlurryUtils.getSlurryCount(handler, settings.getFacing(), settings.getMatcher());
                    long caninsert = count - a;
                    if (caninsert <= 0) {
                        continue;
                    }
                    toInsert = Math.min(toInsert, caninsert);
                }

                SlurryStack copy = stack.copy();
                copy.setAmount(toInsert);

                SlurryStack remaining = SlurryUtils.insertSlurry(handler, copy, settings.getFacing(), Action.EXECUTE);
                if (remaining.isEmpty() || (!remaining.isEmpty() && copy.getAmount() != remaining.getAmount())) {
                    roundRobinOffset = (roundRobinOffset + 1) % slurryConsumers.size();
                    amount -= (copy.getAmount() - remaining.getAmount());
                    if (amount <= 0) {
                        return;
                    }
                }
            }
        }
    }

    private void updateCache(int channel, IControllerContext context) {
        if (this.slurryExtractors == null) {
            this.slurryExtractors = new ArrayList<>();
            this.slurryConsumers = new ArrayList<>();

            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            Iterator<Map.Entry<SidedConsumer, IConnectorSettings>> iterator = connectors.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<SidedConsumer, IConnectorSettings> entry = iterator.next();
                SidedConsumer consumer = entry.getKey();
                SlurryConnectorSettings settings = (SlurryConnectorSettings) entry.getValue();
                if (settings.getSlurryMode() == SlurryConnectorSettings.SlurryMode.EXT) {
                    this.slurryExtractors.add(Pair.of(consumer, settings));
                } else {
                    this.slurryConsumers.add(Pair.of(consumer, settings));
                }
            }

            connectors = context.getRoutedConnectors(channel);
            iterator = connectors.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<SidedConsumer, IConnectorSettings> entry = iterator.next();
                SidedConsumer consumer = entry.getKey();
                SlurryConnectorSettings settings = (SlurryConnectorSettings) entry.getValue();
                if (settings.getSlurryMode() == SlurryConnectorSettings.SlurryMode.INS) {
                    this.slurryConsumers.add(Pair.of(consumer, settings));
                }
            }

            this.slurryConsumers.sort((o1, o2) -> (o2.getSecond()).getPriority().compareTo((o1.getSecond()).getPriority()));
        }
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(XNET_GUI_ELEMENTS, 0, 90, 11, 10);
    }

    @Override
    public void createGui(IEditorGui gui) {
        gui.nl().choices(TAG_MODE, "Slurry distribution mode", this.channelMode, SlurryChannelSettings.ChannelMode.values());
    }

    @Override
    public void update(Map<String, Object> data) {
        this.channelMode = SlurryChannelSettings.ChannelMode.valueOf(((String) data.get(TAG_MODE)).toUpperCase());
    }
}