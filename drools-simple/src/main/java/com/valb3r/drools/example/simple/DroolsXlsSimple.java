package com.valb3r.drools.example.simple;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DroolsXlsSimple {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SneakyThrows
    public static void main(String[] args) {
        var csv = readInputCsv(new File("drools-simple/src/main/resources/example.csv"));

        // Build Drools services and container
        KieServices services = KieServices.Factory.get();
        KieFileSystem fileSystem = services.newKieFileSystem();
        var droolsXlsFilePath = "drools-simple/src/main/resources/taxes-rules.xlsx"; // XLSX file with rules
        // Write XLSX file into Drools working memory:
        fileSystem.write("src/main/resources/" + droolsXlsFilePath + ".drl.xlsx", Files.readAllBytes(Path.of(droolsXlsFilePath)));
        // Create Kie module:
        KieBuilder kb = services.newKieBuilder(fileSystem);
        kb.buildAll();
        KieModule kieModule = kb.getKieModule();
        var container = services.newKieContainer(kieModule.getReleaseId()); // We can use this container for rule execution

        System.out.println();
        System.out.println();
        System.out.println("==================== Drools mapping is: ====================");

        for (Map<String, Object> stringObjectMap : csv) {
            var session = container.newStatelessKieSession(); // Create new session for each row. Since session is stateless, you can move this out of loop and share session with every object
            var droolsMappedOutput = new HashMap<String, Object>();
            session.execute(List.of(new Input(stringObjectMap), new Output(droolsMappedOutput)));
            System.out.printf("For %s %n", stringObjectMap);
            System.out.printf("Result is %s %n", droolsMappedOutput);
        }
    }

    @SneakyThrows
    private static ArrayList<Map<String, Object>> readInputCsv(File file) {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator(';').withHeader();

        var result = new ArrayList<Map<String, Object>>();
        try (var reader = new BufferedReader(new FileReader(file))) {
            MappingIterator<Map<String, Object>> it = mapper.readerFor(new TypeReference<Map<String, Object>>() {})
                    .with(schema)
                    .readValues(reader);

            while (it.hasNext()) {
                var data = it.next();
                var asMap = MAPPER.convertValue(data, new TypeReference<Map<String, Object>>() {});
                result.add(asMap);
            }
        }

        return result;
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
