package labtrack.adapter;
import labtrack.exception.LabTrackException;
import labtrack.model.Equipment;
public interface EquipmentAdapter {
    Equipment adapt(LegacyAssetRecord record) throws LabTrackException;
}
