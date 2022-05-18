package terrails.xnetgases.module.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.JSonTools;
import mcjty.rftoolsbase.api.xnet.channels.Color;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.helper.BaseStringTranslators;
import mekanism.api.chemical.Chemical;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import terrails.xnetgases.module.chemical.ChemicalEnums;
import terrails.xnetgases.module.chemical.utils.ChemicalHelper;
import terrails.xnetgases.module.logic.ChemicalLogicEnums.*;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Function;

public class ChemicalSensor {

    public final String modeTag;
    public final String operatorTag;
    public final String amountTag;
    public final String colorTag;
    public final String filterTag;

    private int amount = 0;

    private SensorMode sensorMode = SensorMode.OFF;
    private Color outputColor = Color.OFF;
    private SensorOperator operator = SensorOperator.EQUAL;
    private ItemStack filter = ItemStack.EMPTY;

    public ChemicalSensor(int index) {
        String temp = String.format("sensor%s_", index);
        modeTag = temp + "mode";
        operatorTag = temp + "operator";
        amountTag = temp + "amount";
        colorTag = temp + "color";
        filterTag = temp + "filter";
    }

    public Color getOutputColor() {
        return outputColor;
    }

    public boolean isEnabled(String tag) {
        if ((modeTag).equals(tag)) {
            return true;
        }
        if ((operatorTag).equals(tag)) {
            return true;
        }
        if ((amountTag).equals(tag)) {
            return true;
        }
        if ((colorTag).equals(tag)) {
            return true;
        }
        if ((filterTag).equals(tag)) {
            return sensorMode != SensorMode.OFF;
        }
        return false;
    }

    public void createGui(IEditorGui gui) {
        gui
                .choices(modeTag, "Sensor mode", sensorMode, SensorMode.values())
                .choices(operatorTag, "Operator", operator, SensorOperator.values())
                .integer(amountTag, "Amount to compare with", amount, 46)
                .colors(colorTag, "Output color", outputColor.getColor(), Color.COLORS)
                .ghostSlot(filterTag, filter)
                .nl();
    }

    public boolean test(@Nullable BlockEntity be, ChemicalLogicConnectorSettings settings) {
        ChemicalEnums.Type type = sensorMode.toType();
        if (type == null || !this.filter.is(MekanismBlocks.CREATIVE_CHEMICAL_TANK.asItem())) return false;

        return switch (sensorMode) {
            case GAS, SLURRY, PIGMENT, INFUSE -> ChemicalHelper.handler(be, settings.getFacing(), type)
                    .map(handler -> ChemicalHelper.handler(filter, null, type)
                                    .map(filterHandler -> {
                                        if (filterHandler.getTanks() <= 0) return false;
                                        Chemical<?> chemical = filterHandler.getChemicalInTank(0).getType();
                                        return operator.match(ChemicalHelper.amountInTank(handler, settings.getFacing(), chemical, type), amount);
                                    }).orElse(filter.isEmpty() && operator.match(ChemicalHelper.amountInTank(handler, settings.getFacing(), type), amount))
                    ).orElse(false);
            default -> false;
        };
    }

    public void update(Map<String, Object> data) {
        sensorMode = this.getObjectFromMap(data, modeTag, SensorMode.OFF, (object) -> SensorMode.valueOf(((String) object).toUpperCase()));
        operator = this.getObjectFromMap(data, operatorTag, SensorOperator.EQUAL, (object) -> SensorOperator.byCode(((String) object).toUpperCase()));
        amount = this.getObjectFromMap(data, amountTag, 0, Integer.class::cast);
        outputColor = this.getObjectFromMap(data, colorTag, Color.OFF, (object) -> Color.colorByValue((Integer) object));
        filter = ChemicalHelper.normalizeStack(this.getObjectFromMap(data, filterTag, ItemStack.EMPTY, ItemStack.class::cast), this.sensorMode.toType());
    }

    private <T> T getObjectFromMap(Map<String, Object> data, String key, T defaultValue, Function<Object, T> function) {
        Object object = data.get(key);
        if (object != null) {
            return function.apply(object);
        } else return defaultValue;
    }

    public void readFromNBT(CompoundTag tag) {
        sensorMode = SensorMode.values()[tag.getByte(modeTag)];
        operator = SensorOperator.values()[tag.getByte(operatorTag)];
        amount = tag.getInt(amountTag);
        outputColor = Color.values()[tag.getByte(colorTag)];
        if (tag.contains(filterTag)) {
            CompoundTag itemTag = tag.getCompound(filterTag);
            filter = ChemicalHelper.normalizeStack(ItemStack.of(itemTag), this.sensorMode.toType());
        } else {
            this.filter = ItemStack.EMPTY;
        }
    }

    public void writeToNBT(CompoundTag tag) {
        tag.putByte(modeTag, (byte) sensorMode.ordinal());
        tag.putByte(operatorTag, (byte) operator.ordinal());
        tag.putInt(amountTag, amount);
        tag.putByte(colorTag, (byte) outputColor.ordinal());
        if (!filter.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            filter.save(itemTag);
            tag.put(filterTag, itemTag);
        }
    }

    public void writeToJson(JsonObject json) {
        json.add(modeTag, new JsonPrimitive(sensorMode.name()));
        json.add(colorTag, new JsonPrimitive(outputColor.name()));
        json.add(operatorTag, new JsonPrimitive(operator.name()));
        json.add(amountTag, new JsonPrimitive(amount));
        if (!filter.isEmpty()) {
            json.add(filterTag, JSonTools.itemStackToJson(filter));
        }
    }

    public void readFromJson(JsonObject json) {
        amount = json.has(amountTag) ? json.get(amountTag).getAsInt() : 0;
        operator = json.has(operatorTag) ? SensorOperator.byName(json.get(operatorTag).getAsString()) : SensorOperator.EQUAL;
        outputColor = json.has(colorTag) ? BaseStringTranslators.getColor(json.get(colorTag).getAsString()) : Color.OFF;
        sensorMode = json.has(modeTag) ? SensorMode.byName(json.get(modeTag).getAsString()) : SensorMode.OFF;
        if (json.has(filterTag)) {
            filter = ChemicalHelper.normalizeStack(JSonTools.jsonToItemStack(json.get(filterTag).getAsJsonObject()), this.sensorMode.toType());
        } else filter = ItemStack.EMPTY;
    }
}
