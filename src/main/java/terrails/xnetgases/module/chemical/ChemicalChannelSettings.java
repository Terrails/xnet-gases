package terrails.xnetgases.module.chemical;

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
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import terrails.xnetgases.helper.BaseChannelSettings;
import terrails.xnetgases.helper.ModuleEnums;
import terrails.xnetgases.helper.ModuleEnums.*;
import terrails.xnetgases.module.chemical.utils.ChemicalHelper;

import java.util.*;

import static terrails.xnetgases.Constants.TAG_MODE;
import static terrails.xnetgases.Constants.XNET_GUI_ELEMENTS;

public class ChemicalChannelSettings extends BaseChannelSettings {

    public static final String TAG_DELAY = "delay";
    public static final String TAG_DISTRIBUTE_OFFSET = "distribute_offset";
    public static final String TAG_DISTRIBUTE_INCREMENT = "distribute_increment";

    private ChannelMode channelMode = ChannelMode.DISTRIBUTE;
    private int delay;
    private int roundRobinOffset;
    private int roundRobinIncrement;

    private List<Pair<SidedConsumer, ChemicalConnectorSettings>> extractors;
    private List<Pair<SidedConsumer, ChemicalConnectorSettings>> consumers;

    public ChemicalChannelSettings() {
        this.delay = 0;
        this.roundRobinOffset = 0;
        this.roundRobinIncrement = 0;
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        this.channelMode = ChannelMode.values()[tag.getByte(TAG_MODE)];
        this.delay = tag.getInt(TAG_DELAY);
        this.roundRobinOffset = tag.getInt(TAG_DISTRIBUTE_OFFSET);
        this.roundRobinIncrement = tag.getInt(TAG_DISTRIBUTE_INCREMENT);
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        tag.putByte(TAG_MODE, (byte) this.channelMode.ordinal());
        tag.putInt(TAG_DELAY, this.delay);
        tag.putInt(TAG_DISTRIBUTE_OFFSET, this.roundRobinOffset);
        tag.putInt(TAG_DISTRIBUTE_INCREMENT, this.roundRobinIncrement);
    }

    @Override
    public void readFromJson(JsonObject data) {
        this.channelMode = ChannelMode.byName(data.get(TAG_MODE).getAsString());
        this.delay = data.get(TAG_DELAY).getAsInt();
        this.roundRobinOffset = data.get(TAG_DISTRIBUTE_OFFSET).getAsInt();
        this.roundRobinIncrement = data.get(TAG_DISTRIBUTE_INCREMENT).getAsInt();
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        object.add(TAG_MODE, new JsonPrimitive(this.channelMode.name()));
        object.add(TAG_DELAY, new JsonPrimitive(this.delay));
        object.add(TAG_DISTRIBUTE_OFFSET, new JsonPrimitive(this.roundRobinOffset));
        object.add(TAG_DISTRIBUTE_INCREMENT, new JsonPrimitive(this.roundRobinIncrement));
        return object;
    }

    @Override
    public void tick(int channel, IControllerContext context) {
        --this.delay;
        if (this.delay <= 0) {
            this.delay = 200 * 6;
        }

        if (this.delay % 10 == 0) {
            updateCache(channel, context);

            for (Pair<SidedConsumer, ChemicalConnectorSettings> entry : this.extractors) {
                SidedConsumer consumer = entry.getFirst();
                ChemicalConnectorSettings settings = entry.getSecond();
                if (this.delay % settings.getOperationSpeed() != 0) {
                    continue;
                }

                Level level = context.getControllerWorld();
                BlockPos extractorPos = context.findConsumerPosition(consumer.consumerId());
                if (extractorPos != null) {
                    BlockPos pos = extractorPos.relative(consumer.side());
                    if (!LevelTools.isLoaded(level, pos)) {
                        continue;
                    }

                    BlockEntity be = level.getBlockEntity(pos);
                    ChemicalEnums.Type type = settings.getConnectorType();

                    Optional<IChemicalHandler<?, ?>> optional = ChemicalHelper.handler(be, settings.getFacing(), type);
                    if (optional.isPresent()) {
                        IChemicalHandler<?, ?> handler = optional.get();

                        if (checkRedstone(level, settings, extractorPos) || !context.matchColor(settings.getColorsMask()))
                            return;

                        ChemicalStack<?> filter = settings.getMatcher();
                        long toExtract = settings.getRate();

                        long amount = ChemicalHelper.amountInTank(handler, settings.getFacing(), filter, type);
                        // Just skip extractor if there is no chemical in tank
                        if (amount <= 0) continue;

                        Integer count = settings.getMinMaxLimit();
                        if (count != null) {
                            long canExtract = amount - count;
                            if (canExtract <= 0)  {
                                continue;
                            }
                            toExtract = Math.min(toExtract, canExtract);
                        }

                        List<Pair<SidedConsumer, ChemicalConnectorSettings>> inserted = new ArrayList<>();
                        ChemicalStack<?> extractStack = ChemicalHelper.extract(handler, toExtract, settings.getFacing(), Action.SIMULATE, settings.getConnectorType());
                        if (extractStack.isEmpty() || (filter != null && filter.getType() != extractStack.getType())) {
                            continue;
                        }

                        toExtract = extractStack.getAmount();
                        long remaining = insertSimulate(inserted, context, extractStack, settings.getConnectorType());
                        toExtract -= remaining;

                        if (inserted.isEmpty() || toExtract <= 0) {
                            continue;
                        }

//                        XNetGases.LOGGER.info("Extracting '{}/{}mB' from '{}'", toExtract, toExtract + remaining, pos);

                        this.roundRobinOffset = (this.roundRobinOffset + this.roundRobinIncrement) % this.consumers.size();
                        if (context.checkAndConsumeRF(Config.controllerOperationRFT.get())) {
                            ChemicalStack<?> stack = ChemicalHelper.extract(handler, toExtract, settings.getFacing(), Action.EXECUTE, settings.getConnectorType());
                            if (stack.isEmpty()) {
                                throw new NullPointerException(handler.getClass().getName() + " misbehaved! handler.extract(" + toExtract + ", Action.SIMULATE, ConnectorType." + settings.getConnectorType().name() + ") returned null even though handler.extract(" + toExtract + ", Action.SIMULATE, ConnectorType." + settings.getConnectorType().name() + ") did not");
                            }
                            insertExecute(context, inserted, stack);
                        }
                    }
                }
            }
        }
    }

    private long insertSimulate(List<Pair<SidedConsumer, ChemicalConnectorSettings>> inserted, IControllerContext context, ChemicalStack<?> stack, ChemicalEnums.Type type) {
        Level level = context.getControllerWorld();
        if (this.channelMode == ChannelMode.PRIORITY) {
            this.roundRobinOffset = 0;
        }
        this.roundRobinIncrement = 0;
        /*
            Regarding 'roundRobinIncrement'
            Let's say that a consumer is full / doesn't exist / doesn't accept chemical for some reason

            When round-robin / distribution is enabled and the loop below goes through a consumer
            that's in any of the above given states. If it's in any of those states 'j' is incremented.

            The next consumers accept the chemical and everything's fine. The inserted list has the valid consumers,
            the function eventually returns when 'amount' is 0 and the inserted list is given to the real insert function.
            This insert function increments 'roundRobinOffset' by one for each consumer.

            We then get back to this function. The last consumer that accepted the chemical will be iterated
            over again and chemicals will be added to it twice in a row instead of once per every round-robin circle.

            Behaviour without 'roundRobinIncrement':
            Simple example with only 3 consumers, [1],[3] are invalid, [0],[2] are valid.

            First run:
                Iteration 1: roundRobinOffset = 0; j = 0; -> consumers[0] is chosen
                    -> Added to inserted list
                Iteration 2: roundRobinOffset = 0; j = 1; -> consumers[1] is chosen
                    -> Skipped
                Iteration 3: roundRobinOffset = 0; j = 2; -> consumers[2] is chosen
                    -> Added to inserted list
                Iteration 4: roundRobinOffset = 0; j = 3; -> consumers[3] is chosen
                    -> Skipped
                Function ends and goes back to main function. 'roundRobinOffset' is
                incremented by 1 for each valid consumer (2 in this case).
            Second run:
                Iteration 1: roundRobinOffset = 2; j = 0; -> consumers[2] is chosen
                    -> This should've gone to consumers[0] as the round-robin ran through
                        all consumers already.

             'roundRobinIncrement' would be 2 at the end of first run and would be added to 'roundRobinOffset'
             which would in end result in (2 + 2) % 4 = 0.
         */
        long amount = stack.getAmount();
        for (int j = 0; j < this.consumers.size(); j++) {
            int i = (j + this.roundRobinOffset) % this.consumers.size();
            if (this.roundRobinOffset < 0) continue;
            Pair<SidedConsumer, ChemicalConnectorSettings> entry = this.consumers.get(i);
            SidedConsumer consumer = entry.getFirst();
            ChemicalConnectorSettings settings = entry.getSecond();

            if (settings.getConnectorType() != type) {
                this.roundRobinIncrement++;
                continue;
            }

            if (settings.getMatcher() == null || settings.getMatcher().getType() == stack.getType()) {
                BlockPos consumerPos = context.findConsumerPosition(consumer.consumerId());
                if (consumerPos != null) {
                    if (!LevelTools.isLoaded(level, consumerPos) || checkRedstone(level, settings, consumerPos) || !context.matchColor(settings.getColorsMask())) {
                        this.roundRobinIncrement++;
                        continue;
                    }

                    BlockPos pos = consumerPos.relative(consumer.side());
                    BlockEntity be = level.getBlockEntity(pos);

                    Optional<IChemicalHandler<?, ?>> optional = ChemicalHelper.handler(be, settings.getFacing(), settings.getConnectorType());
                    if (optional.isPresent()) {
                        IChemicalHandler<?, ?> handler = optional.get();

                        long toInsert = Math.min(settings.getRate(), amount);

                        Integer count = settings.getMinMaxLimit();
                        if (count != null) {
                            long a = ChemicalHelper.amountInTank(handler, settings.getFacing(), settings.getMatcher(), settings.getConnectorType());
                            long canInsert = count - a;
                            if (canInsert <= 0) {
                                continue;
                            }
                            toInsert = Math.min(toInsert, canInsert);
                        }

                        if (settings.isTransferRateRequired() && settings.getRate() > toInsert) {
                            continue;
                        }

                        ChemicalStack<?> copy = stack.copy();
                        copy.setAmount(toInsert);

                        ChemicalStack<?> remaining = ChemicalHelper.insert(handler, copy, settings.getFacing(), Action.SIMULATE, settings.getConnectorType());
                        if (remaining.isEmpty() || (!remaining.isEmpty() && copy.getAmount() != remaining.getAmount())) {
                            inserted.add(entry);
                            amount -= (copy.getAmount() - remaining.getAmount());
                            if (amount <= 0) {
                                return 0;
                            }
                        } else if (remaining.equals(copy)) {
                            this.roundRobinIncrement++;
                        }
                    } else {
                        this.roundRobinIncrement++;
                    }
                }
            }
        }
        return amount;
    }

    private void insertExecute(IControllerContext context, List<Pair<SidedConsumer, ChemicalConnectorSettings>> inserted, ChemicalStack<?> stack) {
        long amount = stack.getAmount();
        for (Pair<SidedConsumer, ChemicalConnectorSettings> pair : inserted) {
            SidedConsumer consumer = pair.getFirst();
            ChemicalConnectorSettings settings = pair.getSecond();

            BlockPos consumerPos = context.findConsumerPosition(consumer.consumerId());
            if (consumerPos == null) continue;

            BlockPos pos = consumerPos.relative(consumer.side());
            BlockEntity be = context.getControllerWorld().getBlockEntity(pos);

            Optional<IChemicalHandler<?, ?>> optional = ChemicalHelper.handler(be, settings.getFacing(), settings.getConnectorType());
            if (optional.isPresent()) {
                IChemicalHandler<?, ?> handler = optional.get();

                long toInsert = Math.min(settings.getRate(), amount);

                Integer count = settings.getMinMaxLimit();
                if (count != null) {
                    long a = ChemicalHelper.amountInTank(handler, settings.getFacing(), settings.getMatcher(), settings.getConnectorType());
                    long canInsert = count - a;
                    if (canInsert <= 0) continue;
                    toInsert = Math.min(toInsert, canInsert);
                }

                ChemicalStack<?> copy = stack.copy();
                copy.setAmount(toInsert);

                ChemicalStack<?> remaining = ChemicalHelper.insert(handler, copy, settings.getFacing(), Action.EXECUTE, settings.getConnectorType());

                if (remaining.isEmpty() || (!remaining.isEmpty() && copy.getAmount() != remaining.getAmount())) {
                    this.roundRobinOffset = (this.roundRobinOffset + 1) % this.consumers.size();
                    amount -= (copy.getAmount() - remaining.getAmount());
                    if (amount <= 0) return;
                }
            }
        }
    }

    @Override
    public void cleanCache() {
        this.extractors = null;
        this.consumers = null;
    }

    private void updateCache(int channel, IControllerContext context) {
        if (this.extractors == null) {
            this.extractors = new ArrayList<>();
            this.consumers = new ArrayList<>();

            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            Iterator<Map.Entry<SidedConsumer, IConnectorSettings>> iterator = connectors.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<SidedConsumer, IConnectorSettings> entry = iterator.next();
                SidedConsumer consumer = entry.getKey();

                ChemicalConnectorSettings settings = (ChemicalConnectorSettings) entry.getValue();
                if (settings.getConnectorMode() == ModuleEnums.ConnectorMode.EXT) {
                    this.extractors.add(Pair.of(consumer, settings));
                } else {
                    this.consumers.add(Pair.of(entry.getKey(), settings));
                }
            }

            connectors = context.getRoutedConnectors(channel);
            iterator = connectors.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<SidedConsumer, IConnectorSettings> entry = iterator.next();
                SidedConsumer consumer = entry.getKey();
                ChemicalConnectorSettings settings = (ChemicalConnectorSettings) entry.getValue();
                if (settings.getConnectorMode() == ModuleEnums.ConnectorMode.INS) {
                    this.consumers.add(Pair.of(consumer, settings));
                }
            }

            this.consumers.sort((o1, o2) -> Integer.compare(o2.getSecond().getPriority(), o1.getSecond().getPriority()));
        }
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(XNET_GUI_ELEMENTS, 0, 90, 11, 10);
    }

    @Override
    public void createGui(IEditorGui gui) {
        gui.nl().choices(TAG_MODE, "Distribution mode", this.channelMode, ChannelMode.values());
    }

    @Override
    public void update(Map<String, Object> map) {
        this.channelMode = ChannelMode.valueOf(((String) map.get(TAG_MODE)).toUpperCase(Locale.ROOT));
    }
}
