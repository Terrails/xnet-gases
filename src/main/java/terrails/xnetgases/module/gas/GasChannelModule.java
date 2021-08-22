package terrails.xnetgases.module.gas;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfigSpec;
import terrails.xnetgases.helper.ChannelModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GasChannelModule extends ChannelModule {

    public static ForgeConfigSpec.IntValue maxGasRateNormal;
    public static ForgeConfigSpec.IntValue maxGasRateAdvanced;

    @Override
    public String getID() {
        return "mekanism.gas";
    }

    @Override
    public String getName() {
        return "Mekanism Gas";
    }

    @Override
    public boolean supportsBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nullable Direction direction) {
        return GasUtils.getGasHandlerFor(world.getBlockEntity(pos), direction).isPresent();
    }

    @Nonnull
    @Override
    public IConnectorSettings createConnector(@Nonnull Direction direction) {
        return new GasConnectorSettings(direction);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new GasChannelSettings();
    }

    @Override
    public void setupConfig(ForgeConfigSpec.Builder builder) {
        maxGasRateNormal = builder
                .comment("Maximum gas per operation that a normal connector can input or output")
                .defineInRange("maxGasRateNormal", 1000, 1, 1000000000);
        maxGasRateAdvanced = builder
                .comment("Maximum gas per operation that an advanced connector can input or output")
                .defineInRange("maxGasRateAdvanced", 5000, 1, 1000000000);
    }
}
