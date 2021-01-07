package terrails.xnetgases.logic;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import terrails.xnetgases.gas.GasUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class XGLogicChannelType implements IChannelType {

    @Override
    public String getID() {
        return "mekanism.logic";
    }

    @Override
    public String getName() {
        return "Mekanism Logic";
    }

    @Override
    public boolean supportsBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nullable Direction direction) {
        TileEntity tile = world.getTileEntity(pos);
        return GasUtils.getGasHandlerFor(tile, direction).isPresent();
    }

    @Nonnull
    @Override
    public IConnectorSettings createConnector(@Nonnull Direction direction) {
        return new XGLogicConnectorSettings(direction);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new XGLogicChannelSettings();
    }
}
