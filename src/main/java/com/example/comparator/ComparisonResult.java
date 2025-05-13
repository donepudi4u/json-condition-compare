
package com.example.comparator;

public class ComparisonResult {
    private String file1;
    private String file2;
    private String result;

    public ComparisonResult(String file1, String file2, String result) {
        this.file1 = file1;
        this.file2 = file2;
        this.result = result;
    }

    public String getFile1() { return file1; }
    public void setFile1(String file1) { this.file1 = file1; }

    public String getFile2() { return file2; }
    public void setFile2(String file2) { this.file2 = file2; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
