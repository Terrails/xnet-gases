package terrails.xnetgases.module.chemical;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.JSonTools;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import terrails.xnetgases.helper.BaseConnectorSettings;
import terrails.xnetgases.helper.ModuleEnums.*;
import terrails.xnetgases.module.chemical.utils.ChemicalHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static terrails.xnetgases.Constants.*;

public class ChemicalConnectorSettings extends BaseConnectorSettings<ChemicalStack<?>> {
    
    private ConnectorMode connectorMode = ConnectorMode.INS;
    private ChemicalEnums.Type connectorType = ChemicalEnums.Type.GAS;

    @Nullable private Integer priority = 0;
    @Nullable private Integer transferRate = null;
    @Nullable private Integer minMaxLimit = null;

    private int operationSpeed = 20;
    private boolean transferRateRequired = false;

    private ItemStack filter = ItemStack.EMPTY;

    public ChemicalConnectorSettings(@Nonnull Direction side) {
        super(side);
    }

    public ConnectorMode getConnectorMode() {
        return this.connectorMode;
    }

    public ChemicalEnums.Type getConnectorType() {
        return this.connectorType;
    }

    public int getOperationSpeed() {
        return this.operationSpeed;
    }

    public int getPriority() {
        return this.priority == null ? 0 : priority;
    }

    public boolean isTransferRateRequired() {
        return this.transferRateRequired;
    }

    public int getRate() {
        // 'advanced' is always false here, so default to non-advanced speed
        return Objects.requireNonNullElse(this.transferRate, getMaxRate(false));
    }

    private int getMaxRate(boolean advanced) {
        return switch (connectorType) {
            case GAS -> advanced ? ChemicalChannelModule.maxGasRateAdvanced.get() : ChemicalChannelModule.maxGasRateNormal.get();
            case INFUSE -> advanced ? ChemicalChannelModule.maxInfuseRateAdvanced.get() : ChemicalChannelModule.maxInfuseRateNormal.get();
            case PIGMENT -> advanced ? ChemicalChannelModule.maxPigmentRateAdvanced.get() : ChemicalChannelModule.maxPigmentRateNormal.get();
            case SLURRY -> advanced ? ChemicalChannelModule.maxSlurryRateAdvanced.get() : ChemicalChannelModule.maxSlurryRateNormal.get();
        };
    }

    @Nullable public Integer getMinMaxLimit() {
        return this.minMaxLimit;
    }

    @Nullable
    @Override
    public ChemicalStack<?> getMatcher() {
        if (!filter.isEmpty()) {
            switch (connectorType) {
                case GAS:
                    if (Capabilities.GAS_HANDLER_CAPABILITY != null && filter.getCapability(Capabilities.GAS_HANDLER_CAPABILITY).isPresent()) {
                        IGasHandler handler = filter.getCapability(Capabilities.GAS_HANDLER_CAPABILITY).orElseThrow(() -> new IllegalArgumentException("IGasHandler Capability doesn't exist!"));
                        return handler.getChemicalInTank(0);
                    }
                    break;
                case INFUSE:
                    if (Capabilities.INFUSION_HANDLER_CAPABILITY != null && filter.getCapability(Capabilities.INFUSION_HANDLER_CAPABILITY).isPresent()) {
                        IInfusionHandler handler = filter.getCapability(Capabilities.INFUSION_HANDLER_CAPABILITY).orElseThrow(() -> new IllegalArgumentException("IInfusionHandler Capability doesn't exist!"));
                        return handler.getChemicalInTank(0);
                    }
                    break;
                case PIGMENT:
                    if (Capabilities.PIGMENT_HANDLER_CAPABILITY != null && filter.getCapability(Capabilities.PIGMENT_HANDLER_CAPABILITY).isPresent()) {
                        IPigmentHandler handler = filter.getCapability(Capabilities.PIGMENT_HANDLER_CAPABILITY).orElseThrow(() -> new IllegalArgumentException("IPigmentHandler Capability doesn't exist!"));
                        return handler.getChemicalInTank(0);
                    }
                    break;
                case SLURRY:
                    if (Capabilities.SLURRY_HANDLER_CAPABILITY != null && filter.getCapability(Capabilities.SLURRY_HANDLER_CAPABILITY).isPresent()) {
                        ISlurryHandler handler = filter.getCapability(Capabilities.SLURRY_HANDLER_CAPABILITY).orElseThrow(() -> new IllegalArgumentException("ISlurryHandler Capability doesn't exist!"));
                        return handler.getChemicalInTank(0);
                    }
                    break;
            }
        }
        return null;
    }

    @Override
    public boolean isEnabled(String tag) {
        if (connectorMode == ConnectorMode.INS) {
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

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        this.connectorMode = ConnectorMode.byName(((String) data.get(TAG_MODE)));
        this.connectorType = ChemicalEnums.Type.NAME_MAP.get(((String) data.get(TAG_TYPE)).toUpperCase(Locale.ROOT));
        this.transferRate = (Integer) data.get(TAG_RATE);
        this.minMaxLimit = (Integer) data.get(TAG_MIN_MAX);

        if (data.containsKey(TAG_REQUIRE_RATE)) {
            this.transferRateRequired = (boolean) data.get(TAG_REQUIRE_RATE);
        } else this.transferRateRequired = false;

        if (data.containsKey(TAG_PRIORITY)) {
            this.priority = (Integer) data.get(TAG_PRIORITY);
        } else this.priority = null;

        if (data.containsKey(TAG_SPEED)) {
            this.operationSpeed = Integer.parseInt((String) data.get(TAG_SPEED));
            if (this.operationSpeed == 0) this.operationSpeed = 20;
        } else this.operationSpeed = 20;

        this.filter = ChemicalHelper.normalizeStack((ItemStack) data.get(TAG_FILTER), this.connectorType);
    }

    @Override
    public void createGui(IEditorGui gui) {
        this.advanced = gui.isAdvanced();
        int maxTransferRate = getMaxRate(this.advanced);

        sideGui(gui);
        colorsGui(gui);
        redstoneGui(gui);
        gui
                .nl()
                .choices(TAG_MODE, "Insert or extract mode", this.connectorMode, ConnectorMode.values())
                .choices(TAG_TYPE, "Connector type", this.connectorType, ChemicalEnums.Type.values());


        if (this.connectorMode == ConnectorMode.INS) {
            gui
                    .label("Pri").integer(TAG_PRIORITY, "Insertion priority", this.priority, 36)
                    .shift(5)
                    .toggle(TAG_REQUIRE_RATE, "Require insert rate", this.transferRateRequired)
                    .nl();
        } else {
            gui
                    .choices(TAG_SPEED, "Number of ticks for each operation", Integer.toString(this.operationSpeed), this.advanced ? CONNECTOR_SPEEDS[0] : CONNECTOR_SPEEDS[1])
                    .nl();
        }

        gui
                .label("Rate").integer(TAG_RATE, this.connectorMode == ConnectorMode.EXT ? "Extraction rate|(max " + maxTransferRate + "mb)" : "Insertion rate|(max " + maxTransferRate + "mb)", this.transferRate, 60, maxTransferRate)
                .label(this.connectorMode == ConnectorMode.EXT ? "Min" : "Max")
                .integer(TAG_MIN_MAX, this.connectorMode == ConnectorMode.EXT ? "Keep this amount in tank" : "Disable insertion if|amount is too high", this.minMaxLimit, 48)
                .nl()
                .label("Filter")
                .ghostSlot(TAG_FILTER, filter);
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        this.transferRateRequired = tag.getBoolean(TAG_REQUIRE_RATE);
        this.connectorMode = ConnectorMode.values()[tag.getByte(TAG_MODE)];
        this.connectorType = ChemicalEnums.Type.values()[tag.getByte(TAG_TYPE)];

        if (tag.contains(TAG_PRIORITY)) {
            this.priority = tag.getInt(TAG_PRIORITY);
        } else this.priority = null;

        if (tag.contains(TAG_RATE)) {
            this.transferRate = tag.getInt(TAG_RATE);
        } else this.transferRate = null;

        if (tag.contains(TAG_MIN_MAX)) {
            this.minMaxLimit = tag.getInt(TAG_MIN_MAX);
        } else this.minMaxLimit = null;

        this.operationSpeed = tag.getInt(TAG_SPEED);
        if (this.operationSpeed == 0) this.operationSpeed = 20;

        if (tag.contains(TAG_FILTER)) {
            CompoundTag itemTag = tag.getCompound(TAG_FILTER);
            this.filter = ChemicalHelper.normalizeStack(ItemStack.of(itemTag), this.connectorType);
        } else this.filter = ItemStack.EMPTY;
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putBoolean(TAG_REQUIRE_RATE, this.transferRateRequired);
        tag.putByte(TAG_MODE, (byte) this.connectorMode.ordinal());
        tag.putByte(TAG_TYPE, (byte) this.connectorType.ordinal());
        tag.putInt(TAG_SPEED, this.operationSpeed);

        if (this.priority != null) tag.putInt(TAG_PRIORITY, this.priority);
        if (this.transferRate != null) tag.putInt(TAG_RATE, this.transferRate);
        if (this.minMaxLimit != null) tag.putInt(TAG_MIN_MAX, this.minMaxLimit);

        if (!this.filter.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            this.filter.save(itemTag);
            tag.put(TAG_FILTER, itemTag);
        }
    }

    @Override
    public void readFromJson(JsonObject data) {
        super.readFromJsonInternal(data);
        this.connectorMode = getEnumSafe(data, TAG_MODE, ConnectorMode::byName);
        this.connectorType = getEnumSafe(data, TAG_TYPE, ChemicalEnums.Type.NAME_MAP::get);
        this.priority = getIntegerSafe(data, TAG_PRIORITY);
        this.transferRate = getIntegerSafe(data, TAG_RATE);
        this.transferRateRequired = getBoolSafe(data, TAG_REQUIRE_RATE);
        this.minMaxLimit = getIntegerSafe(data, TAG_MIN_MAX);
        this.operationSpeed = getIntegerNotNull(data, TAG_SPEED);
        if (this.operationSpeed == 0) this.operationSpeed = 20;
        if (data.has(TAG_FILTER)) {
            this.filter = ChemicalHelper.normalizeStack(JSonTools.jsonToItemStack(data.get(TAG_FILTER).getAsJsonObject()), this.connectorType);
        } else this.filter = ItemStack.EMPTY;
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject data = new JsonObject();
        super.writeToJsonInternal(data);
        data.add(TAG_REQUIRE_RATE, new JsonPrimitive(this.transferRateRequired));
        setEnumSafe(data, TAG_MODE, this.connectorMode);
        setEnumSafe(data, TAG_TYPE, this.connectorType);
        setIntegerSafe(data, TAG_PRIORITY, this.priority);
        setIntegerSafe(data, TAG_RATE, this.transferRate);
        setIntegerSafe(data, TAG_MIN_MAX, this.minMaxLimit);
        setIntegerSafe(data, TAG_SPEED, this.operationSpeed);

        if (!this.filter.isEmpty()) {
            data.add(TAG_FILTER, JSonTools.itemStackToJson(this.filter));
        }
        if (this.operationSpeed == 10 || (this.transferRate != null && this.transferRate > this.getMaxRate(false))) {
            data.add("advancedneeded", new JsonPrimitive(true));
        }
        return data;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return switch (this.connectorMode) {
            case INS -> new IndicatorIcon(XNET_GUI_ELEMENTS, 0, 70, 13, 10);
            case EXT -> new IndicatorIcon(XNET_GUI_ELEMENTS, 13, 70, 13, 10);
        };
    }
}
