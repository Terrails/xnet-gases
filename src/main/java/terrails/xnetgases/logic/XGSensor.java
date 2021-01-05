package terrails.xnetgases.logic;

import mcjty.rftoolsbase.api.xnet.channels.Color;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.xnet.apiimpl.logic.Sensor;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.slurry.Slurry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import terrails.xnetgases.Utils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

public class XGSensor {

    public static final String TAG_MODE = Sensor.TAG_MODE;
    public static final String TAG_OPERATOR = Sensor.TAG_OPERATOR;
    public static final String TAG_AMOUNT = Sensor.TAG_AMOUNT;
    public static final String TAG_COLOR = Sensor.TAG_COLOR;
    public static final String TAG_STACK = Sensor.TAG_STACK;

    public enum SensorMode {
        OFF,
        GAS,
        SLURRY
    }

    // Custom Operator because the original uses Integer instead of Long
    public enum Operator {
        EQUAL("=", Long::equals),
        NOTEQUAL("!=", (i1, i2) -> !i1.equals(i2)),
        LESS("<", (i1, i2) -> i1 < i2),
        GREATER(">", (i1, i2) -> i1 > i2),
        LESSOREQUAL("<=", (i1, i2) -> i1 <= i2),
        GREATOROREQUAL(">=", (i1, i2) -> i1 >= i2);

        private final String code;
        private final BiPredicate<Long, Long> matcher;

        private static final Map<String, Operator> OPERATOR_MAP = new HashMap<>();

        static {
            for (Operator operator : values()) {
                OPERATOR_MAP.put(operator.code, operator);
            }
        }

        Operator(String code, BiPredicate<Long, Long> matcher) {
            this.code = code;
            this.matcher = matcher;
        }

        public String getCode() {
            return code;
        }

        public boolean match(long i1, long i2) {
            return matcher.test(i1, i2);
        }

        @Override
        public String toString() {
            return code;
        }

        public static Operator valueOfCode(String code) {
            return OPERATOR_MAP.get(code);
        }
    }

    private final int index;

    private SensorMode sensorMode = SensorMode.OFF;
    private Operator operator = Operator.EQUAL;
    private int amount = 0;
    private Color outputColor = Color.OFF;
    private ItemStack filter = ItemStack.EMPTY;

    public XGSensor(int index) {
        this.index = index;
    }

    public SensorMode getSensorMode() {
        return sensorMode;
    }

    public void setSensorMode(SensorMode sensorMode) {
        this.sensorMode = sensorMode;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public ItemStack getFilter() {
        return filter;
    }

    public void setFilter(ItemStack filter) {
        this.filter = filter;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Color getOutputColor() {
        return outputColor;
    }

    public void setOutputColor(Color outputColor) {
        this.outputColor = outputColor;
    }

    public boolean isEnabled(String tag) {
        if ((TAG_MODE + index).equals(tag)) {
            return true;
        }
        if ((TAG_OPERATOR + index).equals(tag)) {
            return true;
        }
        if ((TAG_AMOUNT + index).equals(tag)) {
            return true;
        }
        if ((TAG_COLOR + index).equals(tag)) {
            return true;
        }
        if ((TAG_STACK + index).equals(tag)) {
            return sensorMode != SensorMode.OFF;
        }
        return false;
    }

    public void createGui(IEditorGui gui) {
        gui
                .choices(TAG_MODE + index, "Sensor mode", sensorMode, SensorMode.values())
                .choices(TAG_OPERATOR + index, "Operator", operator, Operator.values())
                .integer(TAG_AMOUNT + index, "Amount to compare with", amount, 46)
                .colors(TAG_COLOR + index, "Output color", outputColor.getColor(), Color.COLORS)
                .ghostSlot(TAG_STACK + index, filter)
                .nl();
    }

    public boolean test(@Nullable TileEntity te, XGLogicConnectorSettings settings) {
        switch (sensorMode) {
            case GAS: return Utils.getGasHandlerFor(te, settings.getFacing())
                    .map(handler -> Utils.getGasHandlerFor(filter, null).map(filterHandler -> {
                        if (filterHandler.getTanks() <= 0) {
                            return false;
                        }

                        Gas filterChemical = filterHandler.getChemicalInTank(0).getType();
                        return operator.match(Utils.getGasCount(handler, filterChemical, settings.getFacing()), amount);
                    }).orElseGet(() -> filter.isEmpty() && operator.match(Utils.getGasCount(handler, (Gas) null, settings.getFacing()), amount)))
                    .orElse(false);
            case SLURRY: return Utils.getSlurryHandlerFor(te, settings.getFacing())
                    .map(handler -> Utils.getSlurryHandlerFor(filter, null).map(filterHandler -> {
                        if (filterHandler.getTanks() <= 0) {
                            return false;
                        }

                        Slurry filterChemical = filterHandler.getChemicalInTank(0).getType();
                        return operator.match(Utils.getSlurryCount(handler, filterChemical, settings.getFacing()), amount);
                    }).orElseGet(() -> filter.isEmpty() && operator.match(Utils.getSlurryCount(handler, (Slurry) null, settings.getFacing()), amount)))
                    .orElse(false);
        }
        return false;
    }

    public void update(Map<String, Object> data) {
        Object sm = data.get(TAG_MODE + index);
        if (sm != null) {
            sensorMode = SensorMode.valueOf(((String) sm).toUpperCase());
        } else sensorMode = SensorMode.OFF;

        Object op = data.get(TAG_OPERATOR + index);
        if (op != null) {
            operator = Operator.valueOfCode(((String) op).toUpperCase());
        } else operator = Operator.EQUAL;

        Object a = data.get(TAG_AMOUNT + index);
        if (a != null) {
            amount = (Integer) a;
        } else amount = 0;

        Object co = data.get(TAG_COLOR + index);
        if (co != null) {
            outputColor = Color.colorByValue((Integer) co);
        } else outputColor = Color.OFF;

        Object is = data.get(TAG_STACK + index);
        if (is != null) {
            this.filter = (ItemStack) is;
        } else {
            this.filter = ItemStack.EMPTY;
        }
    }

    public void readFromNBT(CompoundNBT tag) {
        sensorMode = SensorMode.values()[tag.getByte("sensorMode" + index)];
        operator = Operator.values()[tag.getByte("operator" + index)];
        amount = tag.getInt("amount" + index);
        outputColor = Color.values()[tag.getByte("scolor" + index)];
        if (tag.contains("filter" + index)) {
            CompoundNBT itemTag = tag.getCompound("filter" + index);
            filter = ItemStack.read(itemTag);
        } else {
            this.filter = ItemStack.EMPTY;
        }
    }

    public void writeToNBT(CompoundNBT tag) {
        tag.putByte("sensorMode" + index, (byte) sensorMode.ordinal());
        tag.putByte("operator" + index, (byte) operator.ordinal());
        tag.putInt("amount" + index, amount);
        tag.putByte("scolor" + index, (byte) outputColor.ordinal());
        if (!filter.isEmpty()) {
            CompoundNBT itemTag = new CompoundNBT();
            filter.write(itemTag);
            tag.put("filter" + index, itemTag);
        }
    }
}
