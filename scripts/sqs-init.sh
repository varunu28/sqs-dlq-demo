#!/bin/sh
echo "Init localstack sqs"

export AWS_SECRET_ACCESS_KEY="FAKE"
export AWS_ACCESS_KEY_ID="FAKE"
export AWS_DEFAULT_REGION=us-east-1

# For demo purposes, let us create a dead letter queue as well
awslocal sqs --endpoint-url "http://localhost:44566" create-queue --queue-name sample-queue-dlq --attributes '{"MessageRetentionPeriod": "259200", "DelaySeconds": "0"}'

awslocal sqs --endpoint-url "http://localhost:44566" create-queue --queue-name sample-queue --attributes '{"RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:sample-queue-dlq\",\"maxReceiveCount\":\"3\"}", "VisibilityTimeout": "1"}'
