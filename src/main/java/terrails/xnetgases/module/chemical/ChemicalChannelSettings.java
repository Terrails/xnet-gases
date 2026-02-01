package terrails.xnetgases.module.chemical;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.lib.varia.LevelTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.DefaultChannelSettings;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.apiimpl.ConnectedEntity;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.setup.Config;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.common.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import terrails.xnetgases.module.chemical.enums.ChannelMode;
import terrails.xnetgases.module.chemical.enums.ConnectorMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import java.util.*;

import static mcjty.xnet.apiimpl.Constants.TAG_DELAY;
import static mcjty.xnet.apiimpl.Constants.TAG_MODE;
import static terrails.xnetgases.Constants.TAG_DISTRIBUTE_OFFSET;
import static terrails.xnetgases.Constants.XNET_GUI_ELEMENTS;

public class ChemicalChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final MapCodec<ChemicalChannelSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ChannelMode.CODEC.fieldOf("mode").forGetter(ChemicalChannelSettings::getChannelMode)
    ).apply(instance, ChemicalChannelSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChemicalChannelSettings> STREAM_CODEC = StreamCodec.composite(
            ChannelMode.STREAM_CODEC, ChemicalChannelSettings::getChannelMode,
            ChemicalChannelSettings::new
    );

    private ChannelMode channelMode = ChannelMode.DISTRIBUTE;
    private int delay = 0;
    private int roundRobinOffset = 0;

    private List<ConnectedEntity<ChemicalConnectorSettings>> extractors;
    private List<ConnectedEntity<ChemicalConnectorSettings>> consumers;

    public ChemicalChannelSettings() {}

    public ChemicalChannelSettings(ChannelMode channelMode) {
        this.channelMode = channelMode;
    }

    public ChannelMode getChannelMode() {
        return channelMode;
    }

    @Override
    public IChannelType getType() {
        return ChemicalChannelType.TYPE;
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        tag.putInt(TAG_DELAY, delay);
        tag.putInt(TAG_DISTRIBUTE_OFFSET, roundRobinOffset);
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        delay = tag.getInt(TAG_DELAY);
        roundRobinOffset = tag.getInt(TAG_DISTRIBUTE_OFFSET);
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        object.add(TAG_MODE, new JsonPrimitive(channelMode.name()));
        return object;
    }

    @Override
    public void readFromJson(JsonObject data) {
        channelMode = ChannelMode.byName(data.get(TAG_MODE).getAsString());
    }

    @Override
    public void tick(int channel, IControllerContext context) {
        --delay;
        if (delay <= 0) delay = 200 * 6;
        if (delay % 10 != 0) return;

        updateCache(channel, context);
        Level level = context.getControllerWorld();
        for (ConnectedEntity<ChemicalConnectorSettings> extractor : extractors) {
            ChemicalConnectorSettings settings = extractor.settings();
            BlockEntity connectorEntity = extractor.getConnectorEntity();

            if (delay % settings.getOperationSpeed() != 0) continue;
            if (!LevelTools.isLoaded(level, extractor.getBlockPos())) continue;
            if (!checkRedstone(settings, extractor.getConnectorEntity(), context)) continue;

            IChemicalHandler handler = level.getCapability(
                    Capabilities.CHEMICAL.block(),
                    extractor.getBlockPos(),
                    null,
                    extractor.getConnectedEntity(),
                    settings.getFacing()
            );
            if (handler == null) continue;

            tickChemicalHandler(context, settings, connectorEntity, handler);
        }
    }

    private void tickChemicalHandler(IControllerContext context, ChemicalConnectorSettings settings, BlockEntity connectorEntity, IChemicalHandler handler) {
        if (!context.checkAndConsumeRF(Config.controllerChannelRFT.get())) return;

        long amount = settings.getMatcher().amountInTank(handler, settings.getFacing());
        if (amount <= 0) return;
        long toExtract = settings.getTransferRate(connectorEntity);
        Integer count = settings.getMinMaxLimit();
        if (count != null) {
            long canExtract = amount - count;
            if (canExtract <= 0) return;
            toExtract = Math.min(toExtract, canExtract);
        }

        int tankCount = handler.getChemicalTanks();
        for (int tank = 0; tank < tankCount && toExtract > 0; tank++) {
            ChemicalStack simulated = handler.extractChemical(tank, toExtract, Action.SIMULATE);

            if (!settings.getMatcher().test(simulated)) {
                continue;
            }

            long availableInTank = simulated.getAmount();
            long remaining = insertChemical(context, simulated);
            long transferredAmount = availableInTank - remaining;

            if (transferredAmount > 0) {
                handler.extractChemical(tank, transferredAmount, Action.EXECUTE);
                toExtract -= transferredAmount;
            }
        }
    }

    private long insertChemical(IControllerContext context, ChemicalStack stack) {
        Level level = context.getControllerWorld();
        if (channelMode == ChannelMode.PRIORITY) roundRobinOffset = 0;

        long amount = stack.getAmount();
        int currentRoundRobinOffset = roundRobinOffset;
        for (int j = 0; j < consumers.size(); j++) {
            int i = (j + currentRoundRobinOffset) % consumers.size();
            ConnectedEntity<ChemicalConnectorSettings> consumer = consumers.get(i);
            ChemicalConnectorSettings settings = consumer.settings();
            BlockEntity connectorEntity = consumer.getConnectorEntity();

            if (!settings.getMatcher().test(stack)) continue;
            if (!LevelTools.isLoaded(level, consumer.getBlockPos())) continue;
            if (!checkRedstone(settings, consumer.getConnectorEntity(), context)) continue;

            IChemicalHandler handler = level.getCapability(
                    Capabilities.CHEMICAL.block(),
                    consumer.getBlockPos(),
                    null,
                    consumer.getConnectedEntity(),
                    settings.getFacing()
            );
            if (handler == null) continue;

            long toInsert = Math.min(settings.getTransferRate(connectorEntity), amount);

            Integer count = settings.getMinMaxLimit();
            if (count != null) {
                long tankAmount = settings.getMatcher().amountInTank(handler, settings.getFacing());
                long canInsert = count - tankAmount;
                if (canInsert <= 0) continue;
                toInsert = Math.min(toInsert, canInsert);
            }

            if (settings.isTransferRateRequired() && settings.getTransferRate(connectorEntity) > toInsert) {
                continue;
            }

            ChemicalStack copy = stack.copy();
            copy.setAmount(toInsert);

            ChemicalStack remaining = handler.insertChemical(copy, Action.EXECUTE);
            if (remaining.isEmpty() || copy.getAmount() != remaining.getAmount()) {
                roundRobinOffset = (roundRobinOffset + 1) % consumers.size();
                amount -= (copy.getAmount() - remaining.getAmount());
                if (amount <= 0) {
                    return 0;
                }
            }
        }
        return amount;
    }

    @Override
    public void cleanCache() {
        extractors = null;
        consumers = null;
    }

    private void updateCache(int channel, IControllerContext context) {
        if (extractors == null) {
            extractors = new ArrayList<>();
            consumers = new ArrayList<>();

            Level level = context.getControllerWorld();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            for (var entry : connectors.entrySet()) {
                ChemicalConnectorSettings settings = (ChemicalConnectorSettings) entry.getValue();
                ConnectedEntity<ChemicalConnectorSettings> connectedEntity = getConnectedEntityInfo(context, entry, level, settings);
                if (connectedEntity == null) continue;

                if (settings.getConnectorMode() == ConnectorMode.INS) consumers.add(connectedEntity);
                else extractors.add(connectedEntity);
            }

            connectors = context.getRoutedConnectors(channel);
            for (var entry : connectors.entrySet()) {
                ChemicalConnectorSettings settings = (ChemicalConnectorSettings) entry.getValue();
                if (settings.getConnectorMode() == ConnectorMode.EXT) continue;

                ConnectedEntity<ChemicalConnectorSettings> connectedEntity = getConnectedEntityInfo(context, entry, level, settings);
                if (connectedEntity == null) continue;

                consumers.add(connectedEntity);
            }

            // Sort by descending priority
            consumers.sort(Collections.reverseOrder(Comparator.comparing(o -> o.settings().getPriority())));
        }
    }

    private ConnectedEntity<ChemicalConnectorSettings> getConnectedEntityInfo(IControllerContext context, Map.Entry<SidedConsumer, IConnectorSettings> entry, @Nonnull Level level, @Nonnull ChemicalConnectorSettings settings) {
        BlockPos connectorPos = context.findConsumerPosition(entry.getKey().consumerId());
        if (connectorPos == null) {
            return null;
        }

        ConnectorTileEntity connectorTileEntity = (ConnectorTileEntity) level.getBlockEntity(connectorPos);
        if (connectorTileEntity == null) {
            return null;
        }

        BlockPos connectedBlockPos = connectorPos.relative(entry.getKey().side());
        BlockEntity connectedEntity = level.getBlockEntity(connectedBlockPos);
        if (connectedEntity == null) {
            return null;
        }

        return new ConnectedEntity<>(entry.getKey(), settings, connectorPos, connectedBlockPos, connectedEntity, connectorTileEntity);
    }

    @Override
    public int getColors() {
        return 0;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(XNET_GUI_ELEMENTS, 0, 90, 11, 10);
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public boolean isEnabled(String s) {
        return true;
    }

    @Override
    public void createGui(IEditorGui gui) {
        gui.nl().translatableChoices(TAG_MODE, channelMode, ChannelMode.values());
    }

    @Override
    public void update(Map<String, Object> map) {
        Object value = map.get(TAG_MODE);
        if (value instanceof Integer i) {
            channelMode = ChannelMode.values()[i];
        } else {
            channelMode = ChannelMode.DISTRIBUTE;
        }
    }
}
