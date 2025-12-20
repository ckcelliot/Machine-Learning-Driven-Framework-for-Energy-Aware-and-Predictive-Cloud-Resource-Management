package cloudsim;

import autoscaling.AutoScalerPolicy;
import autoscaling.PredictionLoader;
import autoscaling.PredictionRecord;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;


public class MainSimulation {

    public static void main(String[] args) {
    System.out.println("=== ML-driven autoscaling with CloudSim Plus ===");

    // 1) Load ML predictions
    String csvPath = "/Users/azka/Downloads/Java/data/bitbrains_predictions_for_cloudsim.csv";
    PredictionLoader loader = new PredictionLoader(csvPath);

    // 2) Prepare CSV logger for results
    String resultsPath = "/Users/azka/Downloads/Java/exports/results_predictive.csv";
    try (PrintWriter pw = new PrintWriter(new FileWriter(resultsPath))) {

        // CSV header
        pw.println("time,slot,vm_id,pred_cpu,vm_count");

        // 3) Setup CloudSim simulation
        CloudSim simulation = new CloudSim();
        Datacenter datacenter = createDatacenter(simulation);
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);

        // 4) Initial VMs and cloudlets
        List<Vm> vmList = createVms(2);          // start with 2 VMs
        broker.submitVmList(vmList);

        List<Cloudlet> cloudletList = createCloudlets(20);
        broker.submitCloudletList(cloudletList);

        // 5) Autoscaling policy (simple threshold-based on predicted CPU)
        AutoScalerPolicy policy = new AutoScalerPolicy(
                80.0,  // highThreshold (%)
                20.0,  // lowThreshold (%)
                1,     // minVms
                10,    // maxVms
                1,     // scaleStepUp
                1      // scaleStepDown
        );

        // 6) Hook into simulation clock: every "time unit" = one slot
        simulation.addOnClockTickListener(evt -> {
            double time = evt.getTime();
            int slot = (int) time;     // basic mapping: 1 sim time unit == 1 slot
            int vmId = 1;              // for now: follow VM 1

            PredictionRecord rec = loader.get(vmId, slot);
            int currentVmCount = vmList.size();
            int newVmCount = policy.decideVmCount(rec, currentVmCount);

            double predCpu = (rec != null) ? rec.getPredCpuFuture() : -1.0;

            System.out.printf(
                    "t=%.0f slot=%d, vmId=%d, predCpu=%.2f, VMs: %d -> %d%n",
                    time, slot, vmId, predCpu, currentVmCount, newVmCount
            );

            // 🔹 Log to CSV (use US locale so decimal is with dot)
            pw.printf(Locale.US,
                    "%.2f,%d,%d,%.4f,%d%n",
                    time, slot, vmId, predCpu, currentVmCount
            );

            // Scale up
            if (newVmCount > currentVmCount) {
                int toAdd = newVmCount - currentVmCount;
                List<Vm> extraVms = createVms(toAdd);
                vmList.addAll(extraVms);
                broker.submitVmList(extraVms);
            }
            // Scale down (naive: destroy last VMs)
            else if (newVmCount < currentVmCount) {
                int toRemove = currentVmCount - newVmCount;
                for (int i = 0; i < toRemove && !vmList.isEmpty(); i++) {
                    Vm vm = vmList.remove(vmList.size() - 1);
                    if (vm.getHost() != null) {
                        vm.getHost().destroyVm(vm);
                    }
                }
            }
        });

        // 7) Run simulation
        simulation.start();
        System.out.println("=== Simulation finished ===");
        System.out.println("Results saved to: " + resultsPath);

    } catch (IOException e) {
        throw new RuntimeException("Failed to write results CSV: " + e.getMessage(), e);
    }
}


    // ------------ Helper methods to build CloudSim entities ------------

    private static Datacenter createDatacenter(CloudSim simulation) {
        List<Host> hostList = new ArrayList<>();

        int hosts = 2;
        long ram = 16384;          // MB
        long bw = 100000;          // bandwidth
        long storage = 1_000_000;  // storage
        int pesPerHost = 4;
        double mips = 1000;

        for (int i = 0; i < hosts; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < pesPerHost; j++) {
                peList.add(new PeSimple(mips));
            }

            Host host = new HostSimple(ram, bw, storage, peList);
            hostList.add(host);
        }

        return new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
    }

    private static List<Vm> createVms(int count) {
        List<Vm> vmList = new ArrayList<>();

        long size = 10000;  // image size (MB)
        int ram = 2048;     // vm memory (MB)
        long bw = 1000;     // bandwidth
        int pes = 2;        // number of CPU cores
        double mips = 1000;

        for (int i = 0; i < count; i++) {
            Vm vm = new VmSimple(mips, pes);
            vm.setRam(ram).setBw(bw).setSize(size);
            vmList.add(vm);
        }

        return vmList;
    }

    private static List<Cloudlet> createCloudlets(int count) {
        List<Cloudlet> list = new ArrayList<>();

        long length = 10000;
        int pes = 2;

        for (int i = 0; i < count; i++) {
            Cloudlet cloudlet = new CloudletSimple(length, pes);
            cloudlet.setUtilizationModelCpu(new UtilizationModelFull());
            list.add(cloudlet);
        }

        return list;
    }
}
