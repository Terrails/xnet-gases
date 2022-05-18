package terrails.xnetgases.helper;

import com.google.gson.JsonObject;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import net.minecraft.core.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseConnectorSettings<Z> extends AbstractConnectorSettings {

    public BaseConnectorSettings(@Nonnull Direction side) {
        super(side);
    }

    @Nullable
    @Override
    public abstract IndicatorIcon getIndicatorIcon();

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Nullable
    public abstract Z getMatcher();

    @Override
    public abstract boolean isEnabled(String s);

    @Override
    public void createGui(IEditorGui iEditorGui) { }

    @Override
    public abstract JsonObject writeToJson();

    @Override
    public abstract void readFromJson(JsonObject data);
}
