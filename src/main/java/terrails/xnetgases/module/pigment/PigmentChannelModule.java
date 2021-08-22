package terrails.xnetgases.module.pigment;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfigSpec;
import terrails.xnetgases.helper.ChannelModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PigmentChannelModule extends ChannelModule {

    public static ForgeConfigSpec.IntValue maxPigmentRateNormal;
    public static ForgeConfigSpec.IntValue maxPigmentRateAdvanced;

    @Override
    public String getID() {
        return "mekanism.pigment";
    }

    @Override
    public String getName() {
        return "Mekanism Pigment";
    }

    @Override
    public boolean supportsBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nullable Direction direction) {
        return PigmentUtils.getPigmentHandlerFor(world.getBlockEntity(pos), direction).isPresent();
    }

    @Nonnull
    @Override
    public IConnectorSettings createConnector(@Nonnull Direction direction) {
        return new PigmentConnectorSettings(direction);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new PigmentChannelSettings();
    }

    @Override
    public void setupConfig(ForgeConfigSpec.Builder builder) {
        maxPigmentRateNormal = builder
                .comment("Maximum pigment per operation that a normal connector can input or output")
                .defineInRange("maxPigmentRateNormal", 1000, 1, 1000000000);
        maxPigmentRateAdvanced = builder
                .comment("Maximum pigment per operation that an advanced connector can input or output")
                .defineInRange("maxPigmentRateAdvanced", 5000, 1, 1000000000);
    }
}
