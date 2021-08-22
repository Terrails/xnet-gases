package terrails.xnetgases.module.infuse;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfigSpec;
import terrails.xnetgases.helper.ChannelModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InfuseChannelModule extends ChannelModule {

    public static ForgeConfigSpec.IntValue maxInfuseRateNormal;
    public static ForgeConfigSpec.IntValue maxInfuseRateAdvanced;

    @Override
    public String getID() {
        return "mekanism.infuse";
    }

    @Override
    public String getName() {
        return "Mekanism Infuse";
    }

    @Override
    public boolean supportsBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nullable Direction direction) {
        return InfuseUtils.getInfuseHandlerFor(world.getBlockEntity(pos), direction).isPresent();
    }

    @Nonnull
    @Override
    public IConnectorSettings createConnector(@Nonnull Direction direction) {
        return new InfuseConnectorSettings(direction);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new InfuseChannelSettings();
    }

    @Override
    public void setupConfig(ForgeConfigSpec.Builder builder) {
        maxInfuseRateNormal = builder
                .comment("Maximum infuse per operation that a normal connector can input or output")
                .defineInRange("maxInfuseRateNormal", 1000, 1, 1000000000);
        maxInfuseRateAdvanced = builder
                .comment("Maximum infuse per operation that an advanced connector can input or output")
                .defineInRange("maxInfuseRateAdvanced", 5000, 1, 1000000000);
    }
}
