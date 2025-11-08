package com.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TextFileProcessor implements RequestHandler<S3Event, String> {

    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private static final String DYNAMODB_TABLE = "TextFileMetrics";

    public TextFileProcessor() {
        // Use ap-south-1 region (Mumbai)
        this.s3Client = S3Client.builder()
                .region(Region.AP_SOUTH_1)
                .build();
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTH_1)
                .build();
    }

    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        LambdaLogger logger = context.getLogger();
        
        try {
            for (S3EventNotificationRecord record : s3Event.getRecords()) {
                String bucketName = record.getS3().getBucket().getName();
                String fileKey = record.getS3().getObject().getUrlDecodedKey();
                
                logger.log("Processing file: " + fileKey + " from bucket: " + bucketName);
                
                // Check if file is a .txt file
                if (!fileKey.toLowerCase().endsWith(".txt")) {
                    logger.log("Skipping non-txt file: " + fileKey);
                    continue;
                }
                
                // Read file from S3
                FileMetrics metrics = processTextFile(bucketName, fileKey, logger);
                
                // Store metrics in DynamoDB
                storeToDynamoDB(metrics, logger);
                
                logger.log(String.format("Successfully processed file: %s - Lines: %d, Words: %d, Characters: %d",
                        fileKey, metrics.getLineCount(), metrics.getWordCount(), metrics.getCharacterCount()));
            }
            
            return "Success";
            
        } catch (Exception e) {
            logger.log("Error processing file: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private FileMetrics processTextFile(String bucketName, String fileKey, LambdaLogger logger) 
            throws IOException {
        
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
        long fileSize = s3Object.response().contentLength();
        
        int lineCount = 0;
        int wordCount = 0;
        int characterCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(s3Object, StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                characterCount += line.length();
                
                // Count words (split by whitespace)
                String[] words = line.trim().split("\\s+");
                if (!line.trim().isEmpty()) {
                    wordCount += words.length;
                }
            }
        }

        long uploadTimestamp = System.currentTimeMillis();
        
        return new FileMetrics(fileKey, bucketName, uploadTimestamp, 
                              lineCount, wordCount, characterCount, fileSize);
    }

    private void storeToDynamoDB(FileMetrics metrics, LambdaLogger logger) {
        Map<String, AttributeValue> item = new HashMap<>();
        
        item.put("fileKey", AttributeValue.builder().s(metrics.getFileKey()).build());
        item.put("uploadTimestamp", AttributeValue.builder().n(String.valueOf(metrics.getUploadTimestamp())).build());
        item.put("bucketName", AttributeValue.builder().s(metrics.getBucketName()).build());
        item.put("lineCount", AttributeValue.builder().n(String.valueOf(metrics.getLineCount())).build());
        item.put("wordCount", AttributeValue.builder().n(String.valueOf(metrics.getWordCount())).build());
        item.put("characterCount", AttributeValue.builder().n(String.valueOf(metrics.getCharacterCount())).build());
        item.put("fileSize", AttributeValue.builder().n(String.valueOf(metrics.getFileSize())).build());

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(DYNAMODB_TABLE)
                .item(item)
                .build();

        dynamoDbClient.putItem(putItemRequest);
        logger.log("Stored metrics to DynamoDB for file: " + metrics.getFileKey());
    }
}