package terrails.xnetgases;

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
import terrails.xnetgases.gas.GasChannelType;
import terrails.xnetgases.gas.GasConnectable;
import terrails.xnetgases.logic.XGLogicChannelType;
import terrails.xnetgases.slurry.SlurryChannelType;
import terrails.xnetgases.slurry.SlurryConnectable;

import java.nio.file.Path;

@Mod(XNetGases.MOD_ID)
public class XNetGases {

    public static final String MOD_ID = "xnetgases";
    public static final Logger LOGGER = LogManager.getLogger();

    public static ForgeConfigSpec.IntValue maxGasRateNormal;
    public static ForgeConfigSpec.IntValue maxGasRateAdvanced;

    public static ForgeConfigSpec.IntValue maxSlurryRateNormal;
    public static ForgeConfigSpec.IntValue maxSlurryRateAdvanced;

    private static final ForgeConfigSpec CONFIG_SPEC;

    public XNetGases() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC, "xnetgases.toml");
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        loadConfig(FMLPaths.CONFIGDIR.get().resolve("xnetgases.toml"));

        XNet.xNetApi.registerChannelType(new GasChannelType());
        XNet.xNetApi.registerConnectable(new GasConnectable());

        XNet.xNetApi.registerChannelType(new SlurryChannelType());
        XNet.xNetApi.registerConnectable(new SlurryConnectable());

        XNet.xNetApi.registerChannelType(new XGLogicChannelType());
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

        maxSlurryRateNormal = builder
                .comment("Maximum slurry per operation that a normal connector can input or output")
                .defineInRange("maxSlurryRateNormal", 1000, 1, 1000000000);
        maxSlurryRateAdvanced = builder
                .comment("Maximum slurry per operation that an advanced connector can input or output")
                .defineInRange("maxSlurryRateAdvanced", 5000, 1, 1000000000);

        CONFIG_SPEC = builder.pop().build();
    }

    private static void loadConfig(Path path) {
        XNetGases.LOGGER.debug("Loading config file {}", path);

        final CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();

        XNetGases.LOGGER.debug("Built TOML config for {}", path.toString());
        configData.load();
        XNetGases.LOGGER.debug("Loaded TOML config for {}", path.toString());
        CONFIG_SPEC.setConfig(configData);
    }
}
