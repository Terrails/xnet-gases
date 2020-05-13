package terrails.xnetgasses.gas;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import terrails.xnetgasses.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GasChannelType implements IChannelType {

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
        return Utils.getGasHandlerFor(world.getTileEntity(pos), direction).isPresent();
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
}
