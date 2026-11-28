package ui;

import disk.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

// Screen for the two disk algorithms (C-SCAN and C-LOOK).
public final class DiskPanel extends JPanel {

    private static final String DEFAULT_DISK_SIZE = "200";
    // Safety limit so a huge disk size cannot make the graph unreasonably wide.
    private static final int MAX_DISK_SIZE = 10000;

    private String algorithmName;

    private final JTextField requestsField;
    private final JTextField headField;
    private final JTextField sizeField;
    private final JComboBox<String> directionBox;
    private final JTextArea outputArea;
    private final GraphPanel graphPanel;

    public DiskPanel(String algorithmName) {
        this.algorithmName = algorithmName;
        setLayout(new BorderLayout());

        // ----- Input form -----
        requestsField = new JTextField();
        headField = new JTextField();
        sizeField = new JTextField(DEFAULT_DISK_SIZE);
        directionBox = new JComboBox<>(new String[] {"Right", "Left"});

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        form.add(new JLabel("Requests (comma or space):"));
        form.add(requestsField);
        form.add(new JLabel("Initial Head Position:"));
        form.add(headField);
        form.add(new JLabel("Track/Disk Size N:"));
        form.add(sizeField);
        form.add(new JLabel("Initial Direction:"));
        form.add(directionBox);

        JLabel titleLabel = new JLabel(algorithmName + " - Disk Scheduling Input", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        // ----- Buttons -----
        JButton runButton = new JButton("Run");
        JButton sampleButton = new JButton("Load Sample Data");
        JButton resetButton = new JButton("Reset / Clear");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        buttonPanel.add(runButton);
        buttonPanel.add(sampleButton);
        buttonPanel.add(resetButton);

        // Header bar: title in the center, "? Info" button on the right.
        JButton infoButton = new JButton("? Info");
        infoButton.addActionListener(e -> JOptionPane.showMessageDialog(DiskPanel.this, getInfoText(),
                "Info - " + this.algorithmName, JOptionPane.INFORMATION_MESSAGE));
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.add(titleLabel, BorderLayout.CENTER);
        titleBar.add(infoButton, BorderLayout.EAST);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(titleBar, BorderLayout.NORTH);
        inputPanel.add(form, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.SOUTH);

        // ----- Output area -----
        outputArea = new JTextArea(12, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Output"));

        // Keep the form at its natural height: put inputPanel at NORTH of a BorderLayout container
        JPanel inputWrapper = new JPanel(new BorderLayout());
        inputWrapper.add(inputPanel, BorderLayout.NORTH);

        // ----- Head Movement Graph (bottom, full width) -----
        // The graph scales itself to fit whatever space the bottom area has.
        graphPanel = new GraphPanel();
        JPanel graphHolder = new JPanel(new BorderLayout());
        graphHolder.setBorder(BorderFactory.createTitledBorder("Head Movement Graph"));
        graphHolder.add(graphPanel, BorderLayout.CENTER);

        // Top section: Input Panel (left) | Output (right)
        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputWrapper, outputScroll);
        topSplit.setResizeWeight(0.45);

        // Whole screen: top section above, graph below (full width)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, graphHolder);
        mainSplit.setResizeWeight(0.55);
        add(mainSplit, BorderLayout.CENTER);

        // ----- Button actions -----
        runButton.addActionListener(e -> runSimulation());

        sampleButton.addActionListener(e -> loadSampleData());

        resetButton.addActionListener(e -> resetAll());

        loadSampleData();
    }

    // Text shown by the "? Info" button.
    public String getInfoText() {
        if (algorithmName.equals("C-SCAN")) {
            return """
                   C-SCAN (Circular SCAN)
                   
                   - The head moves in the chosen direction and serves requests on the way.
                   - It continues all the way to the physical end of the disk
                     (N-1 if moving Right, 0 if moving Left), even if no request is there.
                   - If requests are still waiting on the other side, the head makes a
                     RETURN JUMP to the opposite end (0 or N-1) and keeps moving in the
                     SAME direction. The jump distance counts as head movement.
                   - If nothing is waiting on the other side, it stops at the last request.
                   - Requests equal to the head (and duplicates) cost 0 movement.
                   
                   Total Head Movement = sum of |difference| between consecutive positions
                   Average Seek Distance = Total Head Movement / Number of Requests""";
        }
        return """
               C-LOOK (Circular LOOK)
               
               - The head moves in the chosen direction and serves requests on the way.
               - It stops at the LAST request in that direction (it does NOT go to 0 or N-1).
               - If requests are still waiting on the other side, the head jumps directly
                 to the FURTHEST waiting request on the opposite end and keeps moving in
                 the SAME direction. The jump distance counts as head movement.
               - If nothing is waiting on the other side, it simply stops.
               - Requests equal to the head (and duplicates) cost 0 movement.
               
               Total Head Movement = sum of |difference| between consecutive positions
               Average Seek Distance = Total Head Movement / Number of Requests""";
    }

    // ------------------------------------------------------------------
    // Button helpers
    // ------------------------------------------------------------------

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    // Loads the practice problem set (200-track disk, 20 requests, head at 92, Right).
    private void loadSampleData() {
        requestsField.setText("148, 31, 92, 174, 14, 119, 63, 189, 45, 136, 77, 161, 23, 105, 195, 54, 128, 86, 152, 39");
        headField.setText("92");
        sizeField.setText(DEFAULT_DISK_SIZE);
        directionBox.setSelectedItem("Right");

        outputArea.setText("");
        graphPanel.clear();
    }

    // Clears everything and puts N back to 200 and the direction back to Right.
    private void resetAll() {
        requestsField.setText("");
        headField.setText("");
        sizeField.setText(DEFAULT_DISK_SIZE);
        directionBox.setSelectedItem("Right");
        outputArea.setText("");
        graphPanel.clear();
    }

    // Converts text to a whole number. Returns null if it is empty or not a number.
    private Integer parseInteger(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // Reads the request list. Shows an error and returns null if anything is wrong.
    private List<Integer> readRequests(int diskSize) {
        // Split on commas and/or spaces.
        String[] tokens = requestsField.getText().trim().split("[,\\s]+");
        List<Integer> list = new ArrayList<>();

        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            Integer value = parseInteger(token);
            if (value == null) {
                showError("\"" + token + "\" is not a valid whole number.");
                return null;
            }
            int cylinder = value;
            if (cylinder < 0 || cylinder >= diskSize) {
                showError("Cylinder " + cylinder + " is out of range. Valid cylinders are 0 to "
                        + (diskSize - 1) + ".");
                return null;
            }
            list.add(value);
        }

        if (list.isEmpty()) {
            showError("Please enter at least one cylinder request.");
            return null;
        }
        return list;
    }

    // Reads one text field as a whole number within [min, max]. Returns null if invalid.
    private Integer readField(JTextField field, String label, int min, int max) {
        Integer value = parseInteger(field.getText());
        if (value == null) {
            showError(label + " must be a whole number.");
            return null;
        }
        if (value < min || value > max) {
            showError(label + " must be between " + min + " and " + max + ".");
            return null;
        }
        return value;
    }

    // ------------------------------------------------------------------
    // Run the simulation and show the results
    // ------------------------------------------------------------------

    private void runSimulation() {
        Integer diskSize = readField(sizeField, "Track/Disk Size N", 1, MAX_DISK_SIZE);
        if (diskSize == null) {
            return;
        }

        Integer head = readField(headField, "Initial Head Position", 0, diskSize - 1);
        if (head == null) {
            return;
        }

        List<Integer> requests = readRequests(diskSize);
        if (requests == null) {
            return;   // an error message was already shown
        }

        String direction = (String) directionBox.getSelectedItem();

        DiskScheduler scheduler;
        if (algorithmName.equals("C-SCAN")) {
            scheduler = new CScanDisk();
        } else {
            scheduler = new CLookDisk();
        }

        DiskScheduler.DiskResult result = scheduler.simulate(requests, head, diskSize, direction);

        outputArea.setText(buildOutputText(result, requests.size(), head, diskSize, direction));
        outputArea.setCaretPosition(0);
        graphPanel.setResult(result, diskSize);
    }

    private String buildOutputText(DiskScheduler.DiskResult result, int requestCount,
                                   int head, int diskSize, String direction) {
        StringBuilder sb = new StringBuilder();
        sb.append(algorithmName).append(" Disk Scheduling Results\n\n");
        sb.append("Disk Size N        = ").append(diskSize)
          .append(" (cylinders 0 to ").append(diskSize - 1).append(")\n");
        sb.append("Initial Head       = ").append(head).append("\n");
        sb.append("Initial Direction  = ").append(direction).append("\n");
        sb.append("Number of Requests = ").append(requestCount).append("\n\n");

        // Order of service (full head path; non-request boundaries in parentheses).
        sb.append("Order of service:\n");
        for (int i = 0; i < result.path.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            int cyl = result.path.get(i);
            sb.append(result.requestFlags.get(i) ? cyl : "(" + cyl + ")");
        }
        sb.append("\n\n");

        // Summary metrics right after the order of service.
        sb.append("Total Head Movement   = ").append(result.totalHeadMovement).append(" cylinders\n");
        sb.append("Average Seek Distance = ").append(result.totalHeadMovement).append(" / ")
          .append(requestCount).append(" = ")
          .append(String.format("%.2f", result.averageSeekDistance)).append("\n\n");

        sb.append("Step-by-step head movement:\n");
        for (int i = 0; i < result.steps.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(result.steps.get(i)).append("\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Head movement graph drawing panel
    // ------------------------------------------------------------------
    private static class GraphPanel extends JPanel {

        // The scale is computed from the panel size on every paint.
        private static final int MIN_TICK_GAP = 40;   // minimum pixels between axis labels
        private static final int LEFT = 30;
        private static final int RIGHT = 60;
        private static final int AXIS_Y = 55;         // the cylinder axis at the top
        private static final int FIRST_ROW_Y = 85;    // y of the starting head position
        private static final int MAX_ROW_HEIGHT = 28; // rows are never taller than this
        private static final int BOTTOM = 25;
        private static final Font LEGEND_FONT = new Font("SansSerif", Font.PLAIN, 12);
        private static final Font AXIS_FONT = new Font("SansSerif", Font.PLAIN, 10);
        private static final BasicStroke SOLID_STROKE = new BasicStroke(2f);
        private static final BasicStroke DASHED_STROKE = new BasicStroke(
            2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {6f, 4f}, 0f);
        private static final BasicStroke THIN_STROKE = new BasicStroke(1f);
        private static final Color MOVE_BLUE = new Color(30, 90, 200);
        private static final Color START_GREEN = new Color(0, 150, 0);
        private static final Color GRID_GRAY = new Color(225, 225, 225);

        private List<Integer> path;
        private List<Boolean> jumpFlags;
        private int diskSize = 200;

        // Updated at the start of every paint so the graph always fits the panel.
        private double pixelsPerCylinder = 1.0;
        private double rowHeight = MAX_ROW_HEIGHT;

        public GraphPanel() {
            setBackground(Color.WHITE);
        }

        public void setResult(DiskScheduler.DiskResult result, int diskSize) {
            this.path = result.path;
            this.jumpFlags = result.jumpFlags;
            this.diskSize = diskSize;
            revalidate();
            repaint();
        }

        public void clear() {
            path = null;
            jumpFlags = null;
            revalidate();
            repaint();
        }

        // Fixed preferred size so the graph scales with the window instead of forcing scroll bars.
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(500, 200);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(200, 120);
        }

        // Converts a cylinder number to an x pixel using the current scale.
        private int xFor(int cylinder) {
            return LEFT + (int) Math.round(cylinder * pixelsPerCylinder);
        }

        // Returns the y pixel position of step number "index" (0 = starting head position).
        private int yFor(int index) {
            return FIRST_ROW_Y + (int) Math.round(index * rowHeight);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (path == null) {
                g2.setColor(Color.GRAY);
                g2.drawString("Press Run to draw the head movement graph here.", LEFT, 40);
                return;
            }

            // Fit the graph inside the visible area.
            int steps = Math.max(path.size() - 1, 1);
            int lastCylinder = Math.max(diskSize - 1, 1);

            // Horizontal: squeeze 0..N-1 into the available width.
            int usableWidth = Math.max(getWidth() - LEFT - RIGHT, 50);
            pixelsPerCylinder = (double) usableWidth / lastCylinder;

            // Vertical: squeeze all steps into the available height
            // (rows are never taller than MAX_ROW_HEIGHT).
            int usableHeight = Math.max(getHeight() - FIRST_ROW_Y - BOTTOM, 20);
            rowHeight = Math.min((double) usableHeight / steps, MAX_ROW_HEIGHT);

            // Smaller rows get smaller dots and text so everything stays readable.
            int dotSize = (int) Math.max(4, Math.min(8, rowHeight - 2));
            int labelSize = (int) Math.max(7, Math.min(11, rowHeight));

            // Axis labels: pick a step (multiple of 10) so labels never overlap.
            int tickStep = 10;
            while (tickStep * pixelsPerCylinder < MIN_TICK_GAP) {
                tickStep = tickStep + 10;
            }

            int bottomY = yFor(path.size() - 1) + 15;

            // Legend
            g2.setFont(LEGEND_FONT);
            g2.setColor(Color.BLACK);
            g2.drawString("Cylinder axis (0 to " + (diskSize - 1)
                + ").  Blue = head movement, red dashed = return jump, green dot = start.", LEFT, 20);

            // Cylinder axis across the top, with light vertical grid lines.
            g2.setFont(AXIS_FONT);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Color.BLACK);
            g2.drawLine(xFor(0), AXIS_Y, xFor(diskSize - 1), AXIS_Y);
            for (int c = 0; c < diskSize; c = c + tickStep) {
                drawAxisMark(g2, fm, c, bottomY);
            }
            // Mark the last cylinder (N-1) too, unless it would crowd the previous label.
            int lastTick = ((diskSize - 1) / tickStep) * tickStep;
            if ((diskSize - 1) != lastTick
                    && ((diskSize - 1) - lastTick) * pixelsPerCylinder >= MIN_TICK_GAP / 2) {
                drawAxisMark(g2, fm, diskSize - 1, bottomY);
            }

            // Head path: one row per step, going downward.
            for (int i = 1; i < path.size(); i++) {
                int x1 = xFor(path.get(i - 1));
                int y1 = yFor(i - 1);
                int x2 = xFor(path.get(i));
                int y2 = yFor(i);

                if (jumpFlags.get(i)) {
                    g2.setStroke(DASHED_STROKE);
                    g2.setColor(Color.RED);
                } else {
                    g2.setStroke(SOLID_STROKE);
                    g2.setColor(MOVE_BLUE);
                }
                g2.drawLine(x1, y1, x2, y2);
            }

            // Dots and the cylinder number next to EVERY vertex (including 0 and N-1).
            g2.setStroke(THIN_STROKE);
            g2.setFont(new Font("SansSerif", Font.BOLD, labelSize));
            for (int i = 0; i < path.size(); i++) {
                int x = xFor(path.get(i));
                int y = yFor(i);

                if (i == 0) {
                    g2.setColor(START_GREEN);
                } else {
                    g2.setColor(Color.BLACK);
                }
                g2.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(path.get(i)), x + dotSize / 2 + 4, y + labelSize / 3);
            }
        }

        // Draws one tick, its label and a light grid line for a cylinder.
        private void drawAxisMark(Graphics2D g2, FontMetrics fm, int cylinder, int bottomY) {
            int x = xFor(cylinder);
            g2.setColor(GRID_GRAY);
            g2.drawLine(x, AXIS_Y, x, bottomY);
            g2.setColor(Color.BLACK);
            g2.drawLine(x, AXIS_Y - 4, x, AXIS_Y);
            String text = String.valueOf(cylinder);
            g2.drawString(text, x - fm.stringWidth(text) / 2, AXIS_Y - 8);
        }
    }
}