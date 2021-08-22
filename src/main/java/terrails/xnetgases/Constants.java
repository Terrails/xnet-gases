package terrails.xnetgases;

import com.google.common.collect.ImmutableSet;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import mcjty.xnet.XNet;
import net.minecraft.util.ResourceLocation;

import java.util.Set;

public class Constants {

    public static final ResourceLocation XNET_GUI_ELEMENTS = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    public static final String TAG_MODE = "mode";
    public static final String TAG_RATE = "rate";
    public static final String TAG_MINMAX = "minmax";
    public static final String TAG_PRIORITY = "priority";
    public static final String TAG_FILTER = "filter";
    public static final String TAG_SPEED = "speed";
    public static final String TAG_REDSTONE = AbstractConnectorSettings.TAG_RS;
    public static final String TAG_COLOR = AbstractConnectorSettings.TAG_COLOR;

    public static final Set<String> INSERT_TAGS = ImmutableSet.of(TAG_MODE, TAG_REDSTONE, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_RATE, TAG_MINMAX, TAG_PRIORITY, TAG_FILTER);
    public static final Set<String> EXTRACT_TAGS = ImmutableSet.of(TAG_MODE, TAG_REDSTONE, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_RATE, TAG_MINMAX, TAG_PRIORITY, TAG_FILTER, TAG_SPEED);
}
