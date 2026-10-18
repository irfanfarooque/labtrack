package labtrack.adapter;
import labtrack.exception.LabTrackException;
import labtrack.exception.ValidationException;
import labtrack.model.Category;
import labtrack.model.ComputerEquipment;
import labtrack.model.Condition;
import labtrack.model.ElectronicEquipment;
import labtrack.model.Equipment;
import labtrack.model.EquipmentStatus;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/** Adapter: converts the legacy TSV export (asset register) into current Equipment objects. */
public class LegacyTsvAdapter implements EquipmentAdapter {
    private static final String[] COLUMNS =
            {"ASTID", "DESC", "CLASS", "CATEGORY", "MAKE", "SERIAL", "LOCTYPE", "PURDATE", "COND", "NOTES"};
    @Override
    public Equipment adapt(LegacyAssetRecord r) throws LabTrackException {
        String code = require(r, "ASTID", "Asset tag"), name = require(r, "DESC", "Description");
        Category category = new Category(0, require(r, "CATEGORY", "Category"));
        String rawDate = r.get("PURDATE");
        LocalDate purchase = null;
        if (!rawDate.isEmpty()) {
            try { purchase = LocalDate.parse(rawDate); }
            catch (java.time.format.DateTimeParseException e) { throw new ValidationException("Asset " + code + ": bad purchase date '" + rawDate + "'."); }
        }
        Condition condition;
        try { condition = Condition.from(r.get("COND")); }
        catch (IllegalArgumentException e) { throw new ValidationException("Asset " + code + ": " + e.getMessage()); }
        return "COMPUTER".equalsIgnoreCase(r.get("CLASS"))
                ? new ComputerEquipment(0, code, name, category, r.get("SERIAL"), r.get("MAKE"), r.get("LOCTYPE"), purchase, condition, EquipmentStatus.AVAILABLE, r.get("NOTES"))
                : new ElectronicEquipment(0, code, name, category, r.get("SERIAL"), r.get("MAKE"), r.get("LOCTYPE"), purchase, condition, EquipmentStatus.AVAILABLE, r.get("NOTES"));
    }
    /** Reads a whole TSV export (first line = header, # lines are comments). */
    public List<Equipment> readAll(Reader reader) throws IOException, LabTrackException {
        List<Equipment> out = new ArrayList<>();
        BufferedReader br = new BufferedReader(reader);
        String line;
        int lineNo = 0;
        while ((line = br.readLine()) != null) {
            lineNo++;
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] parts = line.split("\t", -1);
            if (parts.length < 2 || parts[0].trim().equalsIgnoreCase("ASTID")) continue;
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < COLUMNS.length && i < parts.length; i++) values.put(COLUMNS[i], parts[i]);
            try { out.add(adapt(new LegacyAssetRecord(values))); }
            catch (LabTrackException e) { throw new ValidationException("Legacy import, line " + lineNo + ": " + e.getMessage()); }
        }
        return out;
    }
    private static String require(LegacyAssetRecord r, String key, String label) throws ValidationException {
        String v = r.get(key);
        if (v.isEmpty()) throw new ValidationException("Missing legacy field " + key + " (" + label + ").");
        return v;
    }
}
