package cloudsim;

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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StaticSimulation {

    public static void main(String[] args) {
        System.out.println("=== Static baseline (no ML, no autoscaling) ===");

        String resultsPath = "/Users/azka/Downloads/Java/exports/results_static.csv";

        try (PrintWriter pw = new PrintWriter(new FileWriter(resultsPath))) {
            // Keep same header as predictive for easy comparison
            pw.println("time,slot,vm_id,pred_cpu,vm_count");

            // 1) CloudSim setup
            CloudSim simulation = new CloudSim();
            Datacenter datacenter = createDatacenter(simulation);
            DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);

            // 2) Fixed number of VMs (for example: 2)
            List<Vm> vmList = createVms(2);
            broker.submitVmList(vmList);

            // 3) Same cloudlets as predictive run
            List<Cloudlet> cloudletList = createCloudlets(20);
            broker.submitCloudletList(cloudletList);

            // 4) Log on each clock tick (no scaling, just VM count)
            simulation.addOnClockTickListener(evt -> {
                double time = evt.getTime();
                int slot = (int) time;
                int vmId = 1;  // just for logging consistency
                double predCpu = -1.0;  // no prediction in static baseline
                int vmCount = vmList.size();

                System.out.printf(
                        "t=%.2f slot=%d, vmId=%d, predCpu=%.2f, VMs=%d%n",
                        time, slot, vmId, predCpu, vmCount
                );

                pw.printf(Locale.US,
                        "%.2f,%d,%d,%.4f,%d%n",
                        time, slot, vmId, predCpu, vmCount
                );
            });

            // 5) Run simulation
            simulation.start();
            System.out.println("=== Static Simulation finished ===");
            System.out.println("Static results saved to: " + resultsPath);

        } catch (IOException e) {
            throw new RuntimeException("Failed to write static results CSV: " + e.getMessage(), e);
        }
    }

    // ---- Same helper methods as MainSimulation (copy-pasted) ----

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
