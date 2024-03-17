package terrails.xnetgases.module.logic;

import mekanism.api.chemical.ChemicalType;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ChemicalLogicEnums {

    public enum ConnectorMode {
        SENSOR, OUTPUT;

        private static final Map<String, ConnectorMode> NAME_MAP = Arrays.stream(ConnectorMode.values()).collect(Collectors.toMap(Enum::name, Function.identity()));

        public static ConnectorMode byName(String name) {
            return NAME_MAP.get(name.toUpperCase(Locale.ROOT));
        }
    }

    public enum SensorMode {
        OFF, GAS, SLURRY, INFUSION, PIGMENT;

        private static final Map<String, SensorMode> NAME_MAP = Arrays.stream(SensorMode.values()).collect(Collectors.toMap(Enum::name, Function.identity()));

        public static SensorMode byName(String name) {
            return NAME_MAP.get(name);
        }

        public ChemicalType toType() {
            return switch (this) {
                case OFF -> null;
                case GAS -> ChemicalType.GAS;
                case SLURRY -> ChemicalType.SLURRY;
                case INFUSION -> ChemicalType.INFUSION;
                case PIGMENT -> ChemicalType.PIGMENT;
            };
        }
    }

    // Custom Operator because the original uses integer instead of long.
    // Creative tanks use Long#MAX_VALUE which results in an overflow.
    public enum SensorOperator {
        EQUAL("=", Long::equals),
        NOTEQUAL("!=", (i1, i2) -> !i1.equals(i2)),
        LESS("<", (i1, i2) -> i1 < i2),
        GREATER(">", (i1, i2) -> i1 > i2),
        LESSOREQUAL("<=", (i1, i2) -> i1 <= i2),
        GREATEROREQUAL(">=", (i1, i2) -> i1 >= i2);

        private final String code;
        private final BiPredicate<Long, Long> matcher;

        private static final Map<String, SensorOperator> OPERATOR_MAP = Arrays.stream(SensorOperator.values()).collect(Collectors.toMap(op -> op.code, Function.identity()));
        private static final Map<String, SensorOperator> NAME_MAP = Arrays.stream(SensorOperator.values()).collect(Collectors.toMap(Enum::name, Function.identity()));

        SensorOperator(String code, BiPredicate<Long, Long> matcher) {
            this.code = code;
            this.matcher = matcher;
        }

        public static SensorOperator byCode(String code) {
            return OPERATOR_MAP.get(code);
        }

        public static SensorOperator byName(String name) {
            return NAME_MAP.get(name);
        }

        public boolean match(long i1, long i2) {
            return matcher.test(i1, i2);
        }

        @Override
        public String toString() {
            return code;
        }
    }

}
