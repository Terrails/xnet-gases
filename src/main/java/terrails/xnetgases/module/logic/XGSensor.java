package terrails.xnetgases.module.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.JSonTools;
import mcjty.rftoolsbase.api.xnet.channels.Color;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.helper.BaseStringTranslators;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.slurry.Slurry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import terrails.xnetgases.module.gas.GasUtils;
import terrails.xnetgases.module.infuse.InfuseUtils;
import terrails.xnetgases.module.pigment.PigmentUtils;
import terrails.xnetgases.module.slurry.SlurryUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public class XGSensor {

    public final String modeTag;
    public final String operatorTag;
    public final String amountTag;
    public final String colorTag;
    public final String filterTag;

    public enum SensorMode {
        OFF,
        GAS,
        SLURRY,
        INFUSE,
        PIGMENT
    }

    // Custom Operator because the original uses integer instead of long.
    // Creative tanks use Long#MAX_VALUE which results in an overflow.
    public enum Operator {
        EQUAL("=", Long::equals),
        NOTEQUAL("!=", (i1, i2) -> !i1.equals(i2)),
        LESS("<", (i1, i2) -> i1 < i2),
        GREATER(">", (i1, i2) -> i1 > i2),
        LESSOREQUAL("<=", (i1, i2) -> i1 <= i2),
        GREATEROREQUAL(">=", (i1, i2) -> i1 >= i2);

        private final String code;
        private final BiPredicate<Long, Long> matcher;

        private static final Map<String, Operator> OPERATOR_MAP = Arrays.stream(Operator.values()).collect(Collectors.toMap(op -> op.code, op -> op));

        Operator(String code, BiPredicate<Long, Long> matcher) {
            this.code = code;
            this.matcher = matcher;
        }

        public static Operator byCode(String name) {
            return OPERATOR_MAP.get(name);
        }

        public boolean match(long i1, long i2) {
            return matcher.test(i1, i2);
        }

        @Override
        public String toString() {
            return code;
        }
    }

    private int amount = 0;

    private SensorMode sensorMode = SensorMode.OFF;
    private Color outputColor = Color.OFF;
    private Operator operator = Operator.EQUAL;
    private ItemStack filter = ItemStack.EMPTY;

    public XGSensor(int index) {
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
                .choices(operatorTag, "Operator", operator, Operator.values())
                .integer(amountTag, "Amount to compare with", amount, 46)
                .colors(colorTag, "Output color", outputColor.getColor(), Color.COLORS)
                .ghostSlot(filterTag, filter)
                .nl();
    }

    public boolean test(@Nullable TileEntity te, XGLogicConnectorSettings settings) {
        switch (sensorMode) {
            case GAS: return GasUtils.getGasHandlerFor(te, settings.getFacing())
                    .map(handler -> GasUtils.getGasHandlerFor(filter, null).map(filterHandler -> {
                        if (filterHandler.getTanks() <= 0) {
                            return false;
                        }

                        Gas filterChemical = filterHandler.getChemicalInTank(0).getType();
                        return operator.match(GasUtils.getGasCount(handler, settings.getFacing(), filterChemical), amount);
                    }).orElseGet(() -> filter.isEmpty() && operator.match(GasUtils.getGasCount(handler, settings.getFacing()), amount)))
                    .orElse(false);
            case SLURRY: return SlurryUtils.getSlurryHandlerFor(te, settings.getFacing())
                    .map(handler -> SlurryUtils.getSlurryHandlerFor(filter, null).map(filterHandler -> {
                        if (filterHandler.getTanks() <= 0) {
                            return false;
                        }

                        Slurry filterChemical = filterHandler.getChemicalInTank(0).getType();
                        return operator.match(SlurryUtils.getSlurryCount(handler, settings.getFacing(), filterChemical), amount);
                    }).orElseGet(() -> filter.isEmpty() && operator.match(SlurryUtils.getSlurryCount(handler, settings.getFacing()), amount)))
                    .orElse(false);
            case INFUSE: return InfuseUtils.getInfuseHandlerFor(te, settings.getFacing())
                    .map(handler -> InfuseUtils.getInfuseHandlerFor(filter, null).map(filterHandler -> {
                        if (filterHandler.getTanks() <= 0) {
                            return false;
                        }

                        InfuseType filterChemical = filterHandler.getChemicalInTank(0).getType();
                        return operator.match(InfuseUtils.getInfuseCount(handler, settings.getFacing(), filterChemical), amount);
                    }).orElseGet(() -> filter.isEmpty() && operator.match(InfuseUtils.getInfuseCount(handler, settings.getFacing()), amount)))
                    .orElse(false);
            case PIGMENT: return PigmentUtils.getPigmentHandlerFor(te, settings.getFacing())
                    .map(handler -> PigmentUtils.getPigmentHandlerFor(filter, null).map(filterHandler -> {
                        if (filterHandler.getTanks() <= 0) {
                            return false;
                        }

                        Pigment filterChemical = filterHandler.getChemicalInTank(0).getType();
                        return operator.match(PigmentUtils.getPigmentCount(handler, settings.getFacing(), filterChemical), amount);
                    }).orElseGet(() -> filter.isEmpty() && operator.match(PigmentUtils.getPigmentCount(handler, settings.getFacing()), amount)))
                    .orElse(false);
        }
        return false;
    }

    public void update(Map<String, Object> data) {
        sensorMode = this.getObjectFromMap(data, modeTag, SensorMode.OFF, (object) -> SensorMode.valueOf(((String) object).toUpperCase()));
        operator = this.getObjectFromMap(data, operatorTag, Operator.EQUAL, (object) -> Operator.byCode(((String) object).toUpperCase()));
        amount = this.getObjectFromMap(data, amountTag, 0, Integer.class::cast);
        outputColor = this.getObjectFromMap(data, colorTag, Color.OFF, (object) -> Color.colorByValue((Integer) object));
        filter = this.getObjectFromMap(data, filterTag, ItemStack.EMPTY, ItemStack.class::cast);
    }

    private <T> T getObjectFromMap(Map<String, Object> data, String key, T defaultValue, Function<Object, T> function) {
        Object object = data.get(key);
        if (object != null) {
            return function.apply(object);
        } else return defaultValue;
    }

    public void readFromNBT(CompoundNBT tag) {
        sensorMode = SensorMode.values()[tag.getByte(modeTag)];
        operator = Operator.values()[tag.getByte(operatorTag)];
        amount = tag.getInt(amountTag);
        outputColor = Color.values()[tag.getByte(colorTag)];
        if (tag.contains(filterTag)) {
            CompoundNBT itemTag = tag.getCompound(filterTag);
            filter = ItemStack.of(itemTag);
        } else {
            this.filter = ItemStack.EMPTY;
        }
    }

    public void writeToNBT(CompoundNBT tag) {
        tag.putByte(modeTag, (byte) sensorMode.ordinal());
        tag.putByte(operatorTag, (byte) operator.ordinal());
        tag.putInt(amountTag, amount);
        tag.putByte(colorTag, (byte) outputColor.ordinal());
        if (!filter.isEmpty()) {
            CompoundNBT itemTag = new CompoundNBT();
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
        operator = json.has(operatorTag) ? LogicUtils.getOperatorFrom(json.get(operatorTag).getAsString()) : Operator.EQUAL;
        outputColor = json.has(colorTag) ? BaseStringTranslators.getColor(json.get(colorTag).getAsString()) : Color.OFF;
        sensorMode = json.has(modeTag) ? LogicUtils.getSensorModeFrom(json.get(modeTag).getAsString()) : SensorMode.OFF;
        if (json.has(filterTag)) {
            filter = JSonTools.jsonToItemStack(json.get(filterTag).getAsJsonObject());
        } else filter = ItemStack.EMPTY;
    }
}
