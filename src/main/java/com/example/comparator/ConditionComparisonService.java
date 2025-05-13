
package com.example.comparator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ConditionComparisonService {

    @Autowired
    private ConditionFileRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    public List<ComparisonResult> compareConditions() throws JsonProcessingException {
        List<ConditionFile> files = repository.findAll();
        Map<String, Set<String>> normalizedMap = new HashMap<>();

        for (ConditionFile file : files) {
            EventConfigurationDTO dto = objectMapper.readValue(file.getJsonContent(), EventConfigurationDTO.class);
            Set<String> normalizedSet = normalizeConditionGroup(dto.getConditionGroup());
            normalizedMap.put(file.getFileName(), normalizedSet);
        }

        List<ComparisonResult> results = new ArrayList<>();
        List<String> fileNames = new ArrayList<>(normalizedMap.keySet());

        for (int i = 0; i < fileNames.size(); i++) {
            for (int j = i + 1; j < fileNames.size(); j++) {
                String file1 = fileNames.get(i);
                String file2 = fileNames.get(j);
                Set<String> set1 = normalizedMap.get(file1);
                Set<String> set2 = normalizedMap.get(file2);

                if (set1.equals(set2)) {
                    results.add(new ComparisonResult(file1, file2, "IDENTICAL"));
                } else if (set1.containsAll(set2)) {
                    results.add(new ComparisonResult(file1, file2, "SUBSET"));
                } else if (set2.containsAll(set1)) {
                    results.add(new ComparisonResult(file2, file1, "SUBSET"));
                } else {
                    results.add(new ComparisonResult(file1, file2, "DIFFERENT"));
                }
            }
        }

        return results;
    }

    private Set<String> normalizeConditionGroup(ConditionGroupConfigurationDTO group) throws JsonProcessingException {
        Set<String> normalizedSet = new HashSet<>();
        if (group != null) {
            Map<String, Object> normalized = new TreeMap<>();
            normalized.put("fieldName", group.getFieldName());
            normalized.put("validationCondition", group.getValidationCondition());
            if (group.getFieldValues() != null) {
                List<String> sortedValues = new ArrayList<>(group.getFieldValues());
                Collections.sort(sortedValues);
                normalized.put("fieldValues", sortedValues);
            }
            normalizedSet.add(objectMapper.writeValueAsString(normalized));
            normalizedSet.addAll(normalizeConditionGroup(group.getConditionGroup()));
        }
        return normalizedSet;
    }
}
