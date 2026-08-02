package dev.qcarver.ballsack.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import org.apache.batik.swing.JSVGCanvas;

public final class BallsackGuiApp {
    private final JFrame frame;
    private final JSVGCanvas canvas;
    private final JLabel status;
    private Path lastInput;
    private Path lastSvg;

    private BallsackGuiApp() {
        this.frame = new JFrame("Ballsack Java GUI");
        this.canvas = new JSVGCanvas();
        this.status = new JLabel("Drop xml/json file or folder.");
        configureUi();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BallsackGuiApp app = new BallsackGuiApp();
            app.frame.setVisible(true);
            if (args.length > 0) {
                app.loadFromPathAsync(Paths.get(args[0]));
            }
        });
    }

    private void configureUi() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(new Dimension(1200, 850));

        canvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
        canvas.setEnableImageZoomInteractor(true);
        canvas.setEnablePanInteractor(true);
        canvas.setEnableZoomInteractor(true);
        canvas.setEnableResetTransformInteractor(true);

        canvas.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    Transferable t = support.getTransferable();
                    @SuppressWarnings("unchecked")
                    List<java.io.File> files = (List<java.io.File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    if (files == null || files.isEmpty()) {
                        return false;
                    }
                    loadFromPathAsync(files.get(0).toPath());
                    return true;
                } catch (Exception e) {
                    showError("Drop failed", e.getMessage());
                    return false;
                }
            }
        });

        JPanel toolbar = new JPanel();
        JButton openFile = new JButton("Open...");
        JButton openFolder = new JButton("Open Folder...");
        JButton reload = new JButton("Reload");
        JButton save = new JButton("Save SVG As...");

        openFile.addActionListener(evt -> chooseFile());
        openFolder.addActionListener(evt -> chooseFolder());
        reload.addActionListener(evt -> {
            if (lastInput == null) {
                status.setText("Nothing to reload.");
                return;
            }
            loadFromPathAsync(lastInput);
        });
        save.addActionListener(evt -> saveSvgAs());

        toolbar.add(openFile);
        toolbar.add(openFolder);
        toolbar.add(reload);
        toolbar.add(save);

        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(canvas, BorderLayout.CENTER);
        frame.add(status, BorderLayout.SOUTH);
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open XML or JSON");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            loadFromPathAsync(chooser.getSelectedFile().toPath());
        }
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            loadFromPathAsync(chooser.getSelectedFile().toPath());
        }
    }

    private void saveSvgAs() {
        if (lastSvg == null || !Files.exists(lastSvg)) {
            status.setText("No SVG to save yet.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save SVG");
        chooser.setSelectedFile(new java.io.File("ballsack.svg"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path dest = chooser.getSelectedFile().toPath();
        try {
            Files.copy(lastSvg, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            status.setText("Saved " + dest);
        } catch (IOException e) {
            showError("Save failed", e.getMessage());
        }
    }

    private void loadFromPathAsync(Path input) {
        status.setText("Loading " + input + " ...");
        new SwingWorker<Path, Void>() {
            @Override
            protected Path doInBackground() throws Exception {
                return runBallsackCli(input);
            }

            @Override
            protected void done() {
                try {
                    Path svg = get();
                    lastInput = input.toAbsolutePath().normalize();
                    lastSvg = svg;
                    canvas.setURI(svg.toUri().toString());
                    status.setText("Loaded " + lastInput);
                } catch (Exception e) {
                    showError("Load failed", rootMessage(e));
                    status.setText("Load failed");
                }
            }
        }.execute();
    }

    private Path runBallsackCli(Path input) throws Exception {
        Path repoRoot = detectRepoRoot();
        Path outputDir = Files.createTempDirectory("ballsack-java-gui-");
        Path outputSvg = outputDir.resolve("latest.svg");

        String python = detectPython(repoRoot);
        List<String> command = new ArrayList<>();
        command.add(python);
        command.add("-m");
        command.add("ballsack.cli");
        command.add("--input");
        command.add(input.toAbsolutePath().normalize().toString());
        command.add("--output");
        command.add(outputSvg.toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(repoRoot.toFile());

        String currentPyPath = System.getenv("PYTHONPATH");
        StringBuilder pyPath = new StringBuilder(repoRoot.resolve("src").toString());
        Path bnfoRoot = Paths.get(System.getProperty("user.home"), "Dev", "BNF_Obj");
        pyPath.append(":" + bnfoRoot.toString());
        pyPath.append(":" + bnfoRoot.resolve("src"));
        if (currentPyPath != null && !currentPyPath.isBlank()) {
            pyPath.append(":" + currentPyPath);
        }
        pb.environment().put("PYTHONPATH", pyPath.toString());

        // Directory imports require TREE_UI_FILE. Respect user override first,
        // then fall back to the bundled shim in java_gui/tree_ui.py.
        if (Files.isDirectory(input)) {
            String treeUiFromEnv = System.getenv("TREE_UI_FILE");
            if (treeUiFromEnv == null || treeUiFromEnv.isBlank()) {
                Path bundledTreeUi = repoRoot.resolve("java_gui").resolve("tree_ui.py");
                if (!Files.exists(bundledTreeUi)) {
                    throw new IllegalStateException(
                        "Directory import requires TREE_UI_FILE, and bundled tree_ui.py was not found at " + bundledTreeUi
                    );
                }
                pb.environment().put("TREE_UI_FILE", bundledTreeUi.toString());
            } else {
                pb.environment().put("TREE_UI_FILE", treeUiFromEnv);
            }
        }

        Process process = pb.start();
        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());
        int code = process.waitFor();
        if (code != 0) {
            String msg = "ballsack.cli failed with code " + code;
            if (!stderr.isBlank()) {
                msg += "\n" + stderr;
            } else if (!stdout.isBlank()) {
                msg += "\n" + stdout;
            }
            throw new IllegalStateException(msg);
        }

        if (!Files.exists(outputSvg)) {
            throw new IllegalStateException("ballsack.cli did not produce SVG output.");
        }

        return outputSvg;
    }

    private static String detectPython(Path repoRoot) {
        String fromEnv = System.getenv("BALLSACK_PYTHON");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        Path venvPy = repoRoot.resolve(".venv").resolve("bin").resolve("python");
        if (Files.isExecutable(venvPy)) {
            return venvPy.toString();
        }
        return "python3";
    }

    private static Path detectRepoRoot() {
        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.exists(cwd.resolve("pyproject.toml")) && Files.exists(cwd.resolve("src"))) {
            return cwd;
        }

        Path probe = cwd;
        for (int i = 0; i < 5; i++) {
            probe = probe.getParent();
            if (probe == null) {
                break;
            }
            if (Files.exists(probe.resolve("pyproject.toml")) && Files.exists(probe.resolve("src"))) {
                return probe;
            }
        }
        return cwd;
    }

    private static String readStream(java.io.InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage() == null ? cur.toString() : cur.getMessage();
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
