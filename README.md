# CPU and Disk Scheduling Simulator

A standalone Java Swing desktop application built for **ICS26012 – Operating Systems** (1st Semester, AY 2026–2027) that simulates two CPU scheduling algorithms and two disk scheduling algorithms.

* **Group:** Group 4 – Ulysses (Section 3ITB)
* **Repository:** `<paste your GitHub repository link here>`

## Group Members & Assigned Algorithms

| Member | Algorithm | Coordination Role |
| :--- | :--- | :--- |
| Justin Rei M. Aviles | C-LOOK | Documentation Lead |
| Airuz Arnold D. Balungay | C-SCAN | Leader |
| Carlo Magnus I. Cuison | First-Come First-Served (FCFS) | Video Lead |
| Johann Henreich R. Gomez | Priority (Preemptive) | Testing Lead |

---

## Requirements

* **Java Development Kit (JDK) 17 or later** (uses standard Java Swing and `java.awt` only; no external libraries, database, or internet connection required).

Check your Java version:
```bash
java -version
```

---

## How to Set Up and Run

### Option 1: Run the Executable JAR (Recommended)
Double-click `uly.jar` or run it from a terminal:
```bash
java -jar uly.jar
```

### Option 2: Compile and Run from Source
From the project root folder:

**Windows (PowerShell / Command Prompt):**
```powershell
mkdir out
javac --release 17 -d out src\Main.java src\cpu\*.java src\disk\*.java src\ui\*.java
java -cp out Main
```

**macOS / Linux:**
```bash
mkdir -p out
javac --release 17 -d out src/Main.java src/cpu/*.java src/disk/*.java src/ui/*.java
java -cp out Main
```

### Building the Executable JAR
After compiling into `out/`:
```bash
jar cfe uly.jar Main -C out .
```

---

## How to Use the Application

1. **Main Menu & Navigation Bar**
   * On launch, the **Home / Menu** screen lists the four algorithms grouped into **CPU Scheduling** (`FCFS`, `Preemptive Priority`) and **Disk Scheduling** (`C-SCAN`, `C-LOOK`).
   * Use either the menu buttons or the top navigation bar to switch between screens at any time.
   * Click **About** to view group details, or **Exit** to close the app.

2. **CPU Scheduling (`FCFS` and `Preemptive Priority`)**
   * Each screen loads with the 10-process practice problem set by default.
   * Edit cells directly in the table (`PID`, `Arrival`, `Burst`, and `Priority` for Preemptive Priority — lower number means higher priority).
   * Use **Add Process** or **Remove Selected Row** to change the number of processes.
   * Click **Run** to compute the per-process table (`CT`, `TAT`, `WT`, `RT`), averages, CPU Utilization, Throughput, and draw the **Gantt Chart**.
   * Click **Load Sample Data** to restore the practice set, **Reset / Clear** to start with blank rows, or **? Info** to view the algorithm rules and formulas.

3. **Disk Scheduling (`C-SCAN` and `C-LOOK`)**
   * Each screen loads with the 20-request practice problem set (`N = 200`, Initial Head `92`, Direction `Right`).
   * Enter cylinder requests separated by commas or spaces, set the **Initial Head Position**, **Track/Disk Size N** (cylinders `0` to `N - 1`), and choose **Initial Direction** (`Right` or `Left`).
   * Click **Run** to display the seek sequence, Total Head Movement, Average Seek Distance, step-by-step calculations, and the **Head Movement Graph** (blue = normal seek, red dashed = circular return jump, green dot = start).
   * Click **Load Sample Data**, **Reset / Clear**, or **? Info** as needed.

---

## Project Structure

```text
src/
├── Main.java                      # Entry point, window, navbar, and Home menu
├── cpu/
│   ├── CpuScheduler.java          # Shared CPU interface and Process/GanttBlock/CpuResult models
│   ├── FcfsCpu.java               # First-Come First-Served (non-preemptive)
│   └── PreemptivePriorityCpu.java # Preemptive Priority (lower number = higher priority)
├── disk/
│   ├── DiskScheduler.java         # Shared Disk interface and DiskResult model
│   ├── CScanDisk.java             # Circular SCAN
│   └── CLookDisk.java             # Circular LOOK
└── ui/
    ├── CpuPanel.java              # CPU input table, output report, and Gantt chart panel
    └── DiskPanel.java             # Disk input form, output report, and Head Movement graph panel
```
