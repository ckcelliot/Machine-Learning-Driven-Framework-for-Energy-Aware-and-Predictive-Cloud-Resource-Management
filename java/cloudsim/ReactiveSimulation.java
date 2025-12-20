package cloudsim;

import autoscaling.AutoScalerPolicy;
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

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reactive autoscaling:
 * - NO ML file
 * - Decisions are based on CURRENT average VM CPU in CloudSim.
 */
public class ReactiveSimulation {

    private static final String OUT_PATH =
            "/Users/azka/Downloads/Java/exports/results_reactive.csv";

    public static void main(String[] args) {
        System.out.println("=== Reactive autoscaling with CloudSim Plus ===");

        CloudSim simulation = new CloudSim();
        Datacenter datacenter = createDatacenter(simulation);
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);

        // Start with 2 VMs, similar to predictive/static
        List<Vm> vmList = createVms(2);
        broker.submitVmList(vmList);

        // Same 20 cloudlets
        List<Cloudlet> cloudletList = createCloudlets(20);
        broker.submitCloudletList(cloudletList);

        // Same thresholds as predictive policy
        AutoScalerPolicy policy = new AutoScalerPolicy(
                80.0,   // highThreshold (%)
                20.0,   // lowThreshold (%)
                1,      // minVms
                10,     // maxVms
                1,      // scaleStepUp
                1       // scaleStepDown
        );

        try (PrintWriter pw = new PrintWriter(new FileWriter(OUT_PATH))) {
            pw.println("time,slot,avg_cpu_percent,vm_count");

            // Every clock tick, look at CURRENT VM CPU and react
            simulation.addOnClockTickListener(evt -> {
                double time = evt.getTime();
                int slot = (int) time;

                // CloudSim gives utilization as [0,1], convert to %
                double avgCpuPercent = vmList.stream()
                        .mapToDouble(vm -> vm.getCpuPercentUtilization() * 100.0)
                        .average()
                        .orElse(0.0);

                int currentVmCount = vmList.size();

                // Reuse AutoScalerPolicy by building a fake PredictionRecord
                PredictionRecord rec = new PredictionRecord(
                        slot,
                        -1,                      // vmId not important here
                        avgCpuPercent,
                        avgCpuPercent > 80.0 ? 1 : 0,
                        avgCpuPercent,
                        avgCpuPercent > 80.0 ? 1 : 0
                );

                int newVmCount = policy.decideVmCount(rec, currentVmCount);

                System.out.printf(
                        "t=%.2f slot=%d, avgCpu=%.2f%%, VMs: %d -> %d%n",
                        time, slot, avgCpuPercent, currentVmCount, newVmCount
                );

                // Log BEFORE scaling, just like predictive/static
                pw.printf("%.2f,%d,%.4f,%d%n",
                        time, slot, avgCpuPercent, currentVmCount);
                pw.flush();

                // Scale up
                if (newVmCount > currentVmCount) {
                    int toAdd = newVmCount - currentVmCount;
                    List<Vm> extraVms = createVms(toAdd);
                    vmList.addAll(extraVms);
                    broker.submitVmList(extraVms);
                }
                // Scale down (destroy last VMs)
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

            simulation.start();
            System.out.println("=== Reactive Simulation finished ===");
            System.out.println("Reactive results saved to: " + OUT_PATH);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------ Helper methods (same as MainSimulation) ------------

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
