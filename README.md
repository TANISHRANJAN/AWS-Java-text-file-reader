# AWS-Java-text-file-reader
An AWS Lambda-based serverless application that automatically processes text files uploaded to S3, analyzes their content, and stores metrics in DynamoDB.


## 🚀 Features

- **Automatic Processing**: Triggers automatically when `.txt` files are uploaded to S3
- **Content Analysis**: Counts lines, words, and characters in text files
- **Persistent Storage**: Stores file metrics in DynamoDB for later retrieval
- **Serverless Architecture**: No server management required
- **Cost-Effective**: Pay only for what you use

## 📋 Architecture

```
S3 Bucket (.txt upload)
    ↓
S3 Event Notification
    ↓
Lambda Function (TextFileProcessor)
    ↓
Read & Process File
    ↓
DynamoDB Table (TextFileMetrics)
```

## 🛠️ Technology Stack

- **Runtime**: Java 17
- **Cloud Provider**: AWS
- **Services Used**:
  - AWS Lambda
  - Amazon S3
  - Amazon DynamoDB
  - AWS IAM
  - Amazon CloudWatch
- **Build Tool**: Maven 3.9+
- **SDK**: AWS SDK for Java v2

## 📦 Prerequisites

- AWS Account with appropriate permissions
- Java 17 or higher
- Maven 3.6 or higher
- AWS CLI (optional, for deployment)

## 🏗️ Project Structure

```
text-file-processor/
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── lambda/
                        ├── TextFileProcessor.java
                        └── FileMetrics.java
```

## 📝 Setup Instructions

### 1. Create DynamoDB Table

```bash
aws dynamodb create-table \
    --table-name TextFileMetrics \
    --attribute-definitions \
        AttributeName=fileKey,AttributeType=S \
        AttributeName=uploadTimestamp,AttributeType=N \
    --key-schema \
        AttributeName=fileKey,KeyType=HASH \
        AttributeName=uploadTimestamp,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --region ap-south-1
```

**Table Schema:**
- **Partition Key**: `fileKey` (String)
- **Sort Key**: `uploadTimestamp` (Number)

### 2. Create IAM Role

Create an IAM role named `TextFileProcessorLambdaRole` with the following policies:

**Managed Policies:**
- `AWSLambdaBasicExecutionRole`

**Inline Policy:**
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::YOUR-BUCKET-NAME/*",
                "arn:aws:s3:::YOUR-BUCKET-NAME"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:PutItem",
                "dynamodb:UpdateItem"
            ],
            "Resource": "arn:aws:dynamodb:ap-south-1:YOUR-ACCOUNT-ID:table/TextFileMetrics"
        }
    ]
}
```

### 3. Create S3 Bucket

```bash
aws s3 mb s3://YOUR-BUCKET-NAME --region ap-south-1
```

### 4. Build the Project

```bash
# Clone the repository
git clone https://github.com/YOUR-USERNAME/text-file-processor.git
cd text-file-processor

# Build the JAR file
mvn clean package

# The JAR will be created at: target/text-file-processor-1.0.0.jar
```

### 5. Deploy Lambda Function

```bash
aws lambda create-function \
    --function-name TextFileProcessor \
    --runtime java17 \
    --role arn:aws:iam::YOUR-ACCOUNT-ID:role/TextFileProcessorLambdaRole \
    --handler com.example.lambda.TextFileProcessor::handleRequest \
    --zip-file fileb://target/text-file-processor-1.0.0.jar \
    --timeout 60 \
    --memory-size 512 \
    --region ap-south-1
```

### 6. Configure S3 Event Notification

Add an event notification to your S3 bucket:

```bash
aws s3api put-bucket-notification-configuration \
    --bucket YOUR-BUCKET-NAME \
    --notification-configuration file://notification.json
```

**notification.json:**
```json
{
    "LambdaFunctionConfigurations": [
        {
            "LambdaFunctionArn": "arn:aws:lambda:ap-south-1:YOUR-ACCOUNT-ID:function:TextFileProcessor",
            "Events": ["s3:ObjectCreated:Put"],
            "Filter": {
                "Key": {
                    "FilterRules": [
                        {
                            "Name": "suffix",
                            "Value": ".txt"
                        }
                    ]
                }
            }
        }
    ]
}
```

## 🎯 Usage

1. **Upload a text file to S3:**
   ```bash
   aws s3 cp sample.txt s3://YOUR-BUCKET-NAME/
   ```

2. **Lambda automatically processes the file**

3. **View results in DynamoDB:**
   ```bash
   aws dynamodb scan --table-name TextFileMetrics --region ap-south-1
   ```

## 📊 Output Format

The Lambda function stores the following metrics in DynamoDB:

| Field | Type | Description |
|-------|------|-------------|
| `fileKey` | String | S3 object key (filename) |
| `uploadTimestamp` | Number | Unix timestamp (milliseconds) |
| `bucketName` | String | Source S3 bucket name |
| `lineCount` | Number | Total number of lines |
| `wordCount` | Number | Total number of words |
| `characterCount` | Number | Total number of characters |
| `fileSize` | Number | File size in bytes |

**Example DynamoDB Item:**
```json
{
    "fileKey": "sample.txt",
    "uploadTimestamp": 1699456789123,
    "bucketName": "my-text-bucket",
    "lineCount": 42,
    "wordCount": 256,
    "characterCount": 1847,
    "fileSize": 2048
}
```

## 🔍 Monitoring

View Lambda execution logs in CloudWatch:

```bash
aws logs tail /aws/lambda/TextFileProcessor --follow --region ap-south-1
```

## 🧪 Testing

1. Create a test text file:
   ```bash
   echo -e "Hello World\nThis is a test\nWith multiple lines" > test.txt
   ```

2. Upload to S3:
   ```bash
   aws s3 cp test.txt s3://YOUR-BUCKET-NAME/
   ```

3. Verify in DynamoDB:
   ```bash
   aws dynamodb get-item \
       --table-name TextFileMetrics \
       --key '{"fileKey": {"S": "test.txt"}, "uploadTimestamp": {"N": "TIMESTAMP"}}' \
       --region ap-south-1
   ```

## ⚙️ Configuration

### Changing AWS Region

Update the region in `TextFileProcessor.java`:

```java
this.s3Client = S3Client.builder()
        .region(Region.YOUR_REGION) // e.g., Region.US_EAST_1
        .build();
```

### Adjusting Lambda Settings

- **Memory**: 512 MB (adjustable based on file size)
- **Timeout**: 60 seconds (increase for very large files)
- **Runtime**: Java 17

## 🐛 Troubleshooting

### Common Issues

**Issue**: `ClassNotFoundException`
- **Solution**: Ensure JAR is built with Maven Shade plugin and all dependencies are included

**Issue**: `S3Exception: 307 Redirect`
- **Solution**: Verify Lambda and S3 bucket are in the same AWS region

**Issue**: `Access Denied` errors
- **Solution**: Check IAM role permissions for S3 and DynamoDB access

**Issue**: Lambda timeout
- **Solution**: Increase timeout value for large files

### Viewing Logs

```bash
# Tail recent logs
aws logs tail /aws/lambda/TextFileProcessor --follow

# View specific log stream
aws logs get-log-events \
    --log-group-name /aws/lambda/TextFileProcessor \
    --log-stream-name 'STREAM_NAME'
```

## 💰 Cost Estimation

**Assumptions**: 1,000 text file uploads per month, average 100KB file size

- **Lambda**: ~$0.20/month (512MB, 3 seconds per execution)
- **S3**: ~$0.02/month (storage + requests)
- **DynamoDB**: ~$0.25/month (on-demand pricing)
- **CloudWatch Logs**: ~$0.50/month

**Total**: ~$1.00/month

## 🚦 Limitations

- Maximum file size: Limited by Lambda's 6 MB payload size for synchronous invocation
- Timeout: 15 minutes maximum (Lambda limit)
- Text files only: Binary files are not supported
- Encoding: UTF-8 assumed by default

## 🔐 Security Best Practices

- ✅ Use IAM roles with least privilege principle
- ✅ Enable S3 bucket versioning for data protection
- ✅ Enable CloudWatch Logs encryption
- ✅ Use VPC endpoints for private communication (optional)
- ✅ Enable AWS CloudTrail for audit logging

## 📈 Future Enhancements

- [ ] Support for multiple file formats (CSV, JSON, etc.)
- [ ] Add sentiment analysis for text content
- [ ] Implement batch processing for multiple files
- [ ] Add SNS notifications for processing completion
- [ ] Create a web dashboard for viewing metrics
- [ ] Add support for compressed files (.zip, .gz)

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author : Tanish Ranjan

