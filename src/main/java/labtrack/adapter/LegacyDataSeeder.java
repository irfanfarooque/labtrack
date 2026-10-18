package labtrack.adapter;
import labtrack.model.Category;
import labtrack.model.ComputerEquipment;
import labtrack.model.Equipment;
import labtrack.service.AppContext;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
public final class LegacyDataSeeder {
    private LegacyDataSeeder() {}
    public static void seedIfEmpty(AppContext ctx) {
        try {
            if (!ctx.equipment().list(null, 0, null, null, true).isEmpty()) return;
            try (InputStream in = LegacyDataSeeder.class.getResourceAsStream("/legacy_seed.tsv")) {
                if (in == null) return;
                List<Equipment> items = new LegacyTsvAdapter()
                        .readAll(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (Equipment item : items) {
                    Category category = ctx.equipment().categoryByName(item.getCategory().getName());
                    if (category == null) category = ctx.equipment().addCategory(item.getCategory().getName());
                    item.setCategory(category);
                    ctx.equipment().importEquipment(item);
                }
            }
        } catch (Exception e) {
            System.err.println("Legacy seed skipped: " + e.getMessage());
        }
    }
}
