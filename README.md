# sqs-dlq-demo

## Expected behavior?
Once we have consumed a message from an SQS queue upto its `maxReceiveCount` and not acknowledged its receipt by deleting it from the queue, the message should be pushed to its DLQ. [AWS Documentation](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-dead-letter-queues.html) says:
```
The maxReceiveCount is the number of times a consumer tries receiving a message from a queue without deleting it before being moved to the dead-letter queue. Setting the maxReceiveCount to a low value such as 1 would result in any failure to receive a message to cause the message to be moved to the dead-letter queue.
```

## What is happening?
The test is unable to consume message from DLQ although the message is present in the DLQ when read through `aws-cli`

![demo.png](demo.png)

## Investigation done till now
 - Ensured that the `DelaySeconds` is set to 0 for DLQ so that there is no delay for messages being visible
 - Verified that messages are being pushed to SQS DLQ by querying DLQ through CLI

## Resolution
This is happening due to an undocumented optimization(maybe) on AWS end. So the message is not pushed to DLQ until you query the queue one more time post exhausting the `maxReceiveCount` threshold. The test is fixed by adding an un-neccessary call to query the SQS queue and then the message becomes visible in DLQ.

This issue is already documented under https://github.com/localstack/localstack/issues/8234
