package terrails.xnetgases.module.logic;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import terrails.xnetgases.module.logic.ChemicalLogicEnums.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static terrails.xnetgases.Constants.*;

public class ChemicalLogicConnectorSettings extends AbstractConnectorSettings {

    public static final String TAG_REDSTONE_OUT = "rsout";
    public static final String TAG_SENSORS = "sensors";
    public static final String TAG_COLORS = "colors";

    private static final Set<String> TAGS = ImmutableSet.of(TAG_REDSTONE_OUT, TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3");

    public static final int SENSORS = 4;

    private ConnectorMode connectorMode = ConnectorMode.SENSOR;
    private final List<ChemicalSensor> sensors;

    private int colors;
    private int speed = 2;
    private Integer redstoneOut;

    public ChemicalLogicConnectorSettings(@Nonnull Direction side) {
        super(side);
        sensors = new ArrayList<>(SENSORS);
        for (int i = 0; i < SENSORS; i++) sensors.add(new ChemicalSensor(i));
    }

    public List<ChemicalSensor> getSensors() {
        return sensors;
    }

    public void setColorMask(int colors) {
        this.colors = colors;
    }

    public int getColorMask() {
        return colors;
    }

    public Integer getRedstoneOut() {
        return redstoneOut;
    }

    @Override
    public boolean isEnabled(String tag) {
        if (tag.equals(TAG_FACING)) {
            return advanced && connectorMode != ConnectorMode.OUTPUT;
        }
        if (tag.equals(TAG_SPEED)) {
            return true;
        }
        for (ChemicalSensor sensor : sensors) {
            if (sensor.isEnabled(tag)) {
                return true;
            }
        }

        return TAGS.contains(tag);
    }

    public int getSpeed() {
        return speed;
    }

    public ConnectorMode getConnectorMode() {
        return connectorMode;
    }

    @Override
    public void createGui(IEditorGui gui) {
        advanced = gui.isAdvanced();
        String[] speeds;
        if (advanced) {
            speeds = new String[]{"5", "10", "20", "60", "100", "200"};
        } else {
            speeds = new String[]{"10", "20", "60", "100", "200"};
        }
        sideGui(gui);
        colorsGui(gui);
        redstoneGui(gui);
        gui.nl()
                .choices(TAG_MODE, "Sensor or Output mode", connectorMode, ConnectorMode.values())
                .choices(TAG_SPEED, (connectorMode == ConnectorMode.SENSOR ? "Number of ticks for each check" : "Number of ticks for each operation"), Integer.toString(speed * 5), speeds)
                .nl();

        switch (connectorMode) {
            case SENSOR -> { for (ChemicalSensor sensor : sensors) sensor.createGui(gui); }
            case OUTPUT -> gui.label("Redstone:").integer(TAG_REDSTONE_OUT, "Redstone output value", redstoneOut, 40, 16).nl();
        }

    }

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        connectorMode = ConnectorMode.valueOf(((String)data.get(TAG_MODE)).toUpperCase());
        speed = Integer.parseInt((String) data.get(TAG_SPEED)) / 5;
        if (speed == 0) speed = 2;
        if (connectorMode == ConnectorMode.SENSOR) { for (ChemicalSensor sensor : sensors) sensor.update(data); }
        else redstoneOut = (Integer) data.get(TAG_REDSTONE_OUT);
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putByte(TAG_MODE, (byte) connectorMode.ordinal());
        tag.putInt(TAG_SPEED, speed);
        tag.putInt(TAG_COLORS, colors);
        for (ChemicalSensor sensor : sensors) sensor.writeToNBT(tag);
        if (redstoneOut != null) tag.putInt(TAG_REDSTONE_OUT, redstoneOut);
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        connectorMode = ConnectorMode.values()[tag.getByte(TAG_MODE)];
        speed = tag.getInt(TAG_SPEED);
        if (speed == 0) speed = 2;
        colors = tag.getInt(TAG_COLORS);
        for (ChemicalSensor sensor : sensors) sensor.readFromNBT(tag);
        redstoneOut = tag.getInt(TAG_REDSTONE_OUT);
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return switch (connectorMode) {
            case SENSOR -> new IndicatorIcon(XNET_GUI_ELEMENTS, 26, 70, 13, 10);
            case OUTPUT -> new IndicatorIcon(XNET_GUI_ELEMENTS, 39, 70, 13, 10);
        };
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        super.writeToJsonInternal(object);
        setEnumSafe(object, TAG_MODE, connectorMode);
        setIntegerSafe(object, TAG_SPEED, speed);
        JsonArray sensorArray = new JsonArray();
        for (ChemicalSensor sensor : sensors) {
            JsonObject o = new JsonObject();
            sensor.writeToJson(o);
            sensorArray.add(o);
        }
        object.add(TAG_SENSORS, sensorArray);
        if (speed == 1) object.add("advancedneeded", new JsonPrimitive(true));
        return object;
    }

    @Override
    public void readFromJson(JsonObject object) {
        super.readFromJsonInternal(object);
        connectorMode = getEnumSafe(object, TAG_MODE, ConnectorMode::byName);
        speed = getIntegerNotNull(object, TAG_SPEED);
        JsonArray sensorArray = object.get(TAG_SENSORS).getAsJsonArray();
        sensors.clear();
        for (JsonElement oe : sensorArray) {
            JsonObject o = oe.getAsJsonObject();
            ChemicalSensor sensor = new ChemicalSensor(sensors.size());
            sensor.readFromJson(o);
            sensors.add(sensor);
        }
    }
}
