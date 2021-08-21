package terrails.xnetgases.infuse;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InfuseChannelType implements IChannelType {

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
}
