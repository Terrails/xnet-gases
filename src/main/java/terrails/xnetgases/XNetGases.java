package terrails.xnetgases;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import mcjty.rftoolsbase.api.xnet.channels.IConnectable;
import mcjty.xnet.XNet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import terrails.xnetgases.module.chemical.ChemicalChannelModule;
import terrails.xnetgases.helper.BaseChannelModule;
import terrails.xnetgases.module.chemical.ChemicalHelper;
import terrails.xnetgases.module.logic.ChemicalLogicChannelModule;

import java.util.Arrays;

@Mod(XNetGases.MOD_ID)
public class XNetGases {

    public static final String MOD_ID = "xnetgases";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final ModConfigSpec CONFIG_SPEC;

    private static final BaseChannelModule[] MODULES = {
            new ChemicalLogicChannelModule(),
            new ChemicalChannelModule()
    };

    public XNetGases(final IEventBus bus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CONFIG_SPEC, "xnetgases.toml");
        bus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        Arrays.stream(MODULES).forEach(XNet.xNetApi::registerChannelType);
        XNet.xNetApi.registerConnectable(((blockGetter, connectorPos, blockPos, blockEntity, direction) -> {
            if (ChemicalHelper.handlerPresent(blockEntity, direction)) {
                return IConnectable.ConnectResult.YES;
            } else return IConnectable.ConnectResult.DEFAULT;
        }));
    }

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("General settings").push("general");
        Arrays.stream(MODULES).forEach(module -> module.setupConfig(builder));
        CONFIG_SPEC = builder.pop().build();
    }
}
