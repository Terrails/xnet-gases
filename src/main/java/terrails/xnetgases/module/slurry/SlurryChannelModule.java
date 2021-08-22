package terrails.xnetgases.module.slurry;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfigSpec;
import terrails.xnetgases.helper.ChannelModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SlurryChannelModule extends ChannelModule {

    public static ForgeConfigSpec.IntValue maxSlurryRateNormal;
    public static ForgeConfigSpec.IntValue maxSlurryRateAdvanced;

    @Override
    public String getID() {
        return "mekanism.slurry";
    }

    @Override
    public String getName() {
        return "Mekanism Slurry";
    }

    @Override
    public boolean supportsBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nullable Direction direction) {
        return SlurryUtils.getSlurryHandlerFor(world.getBlockEntity(pos), direction).isPresent();
    }

    @Nonnull
    @Override
    public IConnectorSettings createConnector(@Nonnull Direction direction) {
        return new SlurryConnectorSettings(direction);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new SlurryChannelSettings();
    }

    @Override
    public void setupConfig(ForgeConfigSpec.Builder builder) {
        maxSlurryRateNormal = builder
                .comment("Maximum slurry per operation that a normal connector can input or output")
                .defineInRange("maxSlurryRateNormal", 1000, 1, 1000000000);
        maxSlurryRateAdvanced = builder
                .comment("Maximum slurry per operation that an advanced connector can input or output")
                .defineInRange("maxSlurryRateAdvanced", 5000, 1, 1000000000);
    }
}
