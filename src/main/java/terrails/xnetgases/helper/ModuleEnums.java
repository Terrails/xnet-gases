package terrails.xnetgases.helper;


import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModuleEnums {

    public enum ChannelMode {
        PRIORITY, DISTRIBUTE;

        private static final Map<String, ChannelMode> NAME_MAP = Arrays.stream(ChannelMode.values()).collect(Collectors.toMap(Enum::name, Function.identity()));

        public static ChannelMode byName(String name) {
            return NAME_MAP.get(name.toUpperCase(Locale.ROOT));
        }
    }

    public enum ConnectorMode {
        INS, EXT;

        private static final Map<String, ConnectorMode> NAME_MAP = Arrays.stream(ConnectorMode.values()).collect(Collectors.toMap(Enum::name, Function.identity()));

        public static ConnectorMode byName(String name) {
            return NAME_MAP.get(name.toUpperCase(Locale.ROOT));
        }
    }
}
