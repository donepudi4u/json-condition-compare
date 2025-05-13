
package com.example.comparator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class ConditionFile {
    @Id
    private Long id;
    private String fileName;
    @Lob
    private String jsonContent;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getJsonContent() { return jsonContent; }
    public void setJsonContent(String jsonContent) { this.jsonContent = jsonContent; }
}
