
package com.example.comparator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ComparisonController {

    @Autowired
    private ConditionComparisonService comparisonService;

    @GetMapping("/compare")
    public ResponseEntity<Resource> downloadComparisonReport() throws JsonProcessingException {
        List<ComparisonResult> results = comparisonService.compareConditions();
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(results);
        ByteArrayResource resource = new ByteArrayResource(json.getBytes());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=comparison_report.json")
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(json.length())
                .body(resource);
    }
}
