import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import ui.CpuPanel;
import ui.DiskPanel;

// Entry point (default package). Builds the window, the top navigation bar,
// the Home / Main Menu screen, and switches between screens with a CardLayout.
public class Main {

    // These names are used both as button texts and as CardLayout card names.
    private static final String HOME = "Home / Menu";
    private static final String FCFS = "FCFS";
    private static final String PRIORITY = "Preemptive Priority";
    private static final String C_SCAN = "C-SCAN";
    private static final String C_LOOK = "C-LOOK";

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private final List<JButton> navButtons = new ArrayList<>();

    public static void main(String[] args) {
        // Use the native (system) look and feel before any Swing component is created.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // If it fails, Swing simply keeps its default look and feel.
        }

        // Swing programs should be started on the Swing event thread.
        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            app.createAndShow();
        });
    }

    private void createAndShow() {
        frame = new JFrame("CPU and Disk Scheduling Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Put every screen into the CardLayout.
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(buildHomePanel(), HOME);
        cardPanel.add(new CpuPanel(FCFS), FCFS);
        cardPanel.add(new CpuPanel(PRIORITY), PRIORITY);
        cardPanel.add(new DiskPanel(C_SCAN), C_SCAN);
        cardPanel.add(new DiskPanel(C_LOOK), C_LOOK);

        frame.add(buildNavBar(), BorderLayout.NORTH);
        frame.add(cardPanel, BorderLayout.CENTER);

        frame.setSize(1250, 760);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setLocationRelativeTo(null);   // center on screen
        showCard(HOME);                      // start on the Main Menu
        frame.setVisible(true);
    }

    // ------------------------------------------------------------------
    // Top navigation bar (always visible)
    // ------------------------------------------------------------------
    private JPanel buildNavBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

        bar.add(makeNavButton(HOME));
        bar.add(makeNavButton(FCFS));
        bar.add(makeNavButton(PRIORITY));
        bar.add(makeNavButton(C_SCAN));
        bar.add(makeNavButton(C_LOOK));

        JButton aboutButton = new JButton("About");
        aboutButton.addActionListener(e -> showAbout());
        bar.add(aboutButton);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));
        bar.add(exitButton);

        return bar;
    }

    // Creates a button that switches to the given card.
    private JButton makeNavButton(String cardName) {
        JButton button = new JButton(cardName);
        button.addActionListener(e -> showCard(cardName));
        navButtons.add(button);
        return button;
    }

    // Used by both the top navbar buttons and the big Home menu buttons.
    // Active button: bold + blue text. Others: plain + black text.
    // (Windows Look & Feel ignores custom button backgrounds, so we use text color.)
    private void showCard(String cardName) {
        cardLayout.show(cardPanel, cardName);

        for (JButton b : navButtons) {
            boolean active = cardName.equals(b.getText());
            b.setFont(b.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN));
            b.setForeground(active ? new Color(0, 102, 204) : Color.BLACK);
        }
    }

    // ------------------------------------------------------------------
    // Home / Main Menu screen
    // ------------------------------------------------------------------
    private JPanel buildHomePanel() {
        JPanel home = new JPanel(new BorderLayout(20, 20));
        home.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        // Title
        JLabel title = new JLabel("Group 4 - Ulysses Final Project", JLabel.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        JLabel subtitle = new JLabel("Operating Systems Project - choose an algorithm to begin", JLabel.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 6));
        titlePanel.add(title);
        titlePanel.add(subtitle);
        home.add(titlePanel, BorderLayout.NORTH);

        // Two groups of big buttons: CPU Scheduling and Disk Scheduling
        JPanel groups = new JPanel(new GridLayout(1, 2, 40, 0));
        groups.add(buildGroupPanel("CPU Scheduling", new String[] {FCFS, PRIORITY}));
        groups.add(buildGroupPanel("Disk Scheduling", new String[] {C_SCAN, C_LOOK}));
        home.add(groups, BorderLayout.CENTER);

        // About and Exit at the bottom
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton aboutButton = makeBigButton("About");
        aboutButton.addActionListener(e -> showAbout());
        JButton exitButton = makeBigButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));
        bottom.add(aboutButton);
        bottom.add(exitButton);
        home.add(bottom, BorderLayout.SOUTH);

        return home;
    }

    // A titled box that holds one big button per algorithm.
    private JPanel buildGroupPanel(String groupTitle, String[] cardNames) {
        JPanel group = new JPanel(new GridLayout(0, 1, 15, 15));
        group.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(groupTitle),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)));

        for (String cardName : cardNames) {
            JButton button = makeBigButton(cardName);
            button.addActionListener(e -> showCard(cardName));
            group.add(button);
        }
        return group;
    }

    private JButton makeBigButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 18));
        button.setPreferredSize(new Dimension(220, 60));
        return button;
    }

    // ------------------------------------------------------------------
    // About dialog
    // ------------------------------------------------------------------
    private void showAbout() {
        String text = """
                      CPU and Disk Scheduling Simulator
                      
                      Group 4 - Ulysses (Section 3ITB)
                      
                      Members:
                        - Justin Rei Aviles - C-LOOK
                        - Airuz Arnold Balungay - C-SCAN
                        - Carlo Magnus Cuison - FCFS
                        - Johann Henreich Gomez - Preemptive Priority""";
        JOptionPane.showMessageDialog(frame, text, "About", JOptionPane.INFORMATION_MESSAGE);
    }
}