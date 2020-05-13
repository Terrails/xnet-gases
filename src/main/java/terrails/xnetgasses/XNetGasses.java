package terrails.xnetgasses;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import mcjty.xnet.XNet;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrails.xnetgasses.gas.GasChannelType;
import terrails.xnetgasses.gas.GasConnectable;

import java.nio.file.Path;

@Mod(XNetGasses.MOD_ID)
public class XNetGasses {

    public static final String MOD_ID = "xnetgasses";
    public static final Logger LOGGER = LogManager.getLogger();

    public static ForgeConfigSpec.IntValue maxGasRateNormal;
    public static ForgeConfigSpec.IntValue maxGasRateAdvanced;

    private static final ForgeConfigSpec CONFIG_SPEC;

    public XNetGasses() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC, "xnetgasses.toml");
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        loadConfig(FMLPaths.CONFIGDIR.get().resolve("xnetgasses.toml"));

        XNet.xNetApi.registerChannelType(new GasChannelType());
        XNet.xNetApi.registerConnectable(new GasConnectable());
    }

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("General settings").push("general");

        maxGasRateNormal = builder
                .comment("Maximum gas per operation that a normal connector can input or output")
                .defineInRange("maxGasRateNormal", 1000, 1, 1000000000);
        maxGasRateAdvanced = builder
                .comment("Maximum gas per operation that an advanced connector can input or output")
                .defineInRange("maxGasRateAdvanced", 5000, 1, 1000000000);

        CONFIG_SPEC = builder.pop().build();
    }

    private static void loadConfig(Path path) {
        XNetGasses.LOGGER.debug("Loading config file {}", path);

        final CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();

        XNetGasses.LOGGER.debug("Built TOML config for {}", path.toString());
        configData.load();
        XNetGasses.LOGGER.debug("Loaded TOML config for {}", path.toString());
        CONFIG_SPEC.setConfig(configData);
    }
}
