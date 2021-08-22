package terrails.xnetgases.helper;

import com.google.gson.JsonObject;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.DefaultChannelSettings;
import net.minecraft.nbt.CompoundNBT;

import javax.annotation.Nullable;
import java.util.Map;

public abstract class ChemicalChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    @Override
    public abstract void readFromNBT(CompoundNBT compoundNBT);

    @Override
    public abstract void writeToNBT(CompoundNBT compoundNBT);

    @Override
    public abstract JsonObject writeToJson();

    @Override
    public abstract void readFromJson(JsonObject data);

    @Override
    public abstract void tick(int i, IControllerContext iControllerContext);

    @Override
    public abstract void cleanCache();

    @Nullable
    @Override
    public abstract IndicatorIcon getIndicatorIcon();

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public boolean isEnabled(String s) {
        return true;
    }

    @Override
    public void createGui(IEditorGui iEditorGui) { }

    @Override
    public void update(Map<String, Object> map) { }

    @Override
    public int getColors() {
        return 0;
    }
}
