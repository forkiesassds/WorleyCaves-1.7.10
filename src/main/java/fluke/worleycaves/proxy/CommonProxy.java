package fluke.worleycaves.proxy;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import fluke.worleycaves.event.CaveEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.TERRAIN_GEN_BUS.register(new CaveEvent());
    }
}
