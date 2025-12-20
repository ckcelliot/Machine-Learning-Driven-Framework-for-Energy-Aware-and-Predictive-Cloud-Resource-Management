package autoscaling;

/**
 * Simple autoscaling policy driven by predicted CPU usage.
 *
 * Logic:
 * - If predicted CPU > highThreshold  -> scale up by scaleStepUp
 * - If predicted CPU < lowThreshold   -> scale down by scaleStepDown
 * - Always keep VM count in [minVms, maxVms]
 */
public class AutoScalerPolicy {

    private final double highThreshold;
    private final double lowThreshold;
    private final int minVms;
    private final int maxVms;
    private final int scaleStepUp;
    private final int scaleStepDown;

    public AutoScalerPolicy(double highThreshold,
                            double lowThreshold,
                            int minVms,
                            int maxVms,
                            int scaleStepUp,
                            int scaleStepDown) {
        this.highThreshold = highThreshold;
        this.lowThreshold = lowThreshold;
        this.minVms = minVms;
        this.maxVms = maxVms;
        this.scaleStepUp = scaleStepUp;
        this.scaleStepDown = scaleStepDown;
    }

    /**
     * Decide the new VM count given the prediction and current number of VMs.
     */
    public int decideVmCount(PredictionRecord rec, int currentVmCount) {
        if (rec == null) {
            // No prediction? Keep current VM count.
            return currentVmCount;
        }

        double cpu = rec.getPredCpuFuture();
        int desired = currentVmCount;

        // Scale up
        if (cpu > highThreshold) {
            desired = currentVmCount + scaleStepUp;
        }
        // Scale down
        else if (cpu < lowThreshold && currentVmCount > minVms) {
            desired = currentVmCount - scaleStepDown;
        }

        // Clamp within [minVms, maxVms]
        if (desired < minVms) desired = minVms;
        if (desired > maxVms) desired = maxVms;

        return desired;
    }
}
