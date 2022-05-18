package terrails.xnetgases.module.chemical;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import terrails.xnetgases.module.chemical.utils.*;
import terrails.xnetgases.helper.BaseChannelModule;

public class ChemicalChannelModule extends BaseChannelModule {

    public static ForgeConfigSpec.IntValue maxGasRateNormal;
    public static ForgeConfigSpec.IntValue maxInfuseRateNormal;
    public static ForgeConfigSpec.IntValue maxPigmentRateNormal;
    public static ForgeConfigSpec.IntValue maxSlurryRateNormal;

    public static ForgeConfigSpec.IntValue maxGasRateAdvanced;
    public static ForgeConfigSpec.IntValue maxInfuseRateAdvanced;
    public static ForgeConfigSpec.IntValue maxPigmentRateAdvanced;
    public static ForgeConfigSpec.IntValue maxSlurryRateAdvanced;

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
        return ChemicalHelper.handlerPresent(level.getBlockEntity(pos), direction);
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
    public void setupConfig(ForgeConfigSpec.Builder builder) {
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
