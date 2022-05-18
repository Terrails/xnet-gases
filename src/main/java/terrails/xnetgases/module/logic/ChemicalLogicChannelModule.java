package terrails.xnetgases.module.logic;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import terrails.xnetgases.helper.BaseChannelModule;
import terrails.xnetgases.module.chemical.utils.ChemicalHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ChemicalLogicChannelModule extends BaseChannelModule {

    @Override
    public String getID() {
        return "mekanism.logic";
    }

    @Override
    public String getName() {
        return "Mekanism Logic";
    }

    @Override
    public boolean supportsBlock(@Nonnull Level level, @Nonnull BlockPos pos, @Nullable Direction direction) {
        return ChemicalHelper.handlerPresent(level.getBlockEntity(pos), direction);
    }

    @Nonnull
    @Override
    public IConnectorSettings createConnector(@Nonnull Direction direction) {
        return new ChemicalLogicConnectorSettings(direction);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new ChemicalLogicChannelSettings();
    }
}
