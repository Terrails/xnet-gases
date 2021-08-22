package terrails.xnetgases.module.logic;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import terrails.xnetgases.helper.ChannelModule;
import terrails.xnetgases.module.gas.GasUtils;
import terrails.xnetgases.module.infuse.InfuseUtils;
import terrails.xnetgases.module.pigment.PigmentUtils;
import terrails.xnetgases.module.slurry.SlurryUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class XGLogicChannelModule extends ChannelModule {

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
        TileEntity tile = world.getBlockEntity(pos);
        return GasUtils.getGasHandlerFor(tile, direction).isPresent()
                || SlurryUtils.getSlurryHandlerFor(tile, direction).isPresent()
                || InfuseUtils.getInfuseHandlerFor(tile, direction).isPresent()
                || PigmentUtils.getPigmentHandlerFor(tile, direction).isPresent();
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
