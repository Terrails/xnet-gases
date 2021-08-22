package terrails.xnetgases.module.logic;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static terrails.xnetgases.Constants.*;

public class XGLogicConnectorSettings extends AbstractConnectorSettings {

    public static final String TAG_REDSTONE_OUT = "rsout";
    public static final String TAG_SENSORS = "sensors";
    public static final String TAG_COLORS = "colors";

    private static final Set<String> TAGS = ImmutableSet.of(TAG_REDSTONE_OUT, TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3");

    public enum LogicMode {
        SENSOR,
        OUTPUT
    }

    public static final int SENSORS = 4;

    private LogicMode logicMode = LogicMode.SENSOR;
    private final List<XGSensor> sensors;

    private int colors;
    private int speed = 2;
    private Integer redstoneOut;

    public XGLogicConnectorSettings(@Nonnull Direction side) {
        super(side);
        sensors = new ArrayList<>(SENSORS);
        for (int i = 0; i < SENSORS; i++) {
            sensors.add(new XGSensor(i));
        }
    }

    public List<XGSensor> getSensors() {
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

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        switch (logicMode) {
            case SENSOR:
                return new IndicatorIcon(XNET_GUI_ELEMENTS, 26, 70, 13, 10);
            case OUTPUT:
                return new IndicatorIcon(XNET_GUI_ELEMENTS, 39, 70, 13, 10);
        }
        return null;
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }


    @Override
    public boolean isEnabled(String tag) {
        if (tag.equals(TAG_FACING)) {
            return advanced && logicMode != LogicMode.OUTPUT;
        }
        if (tag.equals(TAG_SPEED)) {
            return true;
        }
        for (XGSensor sensor : sensors) {
            if (sensor.isEnabled(tag)) {
                return true;
            }
        }

        return TAGS.contains(tag);
    }

    public int getSpeed() {
        return speed;
    }

    public LogicMode getLogicMode() {
        return logicMode;
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
                .choices(TAG_MODE, "Sensor or Output mode", logicMode, LogicMode.values())
                .choices(TAG_SPEED, (logicMode == LogicMode.SENSOR ? "Number of ticks for each check" : "Number of ticks for each operation"), Integer.toString(speed * 5), speeds)
                .nl();

        switch (logicMode) {
            case SENSOR:
                for (XGSensor sensor : sensors) {
                    sensor.createGui(gui);
                }
                break;
            case OUTPUT:
                gui.label("Redstone:")
                        .integer(TAG_REDSTONE_OUT, "Redstone output value", redstoneOut, 40, 16)
                        .nl();
                break;
        }

    }

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        logicMode = LogicMode.valueOf(((String)data.get(TAG_MODE)).toUpperCase());

        speed = Integer.parseInt((String) data.get(TAG_SPEED)) / 5;
        if (speed == 0) {
            speed = 2;
        }
        if (logicMode == LogicMode.SENSOR) {
            for (XGSensor sensor : sensors) {
                sensor.update(data);
            }
        } else {
            redstoneOut = (Integer) data.get(TAG_REDSTONE_OUT);
        }
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        super.writeToJsonInternal(object);
        setEnumSafe(object, TAG_MODE, logicMode);
        setIntegerSafe(object, TAG_SPEED, speed);
        JsonArray sensorArray = new JsonArray();
        for (XGSensor sensor : sensors) {
            JsonObject o = new JsonObject();
            sensor.writeToJson(o);
            sensorArray.add(o);
        }
        object.add(TAG_SENSORS, sensorArray);
        if (speed == 1) {
            object.add("advancedneeded", new JsonPrimitive(true));
        }
        return object;
    }

    @Override
    public void readFromJson(JsonObject object) {
        super.readFromJsonInternal(object);
        if (object.has("logicmode")) {
            logicMode = getEnumSafe(object, "logicmode", LogicUtils::getLogicModeFrom);
        } else logicMode = getEnumSafe(object, TAG_MODE, LogicUtils::getLogicModeFrom);
        speed = getIntegerNotNull(object, TAG_SPEED);
        JsonArray sensorArray = object.get(TAG_SENSORS).getAsJsonArray();
        sensors.clear();
        for (JsonElement oe : sensorArray) {
            JsonObject o = oe.getAsJsonObject();
            XGSensor sensor = new XGSensor(sensors.size());
            sensor.readFromJson(o);
            sensors.add(sensor);
        }
    }

    @Override
    public void readFromNBT(CompoundNBT tag) {
        super.readFromNBT(tag);
        if (tag.contains("logicMode")) {
            logicMode = LogicMode.values()[tag.getByte("logicMode")];
        } else logicMode = LogicMode.values()[tag.getByte(TAG_MODE)];
        speed = tag.getInt(TAG_SPEED);
        if (speed == 0) {
            speed = 2;
        }
        colors = tag.getInt(TAG_COLORS);
        for (XGSensor sensor : sensors) {
            sensor.readFromNBT(tag);
        }
        redstoneOut = tag.getInt(TAG_REDSTONE_OUT);
    }

    @Override
    public void writeToNBT(CompoundNBT tag) {
        super.writeToNBT(tag);
        tag.putByte(TAG_MODE, (byte) logicMode.ordinal());
        tag.putInt(TAG_SPEED, speed);
        tag.putInt(TAG_COLORS, colors);
        for (XGSensor sensor : sensors) {
            sensor.writeToNBT(tag);
        }
        if (redstoneOut != null) {
            tag.putInt(TAG_REDSTONE_OUT, redstoneOut);
        }
    }
}
