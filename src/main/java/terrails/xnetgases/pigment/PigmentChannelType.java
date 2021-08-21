package terrails.xnetgases.pigment;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PigmentChannelType implements IChannelType {

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
}
