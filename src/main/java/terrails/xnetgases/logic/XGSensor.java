package terrails.xnetgases.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.JSonTools;
import mcjty.rftoolsbase.api.xnet.channels.Color;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.helper.BaseStringTranslators;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.slurry.Slurry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import terrails.xnetgases.gas.GasUtils;
import terrails.xnetgases.slurry.SlurryUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public class XGSensor {

    public final String TAG_MODE;
    public final String TAG_OPERATOR;
    public final String TAG_AMOUNT;
    public final String TAG_COLOR;
    public final String TAG_FILTER;

    public enum SensorMode {
        OFF,
        GAS,
        SLURRY
    }

    // Custom Operator because the original uses integer instead of long.
    // Creative gas tanks use Long#MAX_VALUE which results in an integer overflow.
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
        TAG_MODE = temp + "mode";
        TAG_OPERATOR = temp + "operator";
        TAG_AMOUNT = temp + "amount";
        TAG_COLOR = temp + "color";
        TAG_FILTER = temp + "filter";
    }

    public Color getOutputColor() {
        return outputColor;
    }

    public boolean isEnabled(String tag) {
        if ((TAG_MODE).equals(tag)) {
            return true;
        }
        if ((TAG_OPERATOR).equals(tag)) {
            return true;
        }
        if ((TAG_AMOUNT).equals(tag)) {
            return true;
        }
        if ((TAG_COLOR).equals(tag)) {
            return true;
        }
        if ((TAG_FILTER).equals(tag)) {
            return sensorMode != SensorMode.OFF;
        }
        return false;
    }

    public void createGui(IEditorGui gui) {
        gui
                .choices(TAG_MODE, "Sensor mode", sensorMode, SensorMode.values())
                .choices(TAG_OPERATOR, "Operator", operator, Operator.values())
                .integer(TAG_AMOUNT, "Amount to compare with", amount, 46)
                .colors(TAG_COLOR, "Output color", outputColor.getColor(), Color.COLORS)
                .ghostSlot(TAG_FILTER, filter)
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
        }
        return false;
    }

    public void update(Map<String, Object> data) {
        sensorMode = this.getObjectFromMap(data, TAG_MODE, SensorMode.OFF, (object) -> SensorMode.valueOf(((String) object).toUpperCase()));
        operator = this.getObjectFromMap(data, TAG_OPERATOR, Operator.EQUAL, (object) -> Operator.byCode(((String) object).toUpperCase()));
        amount = this.getObjectFromMap(data, TAG_AMOUNT, 0, Integer.class::cast);
        outputColor = this.getObjectFromMap(data, TAG_COLOR, Color.OFF, (object) -> Color.colorByValue((Integer) object));
        filter = this.getObjectFromMap(data, TAG_FILTER, ItemStack.EMPTY, ItemStack.class::cast);
    }

    private <T> T getObjectFromMap(Map<String, Object> data, String key, T defaultValue, Function<Object, T> function) {
        Object object = data.get(key);
        if (object != null) {
            return function.apply(object);
        } else return defaultValue;
    }

    public void readFromNBT(CompoundNBT tag) {
        sensorMode = SensorMode.values()[tag.getByte(TAG_MODE)];
        operator = Operator.values()[tag.getByte(TAG_OPERATOR)];
        amount = tag.getInt(TAG_AMOUNT);
        outputColor = Color.values()[tag.getByte(TAG_COLOR)];
        if (tag.contains(TAG_FILTER)) {
            CompoundNBT itemTag = tag.getCompound(TAG_FILTER);
            filter = ItemStack.read(itemTag);
        } else {
            this.filter = ItemStack.EMPTY;
        }
    }

    public void writeToNBT(CompoundNBT tag) {
        tag.putByte(TAG_MODE, (byte) sensorMode.ordinal());
        tag.putByte(TAG_OPERATOR, (byte) operator.ordinal());
        tag.putInt(TAG_AMOUNT, amount);
        tag.putByte(TAG_COLOR, (byte) outputColor.ordinal());
        if (!filter.isEmpty()) {
            CompoundNBT itemTag = new CompoundNBT();
            filter.write(itemTag);
            tag.put(TAG_FILTER, itemTag);
        }
    }

    public void writeToJson(JsonObject json) {
        json.add(TAG_MODE, new JsonPrimitive(sensorMode.name()));
        json.add(TAG_COLOR, new JsonPrimitive(outputColor.name()));
        json.add(TAG_OPERATOR, new JsonPrimitive(operator.name()));
        json.add(TAG_AMOUNT, new JsonPrimitive(amount));
        if (!filter.isEmpty()) {
            json.add("filter", JSonTools.itemStackToJson(filter));
        }
    }

    public void readFromJson(JsonObject json) {
        amount = json.has(TAG_AMOUNT) ? json.get(TAG_AMOUNT).getAsInt() : 0;
        operator = json.has(TAG_OPERATOR) ? LogicUtils.getOperatorFrom(json.get(TAG_OPERATOR).getAsString()) : Operator.EQUAL;
        outputColor = json.has(TAG_COLOR) ? BaseStringTranslators.getColor(json.get(TAG_COLOR).getAsString()) : Color.OFF;
        sensorMode = json.has(TAG_MODE) ? LogicUtils.getSensorModeFrom(json.get(TAG_MODE).getAsString()) : SensorMode.OFF;
        if (json.has("filter")) {
            filter = JSonTools.jsonToItemStack(json.get("filter").getAsJsonObject());
        } else filter = ItemStack.EMPTY;
    }
}
