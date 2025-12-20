package autoscaling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class PredictionLoader {

    // vmId -> (slot -> record)
    private final Map<Integer, Map<Integer, PredictionRecord>> table = new HashMap<>();

    public PredictionLoader(String csvPath) {
        loadCsv(csvPath);
    }

    private void loadCsv(String path) {
        System.out.println("Loading prediction CSV: " + path);

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            // Read header
            String line = br.readLine();
            if (line == null) {
                throw new RuntimeException("CSV is empty: " + path);
            }

            // Read rows
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                int slot = Integer.parseInt(parts[0]);
                int vmId = Integer.parseInt(parts[1]);
                double targetCpuFuture = Double.parseDouble(parts[2]);
                int targetHighLoad = Integer.parseInt(parts[3]);
                double predCpuFuture = Double.parseDouble(parts[4]);
                int predHighLoad = Integer.parseInt(parts[5]);

                PredictionRecord rec = new PredictionRecord(
                        slot, vmId, targetCpuFuture, targetHighLoad, predCpuFuture, predHighLoad
                );

                table.computeIfAbsent(vmId, k -> new HashMap<>()).put(slot, rec);
            }

            System.out.println("Loaded predictions for VMs: " + table.size());

        } catch (Exception e) {
            throw new RuntimeException("Failed to load CSV: " + e.getMessage(), e);
        }
    }

    /** Get prediction for (vmId, slot). Returns null if missing. */
    public PredictionRecord get(int vmId, int slot) {
        Map<Integer, PredictionRecord> vmMap = table.get(vmId);
        return (vmMap == null) ? null : vmMap.get(slot);
    }
}
