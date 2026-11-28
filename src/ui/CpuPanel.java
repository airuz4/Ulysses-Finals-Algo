package ui;

import cpu.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

// Screen for the two CPU algorithms (FCFS and Preemptive Priority).
// The same panel class is used for both; the Priority column is shown only
// when the algorithm is Preemptive Priority.
public final class CpuPanel extends JPanel {

    // Safety limit so a huge number cannot freeze the program or the Gantt chart.
    private static final int MAX_TIME_VALUE = 10000;

    private static final String[] COLUMN_NAMES = {"PID", "Arrival", "Burst", "Priority"};

    private String algorithmName;
    private boolean usesPriority;

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextArea outputArea;
    private GanttPanel ganttPanel;

    public CpuPanel(String algorithmName) {
        this.algorithmName = algorithmName;
        this.usesPriority = algorithmName.equals("Preemptive Priority");
        setLayout(new BorderLayout());

        // ----- Input table (all cells are stored as text, so any typing is accepted) -----
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0);
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        // Center-align every cell so values line up with the column headers.
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);
        if (!usesPriority) {
            table.removeColumn(table.getColumnModel().getColumn(3));
        }

        JLabel titleLabel = new JLabel(algorithmName + " - Process Input", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        // ----- Buttons -----
        JButton addButton = new JButton("Add Process");
        JButton removeButton = new JButton("Remove Selected Row");
        JButton runButton = new JButton("Run");
        JButton sampleButton = new JButton("Load Sample Data");
        JButton resetButton = new JButton("Reset / Clear");

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        row1.add(addButton);
        row1.add(removeButton);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        row2.add(runButton);
        row2.add(sampleButton);
        row2.add(resetButton);
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1));
        buttonPanel.add(row1);
        buttonPanel.add(row2);

        // Header bar: title in the center, "? Info" button on the right.
        JButton infoButton = new JButton("? Info");
        infoButton.addActionListener(e -> JOptionPane.showMessageDialog(CpuPanel.this, getInfoText(),
                "Info - " + this.algorithmName, JOptionPane.INFORMATION_MESSAGE));
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.add(titleLabel, BorderLayout.CENTER);
        titleBar.add(infoButton, BorderLayout.EAST);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(titleBar, BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.SOUTH);

        // ----- Output area (top right) -----
        outputArea = new JTextArea(12, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Output"));

        // ----- Gantt chart (bottom, full width) -----
        ganttPanel = new GanttPanel();
        JScrollPane ganttScroll = new JScrollPane(ganttPanel);
        ganttScroll.setBorder(BorderFactory.createTitledBorder("Gantt Chart"));

        // Top section: Input Panel (left) | Output (right)
        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, outputScroll);
        topSplit.setResizeWeight(0.45);

        // Whole screen: top section above, Gantt chart below (full width)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, ganttScroll);
        mainSplit.setResizeWeight(0.55);
        add(mainSplit, BorderLayout.CENTER);

        // ----- Button actions -----
        addButton.addActionListener(e -> {
            stopEditing();
            tableModel.addRow(new Object[] {"", "", "", ""});
            int last = tableModel.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
        });

        removeButton.addActionListener(e -> {
            if (table.isEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
            int[] rows = table.getSelectedRows();
            if (rows.length == 0) {
                showError("Please select a row to remove first.");
                return;
            }
            // Remove from the bottom up so the row numbers stay correct.
            for (int i = rows.length - 1; i >= 0; i--) {
                tableModel.removeRow(rows[i]);
            }
        });

        runButton.addActionListener(e -> {
            // Save the cell the user is still typing in (even without pressing Enter).
            stopEditing();
            runSimulation();
        });

        sampleButton.addActionListener(e -> loadSampleData());

        resetButton.addActionListener(e -> resetAll());

        loadSampleData();
    }

    // Text shown by the "? Info" button.
    public String getInfoText() {
        if (usesPriority) {
            return """
                   PREEMPTIVE PRIORITY SCHEDULING
                   
                   - A LOWER priority number means HIGHER priority (1 beats 2). No aging.
                   - The CPU always runs the ready process with the best priority.
                   - A running process is preempted ONLY if a newly arrived process has a
                     strictly lower priority number. Equal priority does NOT preempt.
                   - Ties: earlier arrival time first, then lower PID.
                   - The clock starts at 0. If no process has arrived, the CPU is IDLE.
                   - Context-switch time is 0.
                   
                   TAT = Completion - Arrival
                   WT = TAT - Burst
                   RT = First CPU start - Arrival
                   CPU Utilization = Busy time / Total time x 100
                   Throughput = Number of processes / Total time""";
        }
        return """
               FIRST-COME FIRST-SERVED (FCFS) - non-preemptive
               
               - Processes run in the order they arrive.
               - Once a process starts, it runs until its burst is finished.
               - Ties: earlier arrival time first, then lower PID.
               - The clock starts at 0. If no process has arrived, the CPU is IDLE.
               - Context-switch time is 0.
               
               TAT = Completion - Arrival
               WT = TAT - Burst
               RT = First CPU start - Arrival
               CPU Utilization = Busy time / Total time x 100
               Throughput = Number of processes / Total time""";
    }

    // ------------------------------------------------------------------
    // Table helpers
    // ------------------------------------------------------------------

    private void stopEditing() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    private void clearTableAndOutput() {
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }
        tableModel.setRowCount(0);
        outputArea.setText("");
        ganttPanel.setResult(null);
    }

    // Fills the table with the practice problem set (10 processes).
    private void loadSampleData() {
        clearTableAndOutput();
        // One row = { PID, Arrival, Burst, Priority }
        int[][] sample = {
            {1, 4, 6, 3}, {2, 0, 9, 2}, {3, 7, 2, 1}, {4, 2, 11, 4}, {5, 5, 4, 2},
            {6, 0, 3, 1}, {7, 11, 5, 3}, {8, 3, 8, 2}, {9, 9, 1, 4}, {10, 6, 7, 1}
        };
        for (int[] row : sample) {
            tableModel.addRow(new Object[] {row[0], row[1], row[2], row[3]});
        }
    }

    // Back to 5 empty rows, empty output, empty chart.
    private void resetAll() {
        clearTableAndOutput();
        for (int i = 0; i < 5; i++) {
            tableModel.addRow(new Object[] {"", "", "", ""});
        }
    }

    private boolean isRowBlank(int row) {
        int cols = usesPriority ? 4 : 3;
        for (int col = 0; col < cols; col++) {
            Object val = tableModel.getValueAt(row, col);
            if (val != null && !val.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // Reads one cell as a whole number within [min, max]. Shows an error and returns null if invalid.
    private Integer readCell(int row, int col, String label, int min, int max) {
        int rowNumber = row + 1;
        Object raw = tableModel.getValueAt(row, col);
        int value;
        try {
            value = Integer.parseInt(raw == null ? "" : raw.toString().trim());
        } catch (NumberFormatException ex) {
            showError("Row " + rowNumber + ": " + label + " must be a whole number (it is empty or not a number).");
            return null;
        }
        if (value < min) {
            showError("Row " + rowNumber + ": " + label + (min == 1 ? " must be greater than 0." : " cannot be negative."));
            return null;
        }
        if (value > max) {
            showError("Row " + rowNumber + ": " + label + " must be at most " + max + ".");
            return null;
        }
        return value;
    }

    // Reads and validates every non-blank row. Shows an error and returns null if anything is wrong.
    private List<CpuScheduler.Process> readProcesses() {
        List<CpuScheduler.Process> list = new ArrayList<>();
        List<Integer> usedPids = new ArrayList<>();

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (isRowBlank(row)) {
                continue;
            }
            Integer pid = readCell(row, 0, "PID", 0, Integer.MAX_VALUE);
            if (pid == null) {
                return null;
            }
            Integer arrival = readCell(row, 1, "Arrival", 0, MAX_TIME_VALUE);
            if (arrival == null) {
                return null;
            }
            Integer burst = readCell(row, 2, "Burst", 1, MAX_TIME_VALUE);
            if (burst == null) {
                return null;
            }

            int priority = 0;   // FCFS does not use priority
            if (usesPriority) {
                Integer p = readCell(row, 3, "Priority", 0, Integer.MAX_VALUE);
                if (p == null) {
                    return null;
                }
                priority = p;
            }

            if (usedPids.contains(pid)) {
                showError("Row " + (row + 1) + ": PID " + pid + " is a duplicate. Every PID must be unique.");
                return null;
            }

            usedPids.add(pid);
            list.add(new CpuScheduler.Process(pid, arrival, burst, priority));
        }

        if (list.isEmpty()) {
            showError("There are no processes. Please add at least one process.");
            return null;
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Run the simulation and show the results
    // ------------------------------------------------------------------

    private void runSimulation() {
        List<CpuScheduler.Process> processes = readProcesses();
        if (processes == null) {
            return;   // an error message was already shown
        }

        CpuScheduler scheduler;
        if (usesPriority) {
            scheduler = new PreemptivePriorityCpu();
        } else {
            scheduler = new FcfsCpu();
        }

        CpuScheduler.CpuResult result = scheduler.simulate(processes);
        outputArea.setText(buildOutputText(result));
        outputArea.setCaretPosition(0);
        ganttPanel.setResult(result);
    }

    // Builds the per-process table and the summary metrics as plain text.
    private String buildOutputText(CpuScheduler.CpuResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(algorithmName).append(" Scheduling Results\n\n");

        sb.append(String.format("%-6s%-9s%-8s", "PID", "Arrival", "Burst"));
        if (usesPriority) {
            sb.append(String.format("%-10s", "Priority"));
        }
        sb.append(String.format("%-6s%-6s%-6s%-6s\n", "CT", "TAT", "WT", "RT"));

        for (CpuScheduler.Process p : result.processes) {
            sb.append(String.format("%-6d%-9d%-8d", p.pid, p.arrival, p.burst));
            if (usesPriority) {
                sb.append(String.format("%-10d", p.priority));
            }
            sb.append(String.format("%-6d%-6d%-6d%-6d\n", p.completion, p.turnaround, p.waiting, p.response));
        }

        sb.append("\nGantt order: ");
        String ganttIndent = "             ";   // 13 spaces = width of "Gantt order: "
        for (int i = 0; i < result.gantt.size(); i++) {
            CpuScheduler.GanttBlock b = result.gantt.get(i);
            sb.append("| ").append(b.label).append(" (").append(b.start)
            .append("-").append(b.end).append(") ");
            // After every 4 blocks, close the line and continue on a new indented line.
            if ((i + 1) % 4 == 0 && i + 1 < result.gantt.size()) {
                sb.append("|\n").append(ganttIndent);
            }
        }
        sb.append("|\n\n");

        sb.append("Total Time          = ").append(result.totalTime).append("\n");
        sb.append("Total Busy Time     = ").append(result.busyTime).append("\n");
        sb.append("Average WT          = ").append(String.format("%.2f", result.avgWT)).append("\n");
        sb.append("Average TAT         = ").append(String.format("%.2f", result.avgTAT)).append("\n");
        sb.append("Average RT          = ").append(String.format("%.2f", result.avgRT)).append("\n");
        sb.append("CPU Utilization (%) = ").append(String.format("%.2f", result.cpuUtilization)).append("\n");
        sb.append("Throughput          = ").append(String.format("%.2f", result.throughput))
          .append(" processes per time unit\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Gantt chart drawing panel
    // ------------------------------------------------------------------
    private static class GanttPanel extends JPanel {

        private static final int MIN_BLOCK_WIDTH = 36;         // ensures short labels like "IDLE" fit
        private static final int LEFT = 30;
        private static final int RIGHT = 40;
        private static final int BAR_Y = 50;
        private static final int BAR_HEIGHT = 60;
        private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 13);
        private static final Font TIME_FONT = new Font("SansSerif", Font.PLAIN, 12);

        private CpuScheduler.CpuResult result;

        private Color[] colors = {
            new Color(135, 206, 250), new Color(144, 238, 144), new Color(255, 218, 120),
            new Color(255, 160, 160), new Color(200, 170, 240), new Color(120, 220, 210),
            new Color(250, 190, 130), new Color(190, 210, 120)
        };

        public GanttPanel() {
            setBackground(Color.WHITE);
        }

        public void setResult(CpuScheduler.CpuResult result) {
            this.result = result;
            revalidate();   // tell the scroll pane the size may have changed
            repaint();
        }

        // Pixels per time unit: proportional time scale, clamped so charts stay readable.
        private int pixelsPerUnit() {
            return Math.max(16, Math.min(35, 1000 / result.totalTime));
        }

        // Width in pixels of one block (used by BOTH drawing and getPreferredSize).
        private int widthFor(CpuScheduler.GanttBlock b, int pxPerUnit) {
            return Math.max(MIN_BLOCK_WIDTH, (b.end - b.start) * pxPerUnit);
        }

        // Preferred width = sum of the exact same block widths that paintComponent draws.
        @Override
        public Dimension getPreferredSize() {
            if (result == null || result.totalTime <= 0 || result.gantt.isEmpty()) {
                return new Dimension(400, 200);
            }
            int pxPerUnit = pixelsPerUnit();
            int width = LEFT + RIGHT;
            for (CpuScheduler.GanttBlock b : result.gantt) {
                width += widthFor(b, pxPerUnit);
            }
            return new Dimension(width, 200);
        }

        // Same process always gets the same color. IDLE is gray.
        private Color colorFor(String label) {
            if (label.equals("IDLE")) {
                return Color.LIGHT_GRAY;
            }
            int pid = Integer.parseInt(label.substring(1));   // label looks like "P3"
            return colors[pid % colors.length];
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (result == null || result.gantt.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.drawString("Press Run to draw the Gantt chart here.", LEFT, 40);
                return;
            }

            int barBottom = BAR_Y + BAR_HEIGHT;
            int pxPerUnit = pixelsPerUnit();
            g2.setStroke(new BasicStroke(1f));

            int x = LEFT;   // advances block by block
            for (int i = 0; i < result.gantt.size(); i++) {
                CpuScheduler.GanttBlock b = result.gantt.get(i);
                int w = widthFor(b, pxPerUnit);

                // Block rectangle
                g2.setColor(colorFor(b.label));
                g2.fillRect(x, BAR_Y, w, BAR_HEIGHT);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, BAR_Y, w, BAR_HEIGHT);

                // Block label, centered in the rectangle.
                g2.setFont(LABEL_FONT);
                FontMetrics fm = g2.getFontMetrics();
                int textX = x + (w - fm.stringWidth(b.label)) / 2;
                int textY = BAR_Y + (BAR_HEIGHT + fm.getAscent()) / 2 - 2;
                g2.drawString(b.label, textX, textY);

                // Tick marks only at this block's start (x) and end (x + w).
                g2.setColor(Color.BLACK);
                g2.drawLine(x, barBottom, x, barBottom + 5);
                g2.drawLine(x + w, barBottom, x + w, barBottom + 5);

                // Start timestamp under the left edge of the block.
                g2.setFont(TIME_FONT);
                FontMetrics fm2 = g2.getFontMetrics();
                String startText = String.valueOf(b.start);
                g2.drawString(startText, x - fm2.stringWidth(startText) / 2, barBottom + 22);

                // End timestamp, only for the very last block
                // (every other block's end equals the next block's start).
                if (i == result.gantt.size() - 1) {
                    String endText = String.valueOf(b.end);
                    g2.drawString(endText, x + w - fm2.stringWidth(endText) / 2, barBottom + 22);
                }

                x += w;   // next block starts where this one ended
            }
        }
    }
}