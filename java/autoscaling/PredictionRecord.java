package autoscaling;

public class PredictionRecord {
    private final int slot;
    private final int vmId;
    private final double targetCpuFuture;
    private final int targetHighLoad;
    private final double predCpuFuture;
    private final int predHighLoad;

    public PredictionRecord(
            int slot,
            int vmId,
            double targetCpuFuture,
            int targetHighLoad,
            double predCpuFuture,
            int predHighLoad) {

        this.slot = slot;
        this.vmId = vmId;
        this.targetCpuFuture = targetCpuFuture;
        this.targetHighLoad = targetHighLoad;
        this.predCpuFuture = predCpuFuture;
        this.predHighLoad = predHighLoad;
    }

    public int getSlot() { return slot; }
    public int getVmId() { return vmId; }
    public double getTargetCpuFuture() { return targetCpuFuture; }
    public int getTargetHighLoad() { return targetHighLoad; }
    public double getPredCpuFuture() { return predCpuFuture; }
    public int getPredHighLoad() { return predHighLoad; }

    @Override
    public String toString() {
        return "slot=" + slot +
                ", vmId=" + vmId +
                ", predCpuFuture=" + predCpuFuture +
                ", predHighLoad=" + predHighLoad;
    }
}
