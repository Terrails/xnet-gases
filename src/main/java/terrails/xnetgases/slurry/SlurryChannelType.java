package terrails.xnetgases.slurry;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import terrails.xnetgases.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SlurryChannelType implements IChannelType {

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
        return Utils.getSlurryHandlerFor(world.getTileEntity(pos), direction).isPresent();
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
}
