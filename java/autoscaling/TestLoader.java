package autoscaling;

public class TestLoader {
    public static void main(String[] args) {
        String csv = "/Users/azka/Downloads/Java/data/bitbrains_predictions_for_cloudsim.csv";

        PredictionLoader loader = new PredictionLoader(csv);

        // Test: get prediction for VM 1, slot 0
        PredictionRecord r = loader.get(1, 0);

        System.out.println("Record = " + r);
    }
}
