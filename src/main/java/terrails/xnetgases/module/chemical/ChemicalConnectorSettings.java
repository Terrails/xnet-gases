package terrails.xnetgases.module.chemical;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.lib.varia.CompositeStreamCodec;
import mcjty.lib.varia.JSonTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import mcjty.xnet.apiimpl.Constants;

import terrails.xnetgases.XNetGases;
import terrails.xnetgases.module.ChemicalMatcher;
import terrails.xnetgases.module.chemical.enums.ConnectorMode;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static mcjty.xnet.apiimpl.Constants.*;
import static mcjty.xnet.utils.I18nConstants.*;
import static terrails.xnetgases.Constants.*;
import static terrails.xnetgases.I18nConstants.*;

public class ChemicalConnectorSettings extends AbstractConnectorSettings {

    public static final MapCodec<ChemicalConnectorSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BaseSettings.CODEC.fieldOf("base").forGetter(settings -> settings.settings),
            Direction.CODEC.fieldOf("side").forGetter(ChemicalConnectorSettings::getSide),
            ConnectorMode.CODEC.fieldOf("mode").forGetter(ChemicalConnectorSettings::getConnectorMode),
            Codec.INT.optionalFieldOf("priority").forGetter(o -> Optional.ofNullable(o.priority)),
            Codec.INT.optionalFieldOf("rate").forGetter(o -> Optional.ofNullable(o.transferRate)),
            Codec.INT.optionalFieldOf("minmax").forGetter(o -> Optional.ofNullable(o.minMaxLimit)),
            Codec.INT.fieldOf("speed").forGetter(ChemicalConnectorSettings::getOperationSpeed),
            Codec.BOOL.fieldOf("rate_required").forGetter(ChemicalConnectorSettings::isTransferRateRequired),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("filter", ItemStack.EMPTY).forGetter(o -> o.matcher.getStack())
    ).apply(instance, ChemicalConnectorSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChemicalConnectorSettings> STREAM_CODEC = CompositeStreamCodec.composite(
            BaseSettings.STREAM_CODEC, s -> s.settings,
            Direction.STREAM_CODEC, ChemicalConnectorSettings::getSide,
            ConnectorMode.STREAM_CODEC, ChemicalConnectorSettings::getConnectorMode,
            ByteBufCodecs.optional(ByteBufCodecs.INT), s -> Optional.ofNullable(s.priority),
            ByteBufCodecs.optional(ByteBufCodecs.INT), s -> Optional.ofNullable(s.transferRate),
            ByteBufCodecs.optional(ByteBufCodecs.INT), s -> Optional.ofNullable(s.minMaxLimit),
            ByteBufCodecs.INT, ChemicalConnectorSettings::getOperationSpeed,
            ByteBufCodecs.BOOL, ChemicalConnectorSettings::isTransferRateRequired,
            ItemStack.OPTIONAL_STREAM_CODEC, s -> s.matcher.getStack(),
            ChemicalConnectorSettings::new
    );

    @Nullable
    private Integer priority = null, transferRate = null, minMaxLimit = null;
    private int operationSpeed = 20;
    private boolean transferRateRequired = false;
    private ConnectorMode connectorMode = ConnectorMode.INS;
    private ChemicalMatcher matcher = ChemicalMatcher.EMPTY;

    public ChemicalConnectorSettings(@Nonnull Direction side) {
        super(DEFAULT_SETTINGS, side);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ChemicalConnectorSettings(
            @Nonnull BaseSettings base, @Nonnull Direction side, ConnectorMode mode,
            Optional<Integer> priority, Optional<Integer> rate, Optional<Integer> minMaxLimit,
            int speed, boolean transferRateRequired, ItemStack filter) {
        super(base, side);
        this.connectorMode = mode;
        this.priority = priority.orElse(null);
        this.transferRate = rate.orElse(null);
        this.minMaxLimit = minMaxLimit.orElse(null);
        this.operationSpeed = speed;
        this.transferRateRequired = transferRateRequired;
        this.matcher = ChemicalMatcher.from(filter);
    }

    public ConnectorMode getConnectorMode() {
        return connectorMode;
    }

    public int getOperationSpeed() {
        return operationSpeed;
    }

    public int getPriority() {
        return priority == null ? 0 : priority;
    }

    @Nullable
    public Integer getMinMaxLimit() {
        return minMaxLimit;
    }

    public Integer getTransferRate() {
        // 'advanced' is always false here, so default to non-advanced speed
        return Optional.ofNullable(transferRate).orElseGet(XNetGases.maxRateNormal);
    }

    public boolean isTransferRateRequired() {
        return transferRateRequired;
    }

    public ChemicalMatcher getMatcher() {
        return matcher;
    }

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);

        connectorMode = Optional.ofNullable(data.get(TAG_CHEMICAL_MODE))
                .map(o -> ConnectorMode.values()[(int) o])
                .orElse(ConnectorMode.INS);

        transferRate = Optional.ofNullable(data.get(TAG_RATE))
                .map(Integer.class::cast)
                .flatMap(i -> i == -1 ? Optional.empty() : Optional.of(i))
                .orElse(null);

        transferRateRequired = Optional.ofNullable(data.get(TAG_REQUIRE_RATE))
                .map(Boolean.class::cast)
                .orElse(false);

        minMaxLimit = Optional.ofNullable(data.get(TAG_MINMAX))
                .map(Integer.class::cast)
                .flatMap(i -> i == -1 ? Optional.empty() : Optional.of(i))
                .orElse(null);

        priority = Optional.ofNullable(data.get(TAG_PRIORITY))
                .map(Integer.class::cast)
                .filter(i -> i != 0)
                .orElse(0);

        operationSpeed = Optional.ofNullable(data.get(TAG_SPEED))
                .map(String.class::cast)
                .map(Integer::parseInt)
                .filter(i -> i != 0)
                .orElse(20);

        matcher = Optional.ofNullable(data.get(TAG_FILTER))
                .map(ItemStack.class::cast)
                .map(ChemicalMatcher::from)
                .orElse(ChemicalMatcher.EMPTY);
    }

    @Override
    public void createGui(IEditorGui gui) {
        advanced = gui.isAdvanced();
        int maxTransferRate = (advanced ? XNetGases.maxRateAdvanced : XNetGases.maxRateNormal).get();
        String[] speeds = Arrays.stream(advanced ? Constants.ADVANCED_SPEEDS : Constants.SPEEDS)
                .map(s -> String.valueOf(Integer.parseInt(s) * 2))
                .toArray(String[]::new);

        sideGui(gui);
        colorsGui(gui);
        redstoneGui(gui);
        gui.nl();
        gui.translatableChoices(TAG_CHEMICAL_MODE, connectorMode, ConnectorMode.values());
        if (connectorMode == ConnectorMode.INS) {
            gui.label(PRIORITY_LABEL.i18n()).integer(TAG_PRIORITY, PRIORITY_TOOLTIP.i18n(), priority, 36);
        } else {
            gui.choices(TAG_SPEED, SPEED_TOOLTIP.i18n(), Integer.toString(operationSpeed), speeds);
        }
        gui.nl();
        gui.label(RATE_LABEL.i18n()).integer(TAG_RATE, getRateTooltip(), transferRate, 60, maxTransferRate, -1);
        if (connectorMode == ConnectorMode.INS) {
            gui.shift(5);
            gui.toggle(TAG_REQUIRE_RATE, REQUIRE_INSERT_RATE_LABEL.i18n(), transferRateRequired);
        }
        gui.nl();
        gui.label((connectorMode == ConnectorMode.EXT ? MIN : MAX).i18n()).integer(TAG_MINMAX, getMinMaxTooltip(), minMaxLimit, 48, Integer.MAX_VALUE, -1);
        gui.nl();
        gui.label(FILTER_LABEL.i18n()).ghostSlot(TAG_FILTER, matcher.getStack());
    }

    @Override
    public boolean isEnabled(String tag) {
        if (tag.equals(TAG_FACING)) {
            return advanced;
        }

        if (connectorMode == ConnectorMode.INS) {
            return CHEMICAL_INSERT_FEATURES.contains(tag);
        } else {
            return CHEMICAL_EXTRACT_FEATURES.contains(tag);
        }
    }

    @Override
    public IChannelType getType() {
        return ChemicalChannelType.TYPE;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return switch (getConnectorMode()) {
            case INS -> new IndicatorIcon(XNET_GUI_ELEMENTS, 0, 70, 13, 10);
            case EXT -> new IndicatorIcon(XNET_GUI_ELEMENTS, 13, 70, 13, 10);
        };
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject data = new JsonObject();
        super.writeToJsonInternal(data);
        data.add(TAG_REQUIRE_RATE, new JsonPrimitive(transferRateRequired));
        setEnumSafe(data, TAG_CHEMICAL_MODE, connectorMode);
        setIntegerSafe(data, TAG_PRIORITY, priority);
        setIntegerSafe(data, TAG_RATE, transferRate);
        setIntegerSafe(data, TAG_MINMAX, minMaxLimit);
        setIntegerSafe(data, TAG_SPEED, operationSpeed);

        if (operationSpeed == 10 || (transferRate != null && transferRate > XNetGases.maxRateNormal.get())) {
            data.add(TAG_ADVANCED_NEEDED, new JsonPrimitive(true));
        }

        if (!matcher.isEmpty()) {
            data.add(TAG_FILTER, JSonTools.itemStackToJson(matcher.getStack()));
        }
        return data;
    }

    @Override
    public void readFromJson(JsonObject data) {
        super.readFromJsonInternal(data);
        connectorMode = getEnumSafe(data, TAG_CHEMICAL_MODE, ConnectorMode::byName);
        priority = getIntegerSafe(data, TAG_PRIORITY);
        transferRate = getIntegerSafe(data, TAG_RATE);
        transferRateRequired = getBoolSafe(data, TAG_REQUIRE_RATE);
        minMaxLimit = getIntegerSafe(data, TAG_MINMAX);

        operationSpeed = getIntegerNotNull(data, TAG_SPEED);
        if (operationSpeed == 0) operationSpeed = 20;

        matcher = Optional.ofNullable(data.get(TAG_FILTER))
                .map(JsonElement::getAsJsonObject)
                .map(JSonTools::jsonToItemStack)
                .map(ChemicalMatcher::from)
                .orElse(ChemicalMatcher.EMPTY);
    }

    private String getRateTooltip() {
        return CHEMICAL_RATE_TOOLTIP_FORMATTED.i18n(
                (getConnectorMode() == ConnectorMode.EXT ? EXT_ENDING : INS_ENDING).i18n(),
                (advanced ? XNetGases.maxRateAdvanced : XNetGases.maxRateNormal).get()
        );
    }

    private String getMinMaxTooltip() {
        return CHEMICAL_MINMAX_TOOLTIP_FORMATTED.i18n(
                (getConnectorMode() == ConnectorMode.EXT ? EXT_ENDING : INS_ENDING).i18n(),
                (getConnectorMode() == ConnectorMode.EXT ? LOW_FORMAT : HIGH_FORMAT).i18n());
    }
}
