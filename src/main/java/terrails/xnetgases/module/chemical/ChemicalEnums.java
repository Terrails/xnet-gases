package terrails.xnetgases.module.chemical;


import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ChemicalEnums {

    public enum Type {
        GAS, INFUSE, PIGMENT, SLURRY;

        public static final Map<String, Type> NAME_MAP = Arrays.stream(Type.values()).collect(Collectors.toMap(Enum::name, Function.identity()));

        public static Type byName(String name) {
            return NAME_MAP.get(name.toUpperCase(Locale.ROOT));
        }
    }

}
