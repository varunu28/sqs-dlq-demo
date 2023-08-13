#!/bin/sh
echo "Cleanup localstack sqs"

awslocal sqs --endpoint-url "http://localhost:44566" purge-queue --queue-url http://localhost:4566/000000000000/sample-queue-dlq
awslocal sqs --endpoint-url "http://localhost:44566" purge-queue --queue-url http://localhost:4566/000000000000/sample-queue
