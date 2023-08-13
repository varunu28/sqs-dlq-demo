#!/bin/sh
echo "Read message from localstack sqs"

echo "sample queue"
awslocal sqs --endpoint-url "http://localhost:44566" receive-message --queue-url http://localhost:4566/000000000000/sample-queue --attribute-names All --message-attribute-names All --max-number-of-messages 10

echo "DLQ"
awslocal sqs --endpoint-url "http://localhost:44566" receive-message --queue-url http://localhost:4566/000000000000/sample-queue-dlq --attribute-names All --message-attribute-names All --max-number-of-messages 10
