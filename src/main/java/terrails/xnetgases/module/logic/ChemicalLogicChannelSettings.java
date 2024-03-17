package terrails.xnetgases.module.logic;

import com.google.gson.JsonObject;
import mcjty.lib.varia.LevelTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.DefaultChannelSettings;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.tuple.Pair;
import terrails.xnetgases.module.logic.ChemicalLogicEnums.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;

import static terrails.xnetgases.Constants.*;

public class ChemicalLogicChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    private int delay = 0;
    private int colors = 0;
    private List<Pair<SidedConsumer, ChemicalLogicConnectorSettings>> sensors = null;
    private List<Pair<SidedConsumer, ChemicalLogicConnectorSettings>> outputs = null;

    @Override
    public JsonObject writeToJson() {
        return new JsonObject();
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        delay = tag.getInt("delay");
        colors = tag.getInt("colors");
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        tag.putInt("delay", delay);
        tag.putInt("colors", colors);
    }

    @Override
    public void tick(int channel, IControllerContext context) {
        delay--;
        if (delay <= 0) {
            delay = 200 * 6;
        }
        if (delay % 5 != 0) {
            return;
        }

        int d = delay / 5;
        updateCache(channel, context);
        Level level = context.getControllerWorld();

        colors = 0;
        for (Pair<SidedConsumer, ChemicalLogicConnectorSettings> entry : sensors) {
            ChemicalLogicConnectorSettings settings = entry.getValue();
            if (d % settings.getSpeed() != 0) {
                // Use the color settings from this connector as we last remembered it
                colors |= settings.getColorMask();
                continue;
            }
            int sensorColors = 0;
            BlockPos connectorPos = context.findConsumerPosition(entry.getKey().consumerId());
            if (connectorPos != null) {
                Direction side = entry.getKey().side();
                BlockPos pos = connectorPos.relative(side);
                if (!LevelTools.isLoaded(level, pos)) {
                    // If it is not chunkloaded we just use the color settings as we last remembered it
                    colors |= settings.getColorMask();
                    continue;
                }

                boolean sense = !checkRedstone(level, settings, connectorPos);
                if (sense && !context.matchColor(settings.getColorsMask())) {
                    sense = false;
                }

                // If sense is false the sensor is disabled which means the colors from it will also be disabled
                if (sense) {
                    for (ChemicalSensor sensor : settings.getSensors()) {
                        if (sensor.test(level, pos, settings)) {
                            sensorColors |= 1 << sensor.getOutputColor().ordinal();
                        }
                    }
                }
            }
            settings.setColorMask(sensorColors);
            colors |= sensorColors;
        }

        for (Pair<SidedConsumer, ChemicalLogicConnectorSettings> entry : outputs) {
            ChemicalLogicConnectorSettings settings = entry.getValue();
            if (d % settings.getSpeed() != 0) {
                continue;
            }

            BlockPos connectorPos = context.findConsumerPosition(entry.getKey().consumerId());
            if (connectorPos != null) {
                Direction side = entry.getKey().side();
                if (!LevelTools.isLoaded(level, connectorPos)) {
                    continue;
                }

                BlockEntity te = level.getBlockEntity(connectorPos);
                if (te instanceof ConnectorTileEntity connectorTE) {
                    int powerOut;
                    if (checkRedstone(level, settings, connectorPos)) {
                        powerOut = 0;
                    } else if (!context.matchColor(settings.getColorsMask())) {
                        powerOut = 0;
                    } else {
                        powerOut = settings.getRedstoneOut() == null ? 0 : settings.getRedstoneOut();
                    }
                    connectorTE.setPowerOut(side, powerOut);
                }
            }
        }
    }

    private void updateCache(int channel, IControllerContext context) {
        if (sensors == null || outputs == null) {
            sensors = new ArrayList<>();
            outputs = new ArrayList<>();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                ChemicalLogicConnectorSettings con = (ChemicalLogicConnectorSettings) entry.getValue();
                if (con.getConnectorMode() == ConnectorMode.SENSOR) {
                    sensors.add(Pair.of(entry.getKey(), con));
                } else {
                    outputs.add(Pair.of(entry.getKey(), con));
                }
            }

            connectors = context.getRoutedConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                ChemicalLogicConnectorSettings con = (ChemicalLogicConnectorSettings) entry.getValue();
                if (con.getConnectorMode() == ConnectorMode.OUTPUT) {
                    outputs.add(Pair.of(entry.getKey(), con));
                }
            }
        }
    }

    @Override
    public void cleanCache() {
        sensors = null;
        outputs = null;
    }

    @Override
    public int getColors() {
        return colors;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(XNET_GUI_ELEMENTS, 11, 90, 11, 10);
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
    public void createGui(IEditorGui iEditorGui) { }

    @Override
    public void update(Map<String, Object> map) { }
}
