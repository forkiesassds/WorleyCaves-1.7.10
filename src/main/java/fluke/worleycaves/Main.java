package fluke.worleycaves;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import fluke.worleycaves.config.Configs;
import fluke.worleycaves.proxy.CommonProxy;
import fluke.worleycaves.util.Reference;

@Mod(modid = Reference.MOD_ID, name = Reference.NAME, version = Tags.VERSION, acceptedMinecraftVersions = "[1.7.10]")
public class Main {

    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    @Instance
    public static Main instance;

    @SidedProxy(clientSide = Reference.COMMON_PROXY_CLASS, serverSide = Reference.COMMON_PROXY_CLASS)
    public static CommonProxy proxy;

    @EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
        Configs.config = new Configuration(event.getSuggestedConfigurationFile());
        Configs.refreshConfig();

        proxy.preInit(event);
    }
}
