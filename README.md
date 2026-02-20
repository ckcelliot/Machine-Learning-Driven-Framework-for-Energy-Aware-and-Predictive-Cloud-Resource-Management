# Energy-Aware Predictive Autoscaling for Cloud Data Centres  
### CloudSim Plus Simulation with Machine Learning Workload Prediction

This project implements and evaluates **energy-aware autoscaling** in a simulated cloud data centre using **CloudSim Plus** and **machine learning-based CPU predictions**.

The workflow consists of:

1. Preprocessing real workload traces (Bitbrains dataset)
2. Training an ML model to predict future CPU usage
3. Feeding predictions into a CloudSim Plus simulation
4. Applying different autoscaling policies
5. Exporting results for comparison and analysis

The system compares three scaling strategies:

- **Static Policy** – Fixed number of VMs (no scaling)
- **Reactive Policy** – Threshold-based scaling using current conditions
- **Predictive Policy** – Threshold-based scaling using ML-predicted future CPU usage

Simulation results are exported as CSV files and analyzed using Jupyter notebooks to generate metrics and plots for evaluation.

## Key Concepts

This section explains the core components of the predictive autoscaling system and how they interact during simulation. It describes how machine learning predictions are integrated into CloudSim Plus and how scaling decisions are made.

### Simulation Slot

In this project, one CloudSim time unit corresponds to one workload slot.

Each slot represents a discrete time step for which CPU usage predictions are available.

During simulation, the current simulation time is converted into a slot index:

int slot = (int) time;

This mapping ensures that, at every simulation step, the autoscaler retrieves the correct machine-learning prediction corresponding to that time slot.

### Prediction CSV File

The predictive autoscaler relies on the following file generated during the ML phase:

data/bitbrains_predictions_for_cloudsim.csv

This CSV contains future CPU usage predictions aligned with simulation slots.

Each row includes:

- **slot** – time index used by the simulator

- **vm_id** – identifier of the VM

- **target_cpu_future** – actual future CPU usage

- **target_high_load** – actual future high-load label

- **pred_cpu_future** – predicted future CPU usage

- **pred_high_load** – predicted future high-load label

The file is loaded by the PredictionLoader class, which:

1. reads the CSV file

2. stores predictions in memory

3. allows fast lookup using (vm_id, slot)

Example usage inside the simulation:

   PredictionRecord rec = loader.get(vmId, slot);

If no prediction exists for a given slot, the autoscaling policy keeps the current number of virtual machines unchanged.

## Autoscaling Policy

The predictive autoscaling logic is implemented in the AutoScalerPolicy class.

The policy applies simple threshold-based scaling rules driven by predicted CPU usage, rather than current utilization.

Scaling decisions follow these rules:

1. if the predicted CPU usage is above the high threshold, the system scales up

2. if the predicted CPU usage is below the low threshold, the system scales down

3. the number of VMs is always constrained within predefined minimum and maximum limits

An example configuration used in the simulation:

AutoScalerPolicy policy = new AutoScalerPolicy(
    80.0,  // highThreshold (%)
    20.0,  // lowThreshold (%)
    1,     // minVms
    10,    // maxVms
    1,     // scaleStepUp
    1      // scaleStepDown
);

This policy ensures controlled scaling while preventing over-provisioning or under-provisioning beyond configured limits.

## Simulation Variants

The project evaluates three autoscaling strategies:

1. Static Policy – uses a fixed number of VMs throughout the simulation

2. Reactive Policy – scales VMs based on current utilization thresholds

3. Predictive Policy – scales VMs based on ML-predicted future CPU usage

Each strategy is implemented as a separate CloudSim simulation entry point.

### Output Files

Each simulation writes its results to the exports/ directory.

The generated CSV files include:

1. results_static.csv

2. results_reactive.csv

3. results_predictive.csv

These files record time-step-level information such as simulation time, slot index, predicted CPU values (for predictive policy), and the number of active VMs. They are later analyzed using Jupyter notebooks to compute metrics and generate plots for evaluation.

## Repository Structure

The project is organized into Java simulation code, machine learning notebooks, input data, and exported results.

```

.
├── data/
│ ├── fastStorage/2013-8/ # Raw Bitbrains workload traces
│ ├── bitbrains_clean_all.csv # Cleaned dataset
│ ├── bitbrains_ml_windows.csv # Feature-engineered windows
│ ├── bitbrains_regression.csv # Regression dataset
│ ├── bitbrains_classification.csv # Classification dataset
│ └── bitbrains_predictions_for_cloudsim.csv # ML predictions used by simulation
│
├── java/
│ ├── autoscaling/
│ │ ├── AutoScalerPolicy.java
│ │ ├── PredictionLoader.java
│ │ ├── PredictionRecord.java
│ │ └── TestLoader.java
│ │
│ └── cloudsim/
│ ├── MainSimulation.java # Predictive policy
│ ├── ReactiveSimulation.java # Reactive policy
│ └── StaticSimulation.java # Static policy
│
├── lib/ # CloudSim Plus and dependency JARs
│
├── exports/
│ ├── results_predictive.csv
│ ├── results_reactive.csv
│ └── results_static.csv
│
├── notebooks/
│ ├── 1_data_preprocessing.ipynb
│ ├── 2_feature_engineering.ipynb
│ ├── 03_ml_training.ipynb
│ ├── 03_cloudsim_analysis.ipynb
│ └── 04_metrics_and_plots.ipynb
│
└── out/ # Compiled Java output

```

Folder Overview

1. data/ – Contains raw workload traces, processed datasets, and ML prediction files.

2. java/autoscaling/ – Contains autoscaling logic and prediction loading utilities.

3. java/cloudsim/ – Contains CloudSim Plus simulation entry points.

4. lib/ – Contains required CloudSim Plus and dependency JAR files.

5. exports/ – Stores simulation output CSV files.

6. notebooks/ – Jupyter notebooks for data preprocessing, ML training, and result analysis.

7. out/ – Compiled Java classes.


## Requirements and Setup

This project requires a Java environment for the CloudSim simulation and a Python environment for data preprocessing, machine learning, and result analysis.

1. Java Requirements (CloudSim Simulation)

- JDK 11 or higher

- CloudSim Plus JAR files (already included in the lib/ directory)

- An IDE such as VS Code or IntelliJ IDEA (optional)

Verify Java installation:

- java -version

No Maven or Gradle setup is required since all dependencies are provided in lib/.

2. Python Requirements (ML + Analysis)

- Python 3.8+

- Jupyter Notebook or Jupyter Lab

- Required libraries:

  - pandas

  - numpy

  - scikit-learn

  - matplotlib

Optional virtual environment setup:

- python -m venv .venv
- source .venv/bin/activate   # macOS/Linux
-  .venv\Scripts\activate    # Windows

Install required libraries manually or using a requirements.txt file if available.

## Running the Project

The workflow consists of two phases:

1. Generate ML predictions

2. Run CloudSim simulations

### Step 1 – Generate ML Predictions

Open and execute the notebooks in order:

1. 1_data_preprocessing.ipynb

2. 2_feature_engineering.ipynb

3. 03_ml_training.ipynb

After training, ensure the following file is created:

- data/bitbrains_predictions_for_cloudsim.csv

This file is required for the predictive simulation.

### Step 2 – Run CloudSim Simulations

Open the java/ project in your IDE.

You can run the following main classes:

- Predictive Policy

   - cloudsim.MainSimulation

- Reactive Policy

   - cloudsim.ReactiveSimulation

- Static Policy

   - cloudsim.StaticSimulation

Each simulation will generate output files in:

exports/

Generated files include:

- results_predictive.csv

- results_reactive.csv

- results_static.csv

## Important: Path Configuration

The current Java files use absolute file paths (e.g., /Users/...).
Before running the project, update these paths in the simulation classes to match your local directory structure, or convert them to relative paths.

### Step 3 – Analyze Results

Open:

1. 03_cloudsim_analysis.ipynb

2. 04_metrics_and_plots.ipynb

Use these notebooks to:

1. Compare VM scaling behavior

2. Visualize scaling stability

3. Analyze performance differences across policies

4. ## Notes

This project was developed for academic research and experimental evaluation of predictive autoscaling strategies in simulated cloud environments using CloudSim Plus.





