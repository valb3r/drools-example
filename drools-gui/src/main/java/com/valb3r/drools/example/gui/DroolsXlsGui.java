package com.valb3r.drools.example.gui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.formdev.flatlaf.FlatLightLaf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DroolsXlsGui {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JTable csvPreviewTable;
    private JButton loadCsvButton;
    private JTable mappedResultTable;
    private JButton loadTransformFile;
    private JButton executeTransformButton;
    private JLabel transformFilePathLabel;
    private JLabel csvFilePathLabel;
    private JPanel dialogMainPanel;


    private final List<Map<String, Object>> input = new ArrayList<>();
    private final List<Map<String, Object>> output = new ArrayList<>();

    public DroolsXlsGui() {
        executeTransformButton.setEnabled(false);
        csvPreviewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        mappedResultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        loadCsvButton.addActionListener(e -> selectAndReadInputCsv());
        loadTransformFile.addActionListener(e -> {
            var file = chooseFile("xls", "xlsx");
            if (null != file) {
                transformFilePathLabel.setText(file.getAbsolutePath());
                executeTransformButton.setEnabled(true);
            }
        });

        executeTransformButton.addActionListener(e -> executeTransform());
    }

    @SneakyThrows
    public static void main(String[] args) {
        FlatLightLaf.install();
        JFrame frame = new JFrame("Drools mapper example");
        DroolsXlsGui dialog = new DroolsXlsGui();
        frame.setContentPane(dialog.dialogMainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(800, 500);
        frame.setVisible(true);
    }

    @SneakyThrows
    private void selectAndReadInputCsv() {
        var file = chooseFile("csv");
        if (null == file) {
            return;
        }

        this.csvFilePathLabel.setText(file.getAbsolutePath());

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator(';').withHeader();
        var model = new DefaultTableModel();

        this.input.clear();
        try (var reader = new BufferedReader(new FileReader(file))) {
            MappingIterator<Map<String, Object>> it = mapper.readerFor(new TypeReference<Map<String, Object>>() {})
                    .with(schema)
                    .readValues(reader);

            String[] header = null;
            while (it.hasNext()) {
                var data = it.next();
                var asMap = MAPPER.convertValue(data, new TypeReference<Map<String, Object>>() {});
                if (null == header) {
                    header = asMap.keySet().stream().sorted().toArray(String[]::new);
                    model.setColumnIdentifiers(header);
                }
                input.add(asMap);
                model.addRow(Arrays.stream(header).map(asMap::get).map(String::valueOf).toArray());
            }
        }

        csvPreviewTable.setModel(model);
        model.fireTableDataChanged();
    }

    @SneakyThrows
    private void executeTransform() {
        KieServices services = KieServices.Factory.get();
        KieFileSystem fileSystem = services.newKieFileSystem();
        var droolsXlsFilePath = transformFilePathLabel.getText();
        fileSystem.write("src/main/resources/" + droolsXlsFilePath + ".drl.xlsx", Files.readAllBytes(Path.of(droolsXlsFilePath)));
        KieBuilder kb = services.newKieBuilder(fileSystem);
        kb.buildAll();
        KieModule kieModule = kb.getKieModule();
        var container = services.newKieContainer(kieModule.getReleaseId());
        var model = csvPreviewTable.getModel();

        this.output.clear();
        for (int row = 0; row < model.getRowCount(); ++row) {
            var session = container.newStatelessKieSession();
            var droolsMappedOutput = new HashMap<String, Object>();
            session.execute(List.of(new Input(input.get(row)), new Output(droolsMappedOutput)));
            this.output.add(droolsMappedOutput);
        }

        updateResultTable(this.output);
    }

    private void updateResultTable(List<Map<String, Object>> output) {
        var resultModel = new DefaultTableModel();
        var orderedCols = output.stream().flatMap(it -> it.keySet().stream()).distinct().sorted().toArray(String[]::new);
        resultModel.setColumnIdentifiers(orderedCols);

        for (var res : output) {
            var mappedToTable = new ArrayList<String>();
            for (String orderedCol : orderedCols) {
                var value = res.get(orderedCol);
                mappedToTable.add(null == value ? null : String.valueOf(value));
            }
            resultModel.addRow(mappedToTable.toArray());
        }
        mappedResultTable.setModel(resultModel);
        resultModel.fireTableDataChanged();
    }

    private File chooseFile(String... extensions) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                Arrays.stream(extensions).map(it -> "*." + it).collect(Collectors.joining(",")),
                extensions)
        );
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        int result = fileChooser.showOpenDialog(this.dialogMainPanel);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }

        return null;
    }

    @Data
    @AllArgsConstructor
    public static class Input implements Map<String, Object> {

        @Delegate
        private Map<String, Object> data;
    }

    @Data
    @AllArgsConstructor
    public static class Output implements Map<String, Object> {

        @Delegate
        private Map<String, Object> data;
    }
}
