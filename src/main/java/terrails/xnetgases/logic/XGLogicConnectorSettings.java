package terrails.xnetgases.logic;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import mcjty.xnet.XNet;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XGLogicConnectorSettings extends AbstractConnectorSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    public static final String TAG_MODE = "mode";
    public static final String TAG_SPEED = "speed";
    public static final String TAG_REDSTONE_OUT = "rsout";

    public static final int SENSORS = 4;
    private final List<XGSensor> sensors;

    private int colors;
    private int speed = 2;

    public XGLogicConnectorSettings(@Nonnull Direction side) {
        super(side);
        sensors = new ArrayList<>(SENSORS);
        for (int i = 0 ; i < SENSORS ; i++) {
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

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(iconGuiElements, 26, 70, 13, 10);
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    private static final Set<String> TAGS = ImmutableSet.of(TAG_REDSTONE_OUT, TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3");

    @Override
    public boolean isEnabled(String tag) {
        if (tag.equals(TAG_FACING)) {
            return advanced;
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
        gui.nl().choices(TAG_SPEED, "Number of ticks for each check", Integer.toString(speed * 5), speeds).nl();
        for (XGSensor sensor : sensors) {
            sensor.createGui(gui);
        }
    }

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        speed = Integer.parseInt((String) data.get(TAG_SPEED)) / 5;
        if (speed == 0) {
            speed = 2;
        }
        for (XGSensor sensor : sensors) {
            sensor.update(data);
        }
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        super.writeToJsonInternal(object);
        setIntegerSafe(object, "speed", speed);
        JsonArray sensorArray = new JsonArray();
        for (XGSensor sensor : sensors) {
            JsonObject o = new JsonObject();
            sensor.writeToJson(o);
            sensorArray.add(o);
        }
        object.add("sensors", sensorArray);
        if (speed == 1) {
            object.add("advancedneeded", new JsonPrimitive(true));
        }
        return object;
    }

    @Override
    public void readFromJson(JsonObject object) {
        super.readFromJsonInternal(object);
        speed = getIntegerNotNull(object, "speed");
        JsonArray sensorArray = object.get("sensors").getAsJsonArray();
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
        speed = tag.getInt("speed");
        if (speed == 0) {
            speed = 2;
        }
        colors = tag.getInt("colors");
        for (XGSensor sensor : sensors) {
            sensor.readFromNBT(tag);
        }
    }

    @Override
    public void writeToNBT(CompoundNBT tag) {
        super.writeToNBT(tag);
        tag.putInt("speed", speed);
        tag.putInt("colors", colors);
        for (XGSensor sensor : sensors) {
            sensor.writeToNBT(tag);
        }
    }
}
