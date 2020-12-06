package terrails.xnetgases.slurry;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.JSonTools;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import mcjty.xnet.XNet;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import terrails.xnetgases.Utils;
import terrails.xnetgases.XNetGases;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class SlurryConnectorSettings extends AbstractConnectorSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    public static final String TAG_MODE = "mode";
    public static final String TAG_RATE = "rate";
    public static final String TAG_MINMAX = "minmax";
    public static final String TAG_PRIORITY = "priority";
    public static final String TAG_FILTER = "flt";
    public static final String TAG_SPEED = "speed";

    public enum SlurryMode {
        INS,
        EXT
    }

    private SlurryConnectorSettings.SlurryMode slurryMode = SlurryConnectorSettings.SlurryMode.INS;

    @Nullable
    private Integer priority = 0;
    @Nullable private Integer rate = null;
    @Nullable private Integer minmax = null;
    private int speed = 2;

    private ItemStack filter = ItemStack.EMPTY;

    public SlurryConnectorSettings(@Nonnull Direction side) {
        super(side);
    }

    public SlurryConnectorSettings.SlurryMode getSlurryMode() {
        return slurryMode;
    }

    public int getSpeed() {
        return speed;
    }

    @Nonnull
    public Integer getPriority() {
        return priority == null ? 0 : priority;
    }

    @Nonnull
    public Integer getRate() {
        return rate == null ? XNetGases.maxSlurryRateNormal.get() : rate;
    }

    @Nullable
    public Integer getMinmax() {
        return minmax;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        switch (slurryMode) {
            case INS:
                return new IndicatorIcon(iconGuiElements, 0, 70, 13, 10);
            case EXT:
                return new IndicatorIcon(iconGuiElements, 13, 70, 13, 10);
        }
        return null;
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public void createGui(IEditorGui gui) {
        advanced = gui.isAdvanced();
        String[] speeds;
        int maxRate;
        if (advanced) {
            speeds = new String[] { "10", "20", "60", "100", "200" };
            maxRate = XNetGases.maxSlurryRateAdvanced.get();
        } else {
            speeds = new String[] { "20", "60", "100", "200" };
            maxRate = XNetGases.maxSlurryRateNormal.get();
        }

        sideGui(gui);
        colorsGui(gui);
        redstoneGui(gui);
        gui.nl()
                .choices(TAG_MODE, "Insert or extract mode", slurryMode, SlurryConnectorSettings.SlurryMode.values())
                .choices(TAG_SPEED, "Number of ticks for each operation", Integer.toString(speed * 10), speeds)
                .nl()

                .label("Pri").integer(TAG_PRIORITY, "Insertion priority", priority, 36).nl()

                .label("Rate")
                .integer(TAG_RATE, slurryMode == SlurryConnectorSettings.SlurryMode.EXT ? "Slurry extraction rate|(max " + maxRate + "mb)" : "Slurry insertion rate|(max " + maxRate + "mb)", rate, 36, maxRate)
                .shift(10)
                .label(slurryMode == SlurryConnectorSettings.SlurryMode.EXT ? "Min" : "Max")
                .integer(TAG_MINMAX, slurryMode == SlurryConnectorSettings.SlurryMode.EXT ? "Keep this amount of|slurry in tank" : "Disable insertion if|slurry level is too high", minmax, 36)
                .nl()
                .label("Filter")
                .ghostSlot(TAG_FILTER, filter);
    }

    private final Set<String> INSERT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_RATE, TAG_MINMAX, TAG_PRIORITY, TAG_FILTER);
    private final Set<String> EXTRACT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_RATE, TAG_MINMAX, TAG_PRIORITY, TAG_FILTER, TAG_SPEED);

    @Override
    public boolean isEnabled(String tag) {
        if (slurryMode == SlurryConnectorSettings.SlurryMode.INS) {
            if (tag.equals(TAG_FACING)) {
                return advanced;
            }
            return INSERT_TAGS.contains(tag);
        } else {
            if (tag.equals(TAG_FACING)) {
                return advanced;
            }
            return EXTRACT_TAGS.contains(tag);
        }
    }

    @Nullable
    public SlurryStack getMatcher() {
        if (!filter.isEmpty() && Capabilities.SLURRY_HANDLER_CAPABILITY != null && filter.getCapability(Capabilities.SLURRY_HANDLER_CAPABILITY).isPresent()) {
            ISlurryHandler handler = filter.getCapability(Capabilities.SLURRY_HANDLER_CAPABILITY).orElseThrow(() -> new IllegalArgumentException("ISlurryHandler Capability doesn't exist!"));
            if (handler.getTanks() > 0) {
                return handler.getChemicalInTank(0);
            }
        }
        return null;
    }

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        slurryMode = SlurryConnectorSettings.SlurryMode.valueOf(((String) data.get(TAG_MODE)).toUpperCase());
        rate = (Integer) data.get(TAG_RATE);
        minmax = (Integer) data.get(TAG_MINMAX);
        priority = (Integer) data.get(TAG_PRIORITY);
        speed = Integer.parseInt((String) data.get(TAG_SPEED)) / 10;
        if (speed == 0) {
            speed = 2;
        }
        filter = (ItemStack) data.get(TAG_FILTER);
        if (filter == null) {
            filter = ItemStack.EMPTY;
        }
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        super.writeToJsonInternal(object);
        setEnumSafe(object, "slurrymode", slurryMode);
        setIntegerSafe(object, "priority", priority);
        setIntegerSafe(object, "rate", rate);
        setIntegerSafe(object, "minmax", minmax);
        setIntegerSafe(object, "speed", speed);
        if (!filter.isEmpty()) {
            object.add("filter", JSonTools.itemStackToJson(filter));
        }
        if (rate != null && rate > XNetGases.maxSlurryRateNormal.get()) {
            object.add("advancedneeded", new JsonPrimitive(true));
        }
        if (speed == 1) {
            object.add("advancedneeded", new JsonPrimitive(true));
        }
        return object;
    }

    @Override
    public void readFromJson(JsonObject object) {
        super.readFromJsonInternal(object);
        slurryMode = getEnumSafe(object, "slurrymode", Utils::getSlurryConnectorModeFrom);
        priority = getIntegerSafe(object, "priority");
        rate = getIntegerSafe(object, "rate");
        minmax = getIntegerSafe(object, "minmax");
        speed = getIntegerNotNull(object, "speed");
        if (object.has("filter")) {
            filter = JSonTools.jsonToItemStack(object.get("filter").getAsJsonObject());
        } else {
            filter = ItemStack.EMPTY;
        }
    }

    @Override
    public void readFromNBT(CompoundNBT tag) {
        super.readFromNBT(tag);
        slurryMode = SlurryConnectorSettings.SlurryMode.values()[tag.getByte("slurryMode")];
        if (tag.contains("priority")) {
            priority = tag.getInt("priority");
        } else {
            priority = null;
        }
        if (tag.contains("rate")) {
            rate = tag.getInt("rate");
        } else {
            rate = null;
        }
        if (tag.contains("minmax")) {
            minmax = tag.getInt("minmax");
        } else {
            minmax = null;
        }
        speed = tag.getInt("speed");
        if (speed == 0) {
            speed = 2;
        }
        if (tag.contains("filter")) {
            CompoundNBT itemTag = tag.getCompound("filter");
            filter = ItemStack.read(itemTag);
        } else {
            filter = ItemStack.EMPTY;
        }
    }

    @Override
    public void writeToNBT(CompoundNBT tag) {
        super.writeToNBT(tag);
        tag.putByte("slurryMode", (byte) slurryMode.ordinal());
        if (priority != null) {
            tag.putInt("priority", priority);
        }
        if (rate != null) {
            tag.putInt("rate", rate);
        }
        if (minmax != null) {
            tag.putInt("minmax", minmax);
        }
        tag.putInt("speed", speed);
        if (!filter.isEmpty()) {
            CompoundNBT itemTag = new CompoundNBT();
            filter.write(itemTag);
            tag.put("filter", itemTag);
        }
    }
}
