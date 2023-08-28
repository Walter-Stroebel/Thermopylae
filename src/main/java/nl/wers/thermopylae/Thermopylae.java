/*
 */
package nl.wers.thermopylae;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Yes, one big static main class.
 *
 * This is KISS to the max, everybody needs to be able to read this. The
 * intelligence is in the LLM, not in this tool.
 *
 * @author Walter Stroebel
 */
public class Thermopylae {

    private static final Font font = new Font(Font.SERIF, Font.PLAIN, 24);
    private static final String osName = System.getProperty("os.name").toLowerCase();
    private static boolean debug = false;
    private static boolean waitingForLLM = false;
    private static volatile String lastText = "";
    private static final int MAX_COMMAND_SIZE = 200;
    private static int MAX_OUTPUT_SIZE = 6000;

    public static void main(String[] args) {
        try {
            UIManager.put("Button.font", font);
            UIManager.put("Label.font", font);
            setup();
        } catch (Exception ex) {
            // most likely the user is running this headless
            work();
        }
    }

    /**
     * This is a crude catch-all exit. Feel free to improve!
     */
    public static void fatal() {
        String msg = "Sorry, this attempt failed. Turn off your computer and seek human help, you might indeed have a real issue.";
        System.err.println(msg);
        JOptionPane.showConfirmDialog(null, msg);
        System.exit(1);
    }

    public static void toClipboard(String... output) {
        StringBuilder combinedText = new StringBuilder();
        if (debug) {
            combinedText.append("*** the tool is being tested *** the output below is a test ***");
            combinedText.append(System.lineSeparator());
        }
        if (null != output) {
            for (String text : output) {
                combinedText.append(null == text ? "(null)" : text);
            }
        }
        if (debug) {
            combinedText.append(System.lineSeparator());
            combinedText.append("*** the tool is being tested; just report ***");
            combinedText.append(System.lineSeparator());
        }
        if (combinedText.length() > MAX_OUTPUT_SIZE) {
            combinedText.setLength(MAX_OUTPUT_SIZE);
            combinedText.append(System.lineSeparator());
            combinedText.append("Truncated at " + MAX_OUTPUT_SIZE);
        }
        String finalText = combinedText.toString();
        if (debug) {
            System.out.println("Sending [[" + finalText + "]] to clipboard");
        }

        // Now, place the finalText on the clipboard
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection selection = new StringSelection(finalText);
        clipboard.setContents(selection, selection);
        lastText = finalText;
        waitingForLLM = true;
    }

    public static void toClipboard(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try ( BufferedReader bfr = new BufferedReader(new InputStreamReader(is))) {
            for (String s = bfr.readLine(); null != s; s = bfr.readLine()) {
                if (0 < sb.length()) {
                    sb.append(System.lineSeparator());
                }
                sb.append(s);
            }
        }
        toClipboard(sb.toString());
    }

    public static void work() {
        try {
            if (osName.contains("win")) {
                try ( InputStream is = Thermopylae.class.getClassLoader().getResourceAsStream("windows.txt")) {
                    toClipboard(is);
                }
            } else if (osName.contains("nix") || osName.contains("nux")) {
                try ( InputStream is = Thermopylae.class.getClassLoader().getResourceAsStream("linux.txt")) {
                    toClipboard(is);
                }
            } else if (osName.contains("mac")) {
                try ( InputStream is = Thermopylae.class.getClassLoader().getResourceAsStream("mac.txt")) {
                    toClipboard(is);
                }
            } else {
                fatal();
            }
        } catch (Exception ex) {
            fatal();
        }
    }

    public static String handleCommand(final String cmdString) {
        if (debug) {
            System.out.println("Executing [[" + cmdString + "]]");
        }
        final StringBuilder output = new StringBuilder();
        try {
            String[] commandArray;
            if (osName.contains("win")) {
                commandArray = new String[]{"cmd.exe", "/c", cmdString};
            } else {
                commandArray = new String[]{"/bin/bash", "-c", cmdString};
            }

            ProcessBuilder pb = new ProcessBuilder(commandArray);
            pb.redirectErrorStream(true);
            final Process process = pb.start();

            Thread outputThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try ( BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        Logger.getLogger(Thermopylae.class.getName()).log(Level.SEVERE, null, e);
                        output.append("\nYour command ").append(cmdString).append(" caused exception ").append(e.getMessage());
                    }
                }
            });
            outputThread.start();

            if (process.waitFor(10, TimeUnit.SECONDS)) {
                outputThread.join(); // Wait for the output reading thread to finish
            } else {
                process.destroy();
                output.append("\nYour command ").append(cmdString).append(" timed out");
            }
        } catch (Exception ex) {
            Logger.getLogger(Thermopylae.class.getName()).log(Level.SEVERE, null, ex);
            output.append("\nYour command ").append(cmdString).append(" caused exception ").append(ex.getMessage());
        }
        return output.toString();
    }

    public static void setup() throws Exception {
        try ( InputStream is = Thermopylae.class.getClassLoader().getResourceAsStream("userIntro.html")) {
            try ( DataInputStream dis = new DataInputStream(is)) {
                byte[] bytes = dis.readAllBytes();
                JDialog dialog = new JDialog();
                dialog.setTitle("Thermopylae, first aid responder");
                dialog.setAlwaysOnTop(true);
                dialog.setLayout(new BorderLayout());
                dialog.getContentPane().add(new JLabel(new String(bytes, StandardCharsets.UTF_8)), BorderLayout.CENTER);
                Box bottom = Box.createHorizontalBox();
                bottom.add(new JButton(new AbstractAction("Help!") {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        work();
                    }
                }));
                bottom.add(Box.createHorizontalGlue());
                bottom.add(new JButton(new AbstractAction("Test.") {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        debug = true;
                        work();
                    }
                }));
                bottom.add(Box.createHorizontalGlue());
                bottom.add(new JButton(new AbstractAction("Sorry, I am fine, there is no problem.") {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        System.exit(0);
                    }
                }));
                dialog.getContentPane().add(bottom, BorderLayout.SOUTH);
                dialog.pack();
                EventQueue.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setVisible(true);
                    }
                });
                new Timer(500, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        if (waitingForLLM) {
                            if (dialog.isVisible()) {
                                dialog.dispose();
                            }
                            Transferable contents = clipboard.getContents(null);
                            if (contents != null) {
                                // Handle text data
                                if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                                    try {
                                        String currentText = (String) contents.getTransferData(DataFlavor.stringFlavor);
                                        if (!currentText.equals(lastText)) {
                                            if (currentText.length() < MAX_COMMAND_SIZE) {
                                                toClipboard(handleCommand(currentText));
                                            } else {
                                                toClipboard("Commands exceeded ", "" + MAX_COMMAND_SIZE, " characters");
                                            }
                                        }
                                    } catch (Exception ex) {
                                        // this happens fairly frequently; no real handling possible, can only ignore
                                    }
                                }
                            }
                        }
                    }
                }).start();
            }
        }
    }
}
