# transaction-batch-importer

Learning project for building a Spring Batch application with Spring Boot 3.5 and Java 17.

The application validates a transaction CSV file, imports accepted transactions into an in-memory H2 database, generates a text report, then archives the processed input file.

## Spring Batch Concepts Covered

- `Job`: global batch workflow named `importTransactionsJob`.
- Multi-step job flow: validate file, import transactions, generate report, archive input.
- `Tasklet`: used for file validation, report generation, and file archiving.
- Chunk-oriented `Step`: processing phase named `importTransactionsStep`.
- `FlatFileItemReader`: reads transactions from a CSV resource.
- `ItemProcessor`: validates business rules and converts DTOs into JPA entities.
- `JpaItemWriter`: writes entities into the database inside chunk transactions.
- `JobRepository`: stores Spring Batch execution metadata in `BATCH_*` tables.
- `JobLauncher`: starts a job execution asynchronously from a REST endpoint.
- `JobExecutionListener`: logs job start and completion.
- `JobExplorer`: reads execution metadata to expose job status.
- `@RestControllerAdvice`: returns consistent JSON errors for API failures.
- Idempotent import: transactions already present in the database are filtered by `transactionId`.

## Architecture

```text
POST /api/batch/transactions/import
        |
        v
BatchJobController
        |
        v
Async JobLauncher -> importTransactionsJob
        |
        v
validateInputFileStep (tasklet)
        |
        v
importTransactionsStep (chunk size: 10)
        |
        +--> FlatFileItemReader<TransactionCsvDto>
        |        reads app.transaction-import.input-directory/transactions.csv
        |
        +--> TransactionItemProcessor
        |        validates and maps CSV data to TransactionEntity
        |
        +--> JpaItemWriter<TransactionEntity>
                 persists rows into H2 table transactions
        |
        v
generateImportReportStep (tasklet)
        |
        v
archiveInputFileStep (tasklet)
```

## Job Flow

`importTransactionsJob` runs four steps in order:

1. `validateInputFileStep`
   Checks that the input file exists, is not empty, has a `.csv` extension, and starts with this exact header:

   ```csv
   transactionId,accountNumber,amount,currency,type,transactionDate
   ```

2. `importTransactionsStep`
   Keeps the existing chunk-oriented import:

   ```text
   FlatFileItemReader<TransactionCsvDto>
       -> TransactionItemProcessor
       -> JpaItemWriter<TransactionEntity>
   ```

3. `generateImportReportStep`
   Writes a text report with the job execution id, job name, import status, start time, end time, read count, write count, skip count, and rollback count.

4. `archiveInputFileStep`
   Moves the processed CSV file to the archive directory after the previous steps complete successfully.

## Technical Stack

- Java 17
- Spring Boot 3.5
- Spring Batch 5
- Spring Data JPA
- Jakarta Bean Validation
- H2 in-memory database
- Lombok
- Maven
- JUnit 5, AssertJ, Spring Batch Test

## Run The App

```bash
mvn spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

Automatic job execution is disabled with:

```yaml
spring.batch.job.enabled: false
```

Default file locations:

```yaml
app.transaction-import.input-directory: src/main/resources/input
app.transaction-import.archive-directory: target/archive
app.transaction-import.report-directory: target/import-reports
```

After a successful import, `transactions.csv` is moved out of the input directory. The sample backup file `src/main/resources/input/transactions.csv.backup` can be used to restore the demo input.

## H2 Console

Open:

```text
http://localhost:8080/h2-console
```

Use:

```text
JDBC URL: jdbc:h2:mem:batchdb
User: sa
Password:
```

Useful tables:

- `TRANSACTIONS`: imported application data.
- `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION`: Spring Batch metadata.

## Launch The Import Job

```bash
curl -i -X POST http://localhost:8080/api/batch/transactions/import
```

Example response:

```http
HTTP/1.1 202 Accepted
Location: http://localhost:8080/api/batch/jobs/1
```

```json
{
  "jobExecutionId": 1,
  "jobName": "importTransactionsJob",
  "status": "STARTING",
  "createTime": "2026-05-25T14:30:00.000",
  "exitCode": "UNKNOWN"
}
```

The HTTP request returns after the execution is created. The batch continues in a background thread.

## Check Job Status

```bash
curl http://localhost:8080/api/batch/jobs/1
```

Example completed response:

```json
{
  "jobExecutionId": 1,
  "jobName": "importTransactionsJob",
  "status": "COMPLETED",
  "createTime": "2026-05-25T14:30:00.000",
  "startTime": "2026-05-25T14:30:00.010",
  "endTime": "2026-05-25T14:30:01.120",
  "exitCode": "COMPLETED"
}
```

## Error Response Format

API errors are returned with a consistent JSON structure:

```json
{
  "timestamp": "2026-05-25T16:30:00.000",
  "status": 404,
  "error": "Batch job not found",
  "message": "Batch job execution 99 was not found.",
  "path": "/api/batch/jobs/99",
  "details": [
    "jobExecutionId=99"
  ]
}
```

## Sample CSV Format

Default file:

```text
src/main/resources/input/transactions.csv
```

Expected columns:

```csv
transactionId,accountNumber,amount,currency,type,transactionDate
TXN-001,ACC-1001,120.50,EUR,CREDIT,2026-05-20
TXN-002,ACC-1002,45.00,EUR,DEBIT,2026-05-21
TXN-003,ACC-1003,500.00,USD,CREDIT,2026-05-22
TXN-004,ACC-1004,30.00,EUR,DEBIT,2026-05-23
```

The provided sample file contains 30 transaction rows. With the configured chunk size of 10,
the import runs in 3 chunks.

## Generated Report And Archive

After a successful import, the report is written to:

```text
target/import-reports/transaction-import-report-{jobExecutionId}.txt
```

Example:

```text
Transaction Import Report
jobExecutionId=1
jobName=importTransactionsJob
status=COMPLETED
startTime=2026-05-25T14:30:00.010
endTime=2026-05-25T14:30:08.120
readCount=30
writeCount=30
skipCount=0
rollbackCount=0
```

The processed CSV file is archived to:

```text
target/archive/transactions-job-{jobExecutionId}-{timestamp}.csv
```

## Validation Rules

The project uses two validation levels:

- Jakarta Bean Validation on `TransactionCsvDto` for structural CSV constraints.
- `TransactionItemProcessor` for business transformation rules.

- `transactionId` is mandatory.
- `accountNumber` is mandatory.
- `amount` is mandatory.
- `amount` must be greater than zero in the CSV.
- `amount` must fit the database precision: 17 integer digits and 2 decimal digits.
- `currency` must be `EUR` or `USD`.
- `type` must be `CREDIT` or `DEBIT`.
- `transactionDate` is mandatory.
- `DEBIT` amounts are stored as negative values.
- Existing `transactionId` values are ignored to avoid duplicates when the same file is imported twice.

## Future Improvements

- Skip and retry policies.
- Rejected records table.
- External file upload endpoint.
