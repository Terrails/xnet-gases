package terrails.xnetgases.helper;

import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import net.neoforged.neoforge.common.ModConfigSpec;

public abstract class BaseChannelModule implements IChannelType {

    public void setupConfig(final ModConfigSpec.Builder builder) { }
}
