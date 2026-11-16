// Swing (javax.swing) provides GUI components; AWT (java.awt) provides layouts, colors and drawing
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * C-LOOK disk scheduling simulator with a Swing GUI.
 *
 * C-LOOK: the head moves in one direction servicing requests until it reaches the
 * last request in that direction. It then jumps straight to the furthest request
 * at the opposite end and continues in the same direction.
 * (The jump counts as head movement in the total.)
 */
// "extends JFrame" makes this class itself a window
public class CLookSimulator extends JFrame {

    // static final = constant shared by the whole class
    private static final int DEFAULT_TRACK_SIZE = 200;
    private static final int MAX_TRACK_SIZE = 10000;
    private static final int MAX_REQUESTS = 100;

    // Input components (final = the reference never changes, but the text inside can)
    private final JTextField headField = new JTextField();
    private final JTextField trackSizeField = new JTextField(String.valueOf(DEFAULT_TRACK_SIZE));
    private final JComboBox<String> directionBox = new JComboBox<>(new String[]{"Right", "Left"});
    private final JTextArea queueArea = new JTextArea(3, 20); // 3 rows, 20 columns

    // Output components
    private final JTextArea outputArea = new JTextArea();
    private final GraphPanel graphPanel = new GraphPanel(); // custom drawing panel, defined below

    public CLookSimulator() {
        super("C-LOOK Disk Scheduling Simulator"); // calls JFrame's constructor to set the title
        setDefaultCloseOperation(EXIT_ON_CLOSE);   // closing the window ends the program
        setLayout(new BorderLayout());             // regions: NORTH, SOUTH, EAST, WEST, CENTER

        // Left side: inputs on top, output filling the rest
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(510, 720));
        leftPanel.add(buildInputPanel(), BorderLayout.NORTH);
        leftPanel.add(buildOutputPanel(), BorderLayout.CENTER);

        // The graph takes all remaining space to the right
        add(leftPanel, BorderLayout.WEST);
        add(graphPanel, BorderLayout.CENTER);

        setSize(1280, 720);
        setLocationRelativeTo(null); // center window
    }

    // ------------------------------------------------------------------ GUI

    private JPanel buildInputPanel() {
        // GridBagLayout arranges components in a grid; GridBagConstraints configures each cell
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // padding around the edge
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);          // spacing around each component
        c.fill = GridBagConstraints.HORIZONTAL;     // components stretch to fill their cell width

        addRow(panel, c, 0, "Initial Head Position:", headField);
        addRow(panel, c, 1, "Track Size (default 200 = 0-199):", trackSizeField);
        addRow(panel, c, 2, "Direction:", directionBox);

        queueArea.setLineWrap(true);
        queueArea.setToolTipText("Track numbers separated by commas or spaces, e.g. 98, 183, 37");
        // JScrollPane adds scrollbars when the text is too long
        addRow(panel, c, 3, "Request Queue (comma/space separated):", new JScrollPane(queueArea));

        JButton runButton = new JButton("Run C-LOOK");
        JButton resetButton = new JButton("Reset");
        // "e -> ..." is a lambda: a short function that runs when the button is clicked
        runButton.addActionListener(e -> runSimulation());
        resetButton.addActionListener(e -> reset());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // places items in a row
        buttons.add(runButton);
        buttons.add(resetButton);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2; // span both columns
        panel.add(buttons, c);

        return panel;
    }

    /** Adds one "label + input field" row to the grid. */
    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridy = row;
        c.gridwidth = 1;
        c.gridx = 0;        // column 0 = label
        c.weightx = 0.4;    // share of extra width this column receives
        JLabel text = new JLabel("<html>" + label + "</html>"); // <html> lets long labels wrap
        panel.add(text, c);
        c.gridx = 1;        // column 1 = input
        c.weightx = 0.6;
        panel.add(field, c);
    }

    private JScrollPane buildOutputPanel() {
        outputArea.setEditable(false);     // read-only, the user cannot type here
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true); // wrap at word boundaries, not mid-word
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Output"));
        return scroll;
    }

    // ------------------------------------------------------------ Actions

    /** Restores every field, the output area and the graph to their starting state. */
    private void reset() {
        headField.setText("");
        trackSizeField.setText(String.valueOf(DEFAULT_TRACK_SIZE));
        directionBox.setSelectedIndex(0);
        queueArea.setText("");
        showOutput(" ", Color.BLACK);
        graphPanel.clear();
    }

    /** Called when "Run C-LOOK" is clicked: validates all input, then runs the algorithm. */
    private void runSimulation() {
        // Every problem found is collected here so the user sees them all at once
        List<String> errors = new ArrayList<>();

        // Integer (not int) is used below because an Integer can be null, meaning "invalid input"

        // 1. Validate track size
        Integer trackSize = parseNumber(trackSizeField.getText(), "Track size", errors);
        if (trackSize != null && (trackSize < 1 || trackSize > MAX_TRACK_SIZE)) {
            errors.add("Track size must be between 1 and " + MAX_TRACK_SIZE + ".");
            trackSize = null;
        }

        // 2. Validate initial head position (needs a valid track size to check the range)
        Integer head = parseNumber(headField.getText(), "Initial head position", errors);
        if (head != null && trackSize != null && (head < 0 || head >= trackSize)) {
            errors.add("Initial head position must be between 0 and " + (trackSize - 1) + ".");
            head = null;
        }

        // 3. Validate request queue
        List<Integer> requests = parseQueue(queueArea.getText(), trackSize, errors);

        // Show every error found and stop
        if (!errors.isEmpty()) {
            // StringBuilder builds a string efficiently by appending pieces
            StringBuilder sb = new StringBuilder("ERROR:\n");
            for (String error : errors) {
                sb.append(" - ").append(error).append("\n");
            }
            showOutput(sb.toString(), Color.RED);
            graphPanel.clear();
            return;
        }

        boolean goingRight = directionBox.getSelectedItem().equals("Right");
        runCLook(head, trackSize, goingRight, requests); // Integers are auto-unboxed to int here
    }

    /** Converts text to an int. Adds a message to errors and returns null if invalid. */
    private Integer parseNumber(String text, String name, List<String> errors) {
        text = text.trim(); // remove leading/trailing spaces
        if (text.isEmpty()) {
            errors.add(name + " is required.");
            return null;
        }
        // Integer.parseInt throws NumberFormatException for text like "abc" or "3.5"
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            errors.add(name + " must be a whole number (got \"" + text + "\").");
            return null;
        }
    }

    /** Converts the queue text to a list of valid track numbers. */
    private List<Integer> parseQueue(String text, Integer trackSize, List<String> errors) {
        List<Integer> requests = new ArrayList<>();
        text = text.trim();
        if (text.isEmpty()) {
            errors.add("Request queue is empty. Enter at least one track number.");
            return requests;
        }

        // split() takes a regular expression: "[,\\s]+" means "one or more commas or whitespace"
        for (String part : text.split("[,\\s]+")) {
            try {
                int value = Integer.parseInt(part);
                // trackSize is null if it was invalid, so the upper-bound check is skipped in that case
                if (value < 0 || (trackSize != null && value >= trackSize)) {
                    String max = (trackSize != null) ? String.valueOf(trackSize - 1) : "track size - 1";
                    errors.add("Request " + value + " is out of range (valid range is 0 to " + max + ").");
                } else {
                    requests.add(value);
                }
            } catch (NumberFormatException e) {
                errors.add("Invalid request \"" + part + "\" - requests must be whole numbers.");
            }
        }

        if (requests.size() > MAX_REQUESTS) {
            errors.add("Too many requests (" + requests.size() + "). Maximum is " + MAX_REQUESTS + ".");
        }
        return requests;
    }

    // ------------------------------------------------------- C-LOOK logic

    private void runCLook(int head, int trackSize, boolean goingRight, List<Integer> requests) {
        // Copy the list first so the user's original order is not modified, then sort it
        List<Integer> sorted = new ArrayList<>(requests);
        Collections.sort(sorted);

        // Split into requests ahead of the head and requests behind the head
        List<Integer> ahead = new ArrayList<>();
        List<Integer> behind = new ArrayList<>();

        if (goingRight) {
            for (int r : sorted) {
                if (r >= head) ahead.add(r); else behind.add(r);
            }
            // ahead is already ascending; behind is serviced ascending after the jump
        } else {
            for (int r : sorted) {
                if (r <= head) ahead.add(r); else behind.add(r);
            }
            // going left: service in descending order
            Collections.reverse(ahead);
            Collections.reverse(behind);
        }

        // Build the full path: head, then ahead, then (jump) behind
        List<Integer> path = new ArrayList<>();
        path.add(head);
        path.addAll(ahead);
        path.addAll(behind);

        // The jump is the move onto the first "behind" request (if there is one)
        // -1 means "no jump happened"
        int jumpIndex = behind.isEmpty() ? -1 : ahead.size() + 1;

        // Calculate head movement for each step
        int total = 0;
        StringBuilder sequence = new StringBuilder();
        StringBuilder steps = new StringBuilder();

        for (int i = 1; i < path.size(); i++) {
            int from = path.get(i - 1);
            int to = path.get(i);
            int move = Math.abs(to - from); // distance travelled, always positive
            total += move;

            sequence.append(to);
            if (i < path.size() - 1) sequence.append(" -> ");

            // String.format fills %d with ints; %n is a platform-independent newline
            steps.append(String.format("  %d -> %d = %d%s%n", from, to, move, i == jumpIndex ? "  (jump)" : ""));
        }

        // Cast to double first, otherwise int / int would drop the decimal part
        double average = (double) total / requests.size();

        String result = "SEEK SEQUENCE:\n  " + head + " -> " + sequence + "\n\n"
                + "HEAD MOVEMENT FOR EACH STEP:\n" + steps + "\n"
                + "TOTAL HEAD MOVEMENT: " + total + "\n"
                + "AVERAGE SEEK DISTANCE: " + String.format("%.2f", average) + "\n"; // %.2f = 2 decimals

        showOutput(result, Color.BLACK);
        graphPanel.setData(path, trackSize, jumpIndex);
    }

    /** Displays text in the output area in the given color (black for results, red for errors). */
    private void showOutput(String text, Color color) {
        outputArea.setForeground(color);
        outputArea.setText(text);
        outputArea.setCaretPosition(0); // scroll back to the top
    }

    // --------------------------------------------------------------- Graph

    /**
     * Draws the head path. X axis = track number, Y axis = step (top to bottom).
     * "static" nested class: it does not need access to a CLookSimulator object.
     */
    private static class GraphPanel extends JPanel {
        // Empty space (in pixels) kept around the graph for labels
        private static final int LEFT = 40, RIGHT = 40, TOP = 70, BOTTOM = 40;

        private List<Integer> path = new ArrayList<>();
        private int trackSize = DEFAULT_TRACK_SIZE;
        private int jumpIndex = -1;

        GraphPanel() {
            setBackground(Color.WHITE);
        }

        /** Stores the new data and asks Swing to redraw the panel. */
        void setData(List<Integer> path, int trackSize, int jumpIndex) {
            this.path = path;           // "this." separates the field from the parameter of the same name
            this.trackSize = trackSize;
            this.jumpIndex = jumpIndex;
            repaint();                  // schedules a call to paintComponent
        }

        /** Empties the graph. */
        void clear() {
            path = new ArrayList<>();
            jumpIndex = -1;
            repaint();
        }

        /** Swing calls this automatically whenever the panel needs drawing. All drawing goes here. */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // clears the panel background first
            // Graphics2D is the newer drawing class; the cast gives access to antialiasing and strokes
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Head Movement (C-LOOK)", LEFT, 25);

            // Nothing to plot yet
            if (path.isEmpty()) {
                g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g2.drawString("Run the simulation to see the graph.", LEFT, 50);
                return;
            }

            drawAxis(g2);
            drawPath(g2);
            drawLegend(g2);
        }

        /** Converts a track number into an x pixel position (scaled to the panel width). */
        private int xFor(int track) {
            int width = getWidth() - LEFT - RIGHT;
            int maxTrack = Math.max(1, trackSize - 1); // avoids dividing by zero when trackSize is 1
            return LEFT + (int) ((long) track * width / maxTrack); // long avoids int overflow
        }

        /** Converts a step number into a y pixel position (steps are evenly spaced). */
        private int yFor(int step) {
            int height = getHeight() - TOP - BOTTOM;
            int steps = Math.max(1, path.size() - 1);
            return TOP + step * height / steps;
        }

        /** Draws the track-number scale along the top with vertical grid lines. */
        private void drawAxis(Graphics2D g2) {
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            int ticks = 10;
            for (int i = 0; i <= ticks; i++) {
                int track = (trackSize - 1) * i / ticks;
                int x = xFor(track);
                g2.setColor(new Color(225, 225, 225)); // light grey grid line
                g2.drawLine(x, TOP - 10, x, getHeight() - BOTTOM);
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(track), x - 8, TOP - 15);
            }
            g2.drawLine(LEFT, TOP - 10, getWidth() - RIGHT, TOP - 10);
        }

        /** Draws the lines between consecutive positions, then the points and their labels. */
        private void drawPath(Graphics2D g2) {
            // Lines (drawn first so the points sit on top of them)
            for (int i = 1; i < path.size(); i++) {
                int x1 = xFor(path.get(i - 1)), y1 = yFor(i - 1);
                int x2 = xFor(path.get(i)), y2 = yFor(i);

                if (i == jumpIndex) {
                    // Dashed red line: dash pattern is 6px drawn, 6px gap
                    g2.setColor(Color.RED);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            10f, new float[]{6f, 6f}, 0f));
                } else {
                    g2.setColor(new Color(30, 90, 200)); // Color(red, green, blue), each 0-255
                    g2.setStroke(new BasicStroke(2f));
                }
                g2.drawLine(x1, y1, x2, y2);
            }
            g2.setStroke(new BasicStroke(1f)); // reset to a plain line for later drawing

            // Points and labels (the first point is the starting head position)
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            for (int i = 0; i < path.size(); i++) {
                int x = xFor(path.get(i)), y = yFor(i);
                g2.setColor(i == 0 ? new Color(0, 150, 0) : new Color(30, 90, 200));
                g2.fillOval(x - 4, y - 4, 8, 8); // x, y = top-left corner, so subtract half the size to center
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(path.get(i)), x + 6, y - 5);
            }
        }

        /** Draws the small Start / Seek / Jump key in the top-right corner. */
        private void drawLegend(Graphics2D g2) {
            int x = getWidth() - 230, y = 25;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));

            g2.setColor(new Color(0, 150, 0));
            g2.fillOval(x, y - 8, 8, 8);
            g2.setColor(Color.BLACK);
            g2.drawString("Start", x + 12, y);

            g2.setColor(new Color(30, 90, 200));
            g2.drawLine(x + 55, y - 4, x + 80, y - 4);
            g2.setColor(Color.BLACK);
            g2.drawString("Seek", x + 85, y);

            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{4f, 4f}, 0f));
            g2.drawLine(x + 125, y - 4, x + 150, y - 4);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(Color.BLACK);
            g2.drawString("Jump", x + 155, y);
        }
    }

    // ---------------------------------------------------------------- Main

    public static void main(String[] args) {
        // Swing windows should be created on the Event Dispatch Thread (Swing's UI thread);
        // invokeLater schedules the code on that thread
        SwingUtilities.invokeLater(() -> new CLookSimulator().setVisible(true));
    }
}
