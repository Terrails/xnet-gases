package terrails.xnetgases.module.chemical;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import terrails.xnetgases.helper.BaseChannelModule;

public class ChemicalChannelModule extends BaseChannelModule {

    public static ModConfigSpec.IntValue maxGasRateNormal;
    public static ModConfigSpec.IntValue maxInfuseRateNormal;
    public static ModConfigSpec.IntValue maxPigmentRateNormal;
    public static ModConfigSpec.IntValue maxSlurryRateNormal;

    public static ModConfigSpec.IntValue maxGasRateAdvanced;
    public static ModConfigSpec.IntValue maxInfuseRateAdvanced;
    public static ModConfigSpec.IntValue maxPigmentRateAdvanced;
    public static ModConfigSpec.IntValue maxSlurryRateAdvanced;

    @Override
    public String getID() {
        return "mekanism.chemical";
    }

    @Override
    public String getName() {
        return "Mekanism Chemical";
    }

    @Override
    public boolean supportsBlock(@NotNull Level level, @NotNull BlockPos pos, @Nullable Direction direction) {
        return ChemicalHelper.isChemicalHandler(level, pos, direction);
    }

    @NotNull
    @Override
    public IConnectorSettings createConnector(@NotNull Direction direction) {
        return new ChemicalConnectorSettings(direction);
    }

    @NotNull
    @Override
    public IChannelSettings createChannel() {
        return new ChemicalChannelSettings();
    }

    @Override
    public void setupConfig(ModConfigSpec.Builder builder) {
        maxGasRateNormal = builder
                .comment("Maximum gas per operation that a normal connector can input or output")
                .defineInRange("maxGasRateNormal", 1000, 1, 1000000000);
        maxGasRateAdvanced = builder
                .comment("Maximum gas per operation that an advanced connector can input or output")
                .defineInRange("maxGasRateAdvanced", 5000, 1, 1000000000);

        maxInfuseRateNormal = builder
                .comment("Maximum infuse per operation that a normal connector can input or output")
                .defineInRange("maxInfuseRateNormal", 1000, 1, 1000000000);
        maxInfuseRateAdvanced = builder
                .comment("Maximum infuse per operation that an advanced connector can input or output")
                .defineInRange("maxInfuseRateAdvanced", 5000, 1, 1000000000);

        maxPigmentRateNormal = builder
                .comment("Maximum pigment per operation that a normal connector can input or output")
                .defineInRange("maxPigmentRateNormal", 1000, 1, 1000000000);
        maxPigmentRateAdvanced = builder
                .comment("Maximum pigment per operation that an advanced connector can input or output")
                .defineInRange("maxPigmentRateAdvanced", 5000, 1, 1000000000);

        maxSlurryRateNormal = builder
                .comment("Maximum slurry per operation that a normal connector can input or output")
                .defineInRange("maxSlurryRateNormal", 1000, 1, 1000000000);
        maxSlurryRateAdvanced = builder
                .comment("Maximum slurry per operation that an advanced connector can input or output")
                .defineInRange("maxSlurryRateAdvanced", 5000, 1, 1000000000);
    }
}
