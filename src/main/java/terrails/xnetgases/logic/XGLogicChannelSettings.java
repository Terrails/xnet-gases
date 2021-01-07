package terrails.xnetgases.logic;

import com.google.gson.JsonObject;
import mcjty.lib.varia.WorldTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.DefaultChannelSettings;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.XNet;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XGLogicChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    private int delay = 0;
    private int colors = 0;
    private List<Pair<SidedConsumer, XGLogicConnectorSettings>> sensors = null;

    @Override
    public JsonObject writeToJson() {
        return new JsonObject();
    }

    @Override
    public void readFromNBT(CompoundNBT tag) {
        delay = tag.getInt("delay");
        colors = tag.getInt("colors");
    }

    @Override
    public void writeToNBT(CompoundNBT tag) {
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
        World world = context.getControllerWorld();

        colors = 0;
        for (Pair<SidedConsumer, XGLogicConnectorSettings> entry : sensors) {
            XGLogicConnectorSettings settings = entry.getValue();
            if (d % settings.getSpeed() != 0) {
                // Use the color settings from this connector as we last remembered it
                colors |= settings.getColorMask();
                continue;
            }
            int sensorColors = 0;
            BlockPos connectorPos = context.findConsumerPosition(entry.getKey().getConsumerId());
            if (connectorPos != null) {
                Direction side = entry.getKey().getSide();
                BlockPos pos = connectorPos.offset(side);
                if (!WorldTools.isLoaded(world, pos)) {
                    // If it is not chunkloaded we just use the color settings as we last remembered it
                    colors |= settings.getColorMask();
                    continue;
                }

                boolean sense = !checkRedstone(world, settings, connectorPos);
                if (sense && !context.matchColor(settings.getColorsMask())) {
                    sense = false;
                }

                // If sense is false the sensor is disabled which means the colors from it will also be disabled
                if (sense) {
                    TileEntity te = world.getTileEntity(pos);

                    for (XGSensor sensor : settings.getSensors()) {
                        if (sensor.test(te, settings)) {
                            sensorColors |= 1 << sensor.getOutputColor().ordinal();
                        }
                    }
                }
            }
            settings.setColorMask(sensorColors);
            colors |= sensorColors;
        }
    }

    private void updateCache(int channel, IControllerContext context) {
        if (sensors == null) {
            sensors = new ArrayList<>();
            context.getConnectors(channel).entrySet().stream()
                    .map((entry) -> Pair.of(entry.getKey(), (XGLogicConnectorSettings) entry.getValue()))
                    .forEach(sensors::add);
        }
    }

    @Override
    public void cleanCache() {
        sensors = null;
    }

    @Override
    public int getColors() {
        return colors;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(iconGuiElements, 11, 90, 11, 10);
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
    public void createGui(IEditorGui iEditorGui) {

    }

    @Override
    public void update(Map<String, Object> map) {

    }
}
