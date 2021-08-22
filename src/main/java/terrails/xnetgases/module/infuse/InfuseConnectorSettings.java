package terrails.xnetgases.module.infuse;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.JSonTools;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import terrails.xnetgases.helper.ChemicalConnectorSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

import static terrails.xnetgases.Constants.*;

public class InfuseConnectorSettings extends ChemicalConnectorSettings<InfusionStack> {

    public enum InfuseMode {
        INS,
        EXT
    }

    private InfuseMode infuseMode = InfuseMode.INS;

    @Nullable
    private Integer priority = 0;
    @Nullable private Integer rate = null;
    @Nullable private Integer minmax = null;
    private int speed = 2;

    private ItemStack filter = ItemStack.EMPTY;

    public InfuseConnectorSettings(@Nonnull Direction side) {
        super(side);
    }

    public InfuseMode getInfuseMode() {
        return infuseMode;
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
        return rate == null ? InfuseChannelModule.maxInfuseRateNormal.get() : rate;
    }

    @Nullable
    public Integer getMinmax() {
        return minmax;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        switch (infuseMode) {
            case INS:
                return new IndicatorIcon(XNET_GUI_ELEMENTS, 0, 70, 13, 10);
            case EXT:
                return new IndicatorIcon(XNET_GUI_ELEMENTS, 13, 70, 13, 10);
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
            maxRate = InfuseChannelModule.maxInfuseRateAdvanced.get();
        } else {
            speeds = new String[] { "20", "60", "100", "200" };
            maxRate = InfuseChannelModule.maxInfuseRateNormal.get();
        }

        sideGui(gui);
        colorsGui(gui);
        redstoneGui(gui);
        gui.nl()
                .choices(TAG_MODE, "Insert or extract mode", infuseMode, InfuseMode.values())
                .choices(TAG_SPEED, "Number of ticks for each operation", Integer.toString(speed * 10), speeds)
                .nl()

                .label("Pri").integer(TAG_PRIORITY, "Insertion priority", priority, 36).nl()

                .label("Rate")
                .integer(TAG_RATE, infuseMode == InfuseMode.EXT ? "Infuse extraction rate|(max " + maxRate + "mb)" : "Infuse insertion rate|(max " + maxRate + "mb)", rate, 36, maxRate)
                .shift(10)
                .label(infuseMode == InfuseMode.EXT ? "Min" : "Max")
                .integer(TAG_MINMAX, infuseMode == InfuseMode.EXT ? "Keep this amount of|infuse in tank" : "Disable insertion if|infuse level is too high", minmax, 36)
                .nl()
                .label("Filter")
                .ghostSlot(TAG_FILTER, filter);
    }

    private final Set<String> INSERT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_RATE, TAG_MINMAX, TAG_PRIORITY, TAG_FILTER);
    private final Set<String> EXTRACT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_RATE, TAG_MINMAX, TAG_PRIORITY, TAG_FILTER, TAG_SPEED);

    @Override
    public boolean isEnabled(String tag) {
        if (infuseMode == InfuseMode.INS) {
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
    public InfusionStack getMatcher() {
        if (!filter.isEmpty() && Capabilities.INFUSION_HANDLER_CAPABILITY != null && filter.getCapability(Capabilities.INFUSION_HANDLER_CAPABILITY).isPresent()) {
            IInfusionHandler handler = filter.getCapability(Capabilities.INFUSION_HANDLER_CAPABILITY).orElseThrow(() -> new IllegalArgumentException("IInfusionHandler Capability doesn't exist!"));
            if (handler.getTanks() > 0) {
                return handler.getChemicalInTank(0);
            }
        }
        return null;
    }

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        infuseMode = InfuseMode.valueOf(((String) data.get(TAG_MODE)).toUpperCase());
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
        setEnumSafe(object, TAG_MODE, infuseMode);
        setIntegerSafe(object, TAG_PRIORITY, priority);
        setIntegerSafe(object, TAG_RATE, rate);
        setIntegerSafe(object, TAG_MINMAX, minmax);
        setIntegerSafe(object, TAG_SPEED, speed);
        if (!filter.isEmpty()) {
            object.add(TAG_FILTER, JSonTools.itemStackToJson(filter));
        }
        if (rate != null && rate > InfuseChannelModule.maxInfuseRateNormal.get()) {
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
        infuseMode = getEnumSafe(object, TAG_MODE, InfuseUtils::getConnectorModeFrom);
        priority = getIntegerSafe(object, TAG_PRIORITY);
        rate = getIntegerSafe(object, TAG_RATE);
        minmax = getIntegerSafe(object, TAG_MINMAX);
        speed = getIntegerNotNull(object, TAG_SPEED);
        if (object.has(TAG_FILTER)) {
            filter = JSonTools.jsonToItemStack(object.get(TAG_FILTER).getAsJsonObject());
        } else filter = ItemStack.EMPTY;
    }

    @Override
    public void readFromNBT(CompoundNBT tag) {
        super.readFromNBT(tag);
        infuseMode = InfuseMode.values()[tag.getByte(TAG_MODE)];

        if (tag.contains(TAG_PRIORITY)) {
            priority = tag.getInt(TAG_PRIORITY);
        } else priority = null;

        if (tag.contains(TAG_RATE)) {
            rate = tag.getInt(TAG_RATE);
        } else rate = null;

        if (tag.contains(TAG_MINMAX)) {
            minmax = tag.getInt(TAG_MINMAX);
        } else minmax = null;

        speed = tag.getInt(TAG_SPEED);
        if (speed == 0) {
            speed = 2;
        }

        if (tag.contains(TAG_FILTER)) {
            CompoundNBT itemTag = tag.getCompound(TAG_FILTER);
            filter = ItemStack.of(itemTag);
        } else filter = ItemStack.EMPTY;
    }

    @Override
    public void writeToNBT(CompoundNBT tag) {
        super.writeToNBT(tag);
        tag.putByte(TAG_MODE, (byte) infuseMode.ordinal());
        if (priority != null) tag.putInt(TAG_PRIORITY, priority);
        if (rate != null) tag.putInt(TAG_RATE, rate);
        if (minmax != null) tag.putInt(TAG_MINMAX, minmax);
        tag.putInt(TAG_SPEED, speed);
        if (!filter.isEmpty()) {
            CompoundNBT itemTag = new CompoundNBT();
            filter.save(itemTag);
            tag.put(TAG_FILTER, itemTag);
        }
    }
}
