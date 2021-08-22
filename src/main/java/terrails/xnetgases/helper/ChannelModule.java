package terrails.xnetgases.helper;

import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import net.minecraftforge.common.ForgeConfigSpec;

public abstract class ChannelModule implements IChannelType {

    public void setupConfig(final ForgeConfigSpec.Builder builder) { }
}
