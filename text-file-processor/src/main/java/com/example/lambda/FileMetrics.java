package com.example.lambda;

public class FileMetrics {
    private String fileKey;
    private String bucketName;
    private long uploadTimestamp;
    private int lineCount;
    private int wordCount;
    private int characterCount;
    private long fileSize;

    public FileMetrics(String fileKey, String bucketName, long uploadTimestamp,
                      int lineCount, int wordCount, int characterCount, long fileSize) {
        this.fileKey = fileKey;
        this.bucketName = bucketName;
        this.uploadTimestamp = uploadTimestamp;
        this.lineCount = lineCount;
        this.wordCount = wordCount;
        this.characterCount = characterCount;
        this.fileSize = fileSize;
    }

    // Getters
    public String getFileKey() { return fileKey; }
    public String getBucketName() { return bucketName; }
    public long getUploadTimestamp() { return uploadTimestamp; }
    public int getLineCount() { return lineCount; }
    public int getWordCount() { return wordCount; }
    public int getCharacterCount() { return characterCount; }
    public long getFileSize() { return fileSize; }
}