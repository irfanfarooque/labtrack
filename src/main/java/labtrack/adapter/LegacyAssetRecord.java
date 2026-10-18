package labtrack.adapter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
public class LegacyAssetRecord {
    private final Map<String, String> values = new HashMap<>();
    public LegacyAssetRecord(Map<String, String> rawValues) {
        rawValues.forEach((k, v) -> values.put(k.trim().toUpperCase(Locale.ROOT), v == null ? "" : v.trim()));
    }
/** Case-insensitive lookup; missing fields come back as empty text. */
    public String get(String key) {
        return values.getOrDefault(key.trim().toUpperCase(Locale.ROOT), "");
    }
}
