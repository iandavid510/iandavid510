import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import org.biojava.nbio.core.sequence.io.GenbankReaderHelper;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.features.*;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class GenBankViewer extends JFrame {

    private JTextArea outputArea;
    private JButton openFileButton, saveFileButton;
    private JPanel mapPanel;
    private List<FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound>> features;
    private int maxLength = 10000; // Dynamically updated based on sequence length

    public GenBankViewer() {
        // Set up the JFrame
        super("GenBank Viewer");
        setSize(1000, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Set up layout
        setLayout(new BorderLayout());

        // Text area to display file contents
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER); // Put in the center for better visibility

        // File selection button
        openFileButton = new JButton("Open GenBank File");
        saveFileButton = new JButton("Save to Text File");

        JPanel topPanel = new JPanel();
        topPanel.add(openFileButton);
        topPanel.add(saveFileButton);
        add(topPanel, BorderLayout.NORTH);

        // File chooser action listener
        openFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openGenBankFile();
            }
        });

        // Save file button action listener
        saveFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveToFile();
            }
        });

        // Display the window
        setVisible(true);
    }

    // Open and parse GenBank file
    private void openGenBankFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            // Parse the file using BioJava
            System.out.println("Opening file: " + selectedFile.getName());
            parseWithBioJava(selectedFile);
        }
    }

    // Use BioJava to parse the GenBank file and display information
    private void parseWithBioJava(File genbankFile) {
        try {
            System.out.println("Parsing GenBank file...");
            // Parse GenBank file using BioJava
            Map<String, DNASequence> dnaSequences = GenbankReaderHelper.readGenbankDNASequence(genbankFile);

            // Clear JTextArea before appending new content
            outputArea.setText("");  // Clear previous text

            // Display the parsed data in the text area
            StringBuilder displayContent = new StringBuilder();

            features = new ArrayList<>(); // Store parsed features for interactive map

            // Iterate through sequences
            for (Map.Entry<String, DNASequence> entry : dnaSequences.entrySet()) {
                System.out.println("Processing sequence: " + entry.getKey());
                displayContent.append("**Accession**: ").append(entry.getKey()).append("\n\n");

                // DNA Section
                displayContent.append("--- DNA ---\n");
                String dnaSequence = entry.getValue().getSequenceAsString();
                displayContent.append(formatSequence(dnaSequence, 60)).append("\n");
                displayContent.append("Sequence Length: ").append(entry.getValue().getLength()).append("\n\n");

                // Set maxLength dynamically from sequence length
                maxLength = entry.getValue().getLength();

                // Features Section
                displayContent.append("--- Features ---\n");
                AbstractSequence<NucleotideCompound> seq = entry.getValue();  // DNASequence uses NucleotideCompound

                for (FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound> feature : seq.getFeatures()) {
                    features.add(feature); // Add features to be displayed later

                    String type = feature.getType();
                    String location = feature.getLocations().toString();

                    // Extract annotations (like gene names) from the feature's qualifiers
                    String geneName = extractQualifier(feature, "gene");

                    // Ensure "N/A" is shown if the gene name is null
                    String geneDisplayName = (geneName == null || geneName.isEmpty()) ? "N/A" : geneName;

                    // Display feature information in the JTextArea
                    displayContent.append(type)
                            .append(": Location: ").append(location)
                            .append(", Gene: ").append(geneDisplayName)
                            .append("\n");

                    System.out.println("Feature: " + type + ", Location: " + location + ", Gene: " + geneDisplayName);
                }

                // Append accession and other related information
                displayContent.append("\n--- Accession Information ---\n");
                displayContent.append("Version: 1.0\nType: DNA\n\n");
            }

            // Append to JTextArea
            outputArea.append(displayContent.toString());

            // Revalidate and repaint the JTextArea to make sure it's updated
            outputArea.revalidate();
            outputArea.repaint();

            System.out.println("Data successfully appended to GUI");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error parsing file: " + e.getMessage());
        }
    }

    // Utility to format sequences by breaking them into fixed-width lines
    private String formatSequence(String sequence, int lineLength) {
        StringBuilder formattedSequence = new StringBuilder();
        for (int i = 0; i < sequence.length(); i += lineLength) {
            int end = Math.min(sequence.length(), i + lineLength);
            formattedSequence.append(sequence, i, end).append("\n");
        }
        return formattedSequence.toString();
    }

    // Extract a qualifier (such as gene name) from the feature
    private String extractQualifier(FeatureInterface<?, ?> feature, String qualifierName) {
        Map<String, List<Qualifier>> qualifiers = feature.getQualifiers();
        if (qualifiers.containsKey(qualifierName)) {
            List<Qualifier> qualifierList = qualifiers.get(qualifierName);
            if (!qualifierList.isEmpty()) {
                return qualifierList.get(0).getValue();  // Get the value from the Qualifier object
            }
        }
        return null;
    }

    // Save the content of the JTextArea to a file
    private void saveToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Text Output");
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(outputArea.getText());  // Save the JTextArea content to the file
                JOptionPane.showMessageDialog(this, "File saved successfully: " + file.getName());
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GenBankViewer();
            }
        });
    }
}
