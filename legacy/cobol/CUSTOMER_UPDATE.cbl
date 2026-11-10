       IDENTIFICATION DIVISION.
       PROGRAM-ID.    CUSTUPD.
       AUTHOR.        LEGACY-MAINFRAME-TEAM.
      *================================================================*
      * PROGRAM  : CUSTUPD  (CUSTOMER_UPDATE.cbl)                      *
      * TYPE     : BATCH  (JCL STEP CUSTUPD01, NIGHTLY)                *
      * PURPOSE  : APPLY CUSTOMER MAINTENANCE TRANSACTIONS TO DB2      *
      *            TABLE CUSTOMER_MASTER AND WRITE AN AUDIT RECORD     *
      *            FOR EVERY SUCCESSFUL UPDATE.                        *
      * INPUT    : CUSTTXN  - SEQUENTIAL TRANSACTION FILE (LRECL 300)  *
      * OUTPUT   : CUSTAUD  - SEQUENTIAL AUDIT FILE                    *
      *            CUSTREJ  - SEQUENTIAL REJECT FILE                   *
      * TXN CODES: 'U' CONTACT/ADDRESS UPDATE                          *
      *            'S' STATUS CHANGE                                   *
      *            'L' CREDIT LIMIT CHANGE                             *
      *================================================================*
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT CUSTTXN-FILE ASSIGN TO CUSTTXN
                  ORGANIZATION IS SEQUENTIAL
                  FILE STATUS  IS WS-TXN-STATUS.
           SELECT CUSTAUD-FILE ASSIGN TO CUSTAUD
                  ORGANIZATION IS SEQUENTIAL
                  FILE STATUS  IS WS-AUD-STATUS.
           SELECT CUSTREJ-FILE ASSIGN TO CUSTREJ
                  ORGANIZATION IS SEQUENTIAL
                  FILE STATUS  IS WS-REJ-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  CUSTTXN-FILE
           RECORDING MODE IS F.
       01  TXN-RECORD.
           05  TXN-CUST-ID                PIC X(10).
           05  TXN-CODE                   PIC X(01).
               88  TXN-CONTACT-UPDATE                VALUE 'U'.
               88  TXN-STATUS-CHANGE                 VALUE 'S'.
               88  TXN-LIMIT-CHANGE                  VALUE 'L'.
           05  TXN-EXPECTED-VERSION       PIC 9(09).
           05  TXN-FIRST-NAME             PIC X(30).
           05  TXN-LAST-NAME              PIC X(30).
           05  TXN-EMAIL                  PIC X(60).
           05  TXN-PHONE                  PIC X(15).
           05  TXN-ADDR-LINE1             PIC X(40).
           05  TXN-ADDR-LINE2             PIC X(40).
           05  TXN-CITY                   PIC X(30).
           05  TXN-STATE                  PIC X(02).
           05  TXN-ZIP                    PIC X(10).
           05  TXN-COUNTRY                PIC X(03).
           05  TXN-NEW-STATUS             PIC X(01).
           05  TXN-NEW-LIMIT              PIC 9(09)V99.
           05  TXN-USER-ID                PIC X(08).

       FD  CUSTAUD-FILE
           RECORDING MODE IS F.
       01  AUD-RECORD.
           05  AUD-CUST-ID                PIC 9(10).
           05  AUD-TXN-CODE               PIC X(01).
           05  AUD-OLD-STATUS             PIC X(01).
           05  AUD-NEW-STATUS             PIC X(01).
           05  AUD-OLD-LIMIT              PIC S9(09)V99.
           05  AUD-NEW-LIMIT              PIC S9(09)V99.
           05  AUD-NEW-VERSION            PIC 9(09).
           05  AUD-USER-ID                PIC X(08).
           05  AUD-TIMESTAMP              PIC X(26).

       FD  CUSTREJ-FILE
           RECORDING MODE IS F.
       01  REJ-RECORD.
           05  REJ-CUST-ID                PIC X(10).
           05  REJ-TXN-CODE               PIC X(01).
           05  REJ-REASON-CODE            PIC X(04).
           05  REJ-REASON-TEXT            PIC X(60).

       WORKING-STORAGE SECTION.
       01  WS-FILE-STATUSES.
           05  WS-TXN-STATUS              PIC X(02).
               88  WS-TXN-OK                         VALUE '00'.
               88  WS-TXN-EOF                        VALUE '10'.
           05  WS-AUD-STATUS              PIC X(02).
           05  WS-REJ-STATUS              PIC X(02).

       01  WS-SWITCHES.
           05  WS-EOF-SW                  PIC X(01) VALUE 'N'.
               88  WS-END-OF-FILE                    VALUE 'Y'.
           05  WS-VALID-SW                PIC X(01) VALUE 'Y'.
               88  WS-TXN-VALID                      VALUE 'Y'.
               88  WS-TXN-INVALID                    VALUE 'N'.

       01  WS-COUNTERS.
           05  WS-READ-CNT                PIC 9(09) COMP VALUE 0.
           05  WS-UPD-CNT                 PIC 9(09) COMP VALUE 0.
           05  WS-REJ-CNT                 PIC 9(09) COMP VALUE 0.
           05  WS-COMMIT-CNT              PIC 9(09) COMP VALUE 0.

       01  WS-CONSTANTS.
           05  WS-COMMIT-INTERVAL         PIC 9(04) COMP VALUE 100.
           05  WS-MAX-CREDIT-LIMIT        PIC 9(09)V99
                                          VALUE 1000000.00.
           05  WS-MAX-RISK-FOR-INCREASE   PIC 9(03) VALUE 700.
           05  WS-DEFAULT-COUNTRY         PIC X(03) VALUE 'USA'.

       01  WS-WORK-FIELDS.
           05  WS-CUST-ID-NUM             PIC 9(10).
           05  WS-AT-COUNT                PIC 9(04) COMP.
           05  WS-OLD-STATUS              PIC X(01).
           05  WS-OLD-LIMIT               PIC S9(09)V99 COMP-3.
           05  WS-REJ-CODE                PIC X(04).
           05  WS-REJ-TEXT                PIC X(60).
           05  WS-CURRENT-TS              PIC X(26).

           COPY CUSTREC.

           EXEC SQL INCLUDE SQLCA END-EXEC.

       PROCEDURE DIVISION.
      *----------------------------------------------------------------*
       0000-MAIN.
      *----------------------------------------------------------------*
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS-TXN UNTIL WS-END-OF-FILE
           PERFORM 9000-TERMINATE
           GOBACK.

      *----------------------------------------------------------------*
       1000-INITIALIZE.
      *----------------------------------------------------------------*
           OPEN INPUT  CUSTTXN-FILE
                OUTPUT CUSTAUD-FILE
                       CUSTREJ-FILE
           IF NOT WS-TXN-OK
              DISPLAY 'CUSTUPD - OPEN ERROR CUSTTXN FS=' WS-TXN-STATUS
              PERFORM 9999-ABEND
           END-IF
           PERFORM 8000-READ-TXN.

      *----------------------------------------------------------------*
       2000-PROCESS-TXN.
      *----------------------------------------------------------------*
           SET WS-TXN-VALID TO TRUE
           PERFORM 2100-VALIDATE-TXN
           IF WS-TXN-VALID
              PERFORM 2200-FETCH-CUSTOMER
           END-IF
           IF WS-TXN-VALID
              PERFORM 2300-APPLY-BUSINESS-RULES
           END-IF
           IF WS-TXN-VALID
              PERFORM 2400-UPDATE-CUSTOMER
           END-IF
           IF WS-TXN-VALID
              PERFORM 2500-WRITE-AUDIT
              ADD 1 TO WS-UPD-CNT WS-COMMIT-CNT
              IF WS-COMMIT-CNT >= WS-COMMIT-INTERVAL
                 PERFORM 7000-COMMIT
              END-IF
           ELSE
              PERFORM 2600-WRITE-REJECT
           END-IF
           PERFORM 8000-READ-TXN.

      *----------------------------------------------------------------*
       2100-VALIDATE-TXN.
      *----------------------------------------------------------------*
      *    BR001 - CUSTOMER ID MUST BE NUMERIC AND GREATER THAN ZERO
           IF TXN-CUST-ID NOT NUMERIC
              MOVE 'E001' TO WS-REJ-CODE
              MOVE 'CUSTOMER ID NOT NUMERIC' TO WS-REJ-TEXT
              SET WS-TXN-INVALID TO TRUE
           ELSE
              MOVE TXN-CUST-ID TO WS-CUST-ID-NUM
              IF WS-CUST-ID-NUM = ZERO
                 MOVE 'E001' TO WS-REJ-CODE
                 MOVE 'CUSTOMER ID MUST BE > 0' TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              END-IF
           END-IF
      *    BR002 - TRANSACTION CODE MUST BE U, S OR L
           IF WS-TXN-VALID
              IF NOT (TXN-CONTACT-UPDATE OR TXN-STATUS-CHANGE
                      OR TXN-LIMIT-CHANGE)
                 MOVE 'E002' TO WS-REJ-CODE
                 MOVE 'INVALID TRANSACTION CODE' TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              END-IF
           END-IF.

      *----------------------------------------------------------------*
       2200-FETCH-CUSTOMER.
      *----------------------------------------------------------------*
           EXEC SQL
                SELECT CUST_ID, FIRST_NAME, LAST_NAME, DOB, EMAIL,
                       PHONE, ADDR_LINE1, ADDR_LINE2, CITY, STATE,
                       ZIP, COUNTRY, STATUS, CUST_TYPE, CREDIT_LIMIT,
                       ACCOUNT_BAL, RISK_SCORE, LAST_UPD_TS,
                       LAST_UPD_USER, VERSION
                  INTO :CUST-ID, :CUST-FIRST-NAME, :CUST-LAST-NAME,
                       :CUST-DOB, :CUST-EMAIL, :CUST-PHONE,
                       :CUST-ADDR-LINE1, :CUST-ADDR-LINE2, :CUST-CITY,
                       :CUST-STATE, :CUST-ZIP, :CUST-COUNTRY,
                       :CUST-STATUS, :CUST-TYPE, :CUST-CREDIT-LIMIT,
                       :CUST-ACCOUNT-BAL, :CUST-RISK-SCORE,
                       :CUST-LAST-UPD-TS, :CUST-LAST-UPD-USER,
                       :CUST-VERSION
                  FROM CUSTOMER_MASTER
                 WHERE CUST_ID = :WS-CUST-ID-NUM
           END-EXEC
           EVALUATE SQLCODE
              WHEN 0
                 CONTINUE
      *       BR003 - CUSTOMER MUST EXIST
              WHEN 100
                 MOVE 'E003' TO WS-REJ-CODE
                 MOVE 'CUSTOMER NOT FOUND' TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              WHEN OTHER
                 DISPLAY 'CUSTUPD - SELECT FAILED SQLCODE=' SQLCODE
                 PERFORM 9999-ABEND
           END-EVALUATE
      *    BR004 - OPTIMISTIC LOCK: EXPECTED VERSION MUST MATCH
           IF WS-TXN-VALID
              IF TXN-EXPECTED-VERSION NOT = CUST-VERSION
                 MOVE 'E004' TO WS-REJ-CODE
                 MOVE 'RECORD CHANGED BY ANOTHER USER' TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              END-IF
           END-IF.

      *----------------------------------------------------------------*
       2300-APPLY-BUSINESS-RULES.
      *----------------------------------------------------------------*
           MOVE CUST-STATUS       TO WS-OLD-STATUS
           MOVE CUST-CREDIT-LIMIT TO WS-OLD-LIMIT
      *    BR005 - IF CUST-STATUS = CLOSED THEN REJECT ANY UPDATE
           IF CUST-CLOSED
              MOVE 'E005' TO WS-REJ-CODE
              MOVE 'CUSTOMER IS CLOSED - UPDATE REJECTED' TO WS-REJ-TEXT
              SET WS-TXN-INVALID TO TRUE
           ELSE
              EVALUATE TRUE
                 WHEN TXN-CONTACT-UPDATE
                    PERFORM 2310-CONTACT-RULES
                 WHEN TXN-STATUS-CHANGE
                    PERFORM 2320-STATUS-RULES
                 WHEN TXN-LIMIT-CHANGE
                    PERFORM 2330-LIMIT-RULES
              END-EVALUATE
           END-IF.

      *----------------------------------------------------------------*
       2310-CONTACT-RULES.
      *----------------------------------------------------------------*
      *    BR006 - SUSPENDED CUSTOMERS MAY ONLY HAVE STATUS CHANGES
           IF CUST-SUSPENDED
              MOVE 'E006' TO WS-REJ-CODE
              MOVE 'CUSTOMER SUSPENDED - CONTACT UPDATE NOT ALLOWED'
                TO WS-REJ-TEXT
              SET WS-TXN-INVALID TO TRUE
           END-IF
      *    BR007 - EMAIL, WHEN SUPPLIED, MUST CONTAIN EXACTLY ONE '@'
           IF WS-TXN-VALID AND TXN-EMAIL NOT = SPACES
              MOVE ZERO TO WS-AT-COUNT
              INSPECT TXN-EMAIL TALLYING WS-AT-COUNT FOR ALL '@'
              IF WS-AT-COUNT NOT = 1
                 MOVE 'E007' TO WS-REJ-CODE
                 MOVE 'INVALID EMAIL ADDRESS' TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              END-IF
           END-IF
      *    BR008 - PHONE, WHEN SUPPLIED, MUST BE NUMERIC
           IF WS-TXN-VALID AND TXN-PHONE NOT = SPACES
              IF FUNCTION TRIM(TXN-PHONE) NOT NUMERIC
                 MOVE 'E008' TO WS-REJ-CODE
                 MOVE 'INVALID PHONE NUMBER' TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              END-IF
           END-IF
      *    BR009 - ONLY NON-BLANK FIELDS OVERWRITE; NAMES UPPER-CASED
           IF WS-TXN-VALID
              IF TXN-FIRST-NAME NOT = SPACES
                 MOVE FUNCTION UPPER-CASE(TXN-FIRST-NAME)
                   TO CUST-FIRST-NAME
              END-IF
              IF TXN-LAST-NAME NOT = SPACES
                 MOVE FUNCTION UPPER-CASE(TXN-LAST-NAME)
                   TO CUST-LAST-NAME
              END-IF
              IF TXN-EMAIL NOT = SPACES
                 MOVE FUNCTION LOWER-CASE(TXN-EMAIL) TO CUST-EMAIL
              END-IF
              IF TXN-PHONE NOT = SPACES
                 MOVE TXN-PHONE TO CUST-PHONE
              END-IF
              IF TXN-ADDR-LINE1 NOT = SPACES
                 MOVE TXN-ADDR-LINE1 TO CUST-ADDR-LINE1
                 MOVE TXN-ADDR-LINE2 TO CUST-ADDR-LINE2
                 MOVE TXN-CITY       TO CUST-CITY
                 MOVE TXN-STATE      TO CUST-STATE
                 MOVE TXN-ZIP        TO CUST-ZIP
      *          BR010 - BLANK COUNTRY DEFAULTS TO 'USA'
                 IF TXN-COUNTRY = SPACES
                    MOVE WS-DEFAULT-COUNTRY TO CUST-COUNTRY
                 ELSE
                    MOVE TXN-COUNTRY TO CUST-COUNTRY
                 END-IF
              END-IF
           END-IF.

      *----------------------------------------------------------------*
       2320-STATUS-RULES.
      *----------------------------------------------------------------*
      *    BR011 - NEW STATUS MUST BE A, I, S OR C
           IF TXN-NEW-STATUS NOT = 'A' AND 'I' AND 'S' AND 'C'
              MOVE 'E011' TO WS-REJ-CODE
              MOVE 'INVALID STATUS CODE' TO WS-REJ-TEXT
              SET WS-TXN-INVALID TO TRUE
           END-IF
      *    BR012 - CUSTOMER CANNOT BE CLOSED WITH NON-ZERO BALANCE
           IF WS-TXN-VALID AND TXN-NEW-STATUS = 'C'
              IF CUST-ACCOUNT-BAL NOT = ZERO
                 MOVE 'E012' TO WS-REJ-CODE
                 MOVE 'CANNOT CLOSE - OUTSTANDING BALANCE'
                   TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              END-IF
           END-IF
      *    BR013 - CANNOT ACTIVATE A CUSTOMER WITH NEGATIVE BALANCE
           IF WS-TXN-VALID AND TXN-NEW-STATUS = 'A'
              IF CUST-ACCOUNT-BAL < ZERO
                 MOVE 'E013' TO WS-REJ-CODE
                 MOVE 'CANNOT ACTIVATE - NEGATIVE BALANCE'
                   TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              END-IF
           END-IF
           IF WS-TXN-VALID
              MOVE TXN-NEW-STATUS TO CUST-STATUS
           END-IF.

      *----------------------------------------------------------------*
       2330-LIMIT-RULES.
      *----------------------------------------------------------------*
      *    BR014 - LIMIT CHANGES ONLY ALLOWED FOR ACTIVE CUSTOMERS
           IF NOT CUST-ACTIVE
              MOVE 'E014' TO WS-REJ-CODE
              MOVE 'CREDIT LIMIT CHANGE REQUIRES ACTIVE CUSTOMER'
                TO WS-REJ-TEXT
              SET WS-TXN-INVALID TO TRUE
           END-IF
      *    BR015 - NEW LIMIT MUST NOT EXCEED 1,000,000.00
           IF WS-TXN-VALID AND TXN-NEW-LIMIT > WS-MAX-CREDIT-LIMIT
              MOVE 'E015' TO WS-REJ-CODE
              MOVE 'CREDIT LIMIT EXCEEDS MAXIMUM' TO WS-REJ-TEXT
              SET WS-TXN-INVALID TO TRUE
           END-IF
      *    BR016 - NEW LIMIT CANNOT BE BELOW CURRENT POSITIVE BALANCE
           IF WS-TXN-VALID AND CUST-ACCOUNT-BAL > ZERO
              IF TXN-NEW-LIMIT < CUST-ACCOUNT-BAL
                 MOVE 'E016' TO WS-REJ-CODE
                 MOVE 'CREDIT LIMIT BELOW CURRENT BALANCE'
                   TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              END-IF
           END-IF
           IF WS-TXN-VALID AND TXN-NEW-LIMIT > CUST-CREDIT-LIMIT
      *       BR017 - IF ACCOUNT-BAL < 0 THEN LIMIT CANNOT BE INCREASED
              IF CUST-ACCOUNT-BAL < ZERO
                 MOVE 'E017' TO WS-REJ-CODE
                 MOVE 'NEGATIVE BALANCE - LIMIT INCREASE REJECTED'
                   TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              END-IF
      *       BR018 - RISK SCORE > 700 BLOCKS ANY LIMIT INCREASE
              IF WS-TXN-VALID
                 AND CUST-RISK-SCORE > WS-MAX-RISK-FOR-INCREASE
                 MOVE 'E018' TO WS-REJ-CODE
                 MOVE 'RISK SCORE TOO HIGH - LIMIT INCREASE REJECTED'
                   TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              END-IF
           END-IF
           IF WS-TXN-VALID
              MOVE TXN-NEW-LIMIT TO CUST-CREDIT-LIMIT
           END-IF.

      *----------------------------------------------------------------*
       2400-UPDATE-CUSTOMER.
      *----------------------------------------------------------------*
      *    BR019 - STAMP AUDIT COLUMNS AND BUMP VERSION ON EVERY UPDATE
           MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-TS
           EXEC SQL
                UPDATE CUSTOMER_MASTER
                   SET FIRST_NAME    = :CUST-FIRST-NAME,
                       LAST_NAME     = :CUST-LAST-NAME,
                       EMAIL         = :CUST-EMAIL,
                       PHONE         = :CUST-PHONE,
                       ADDR_LINE1    = :CUST-ADDR-LINE1,
                       ADDR_LINE2    = :CUST-ADDR-LINE2,
                       CITY          = :CUST-CITY,
                       STATE         = :CUST-STATE,
                       ZIP           = :CUST-ZIP,
                       COUNTRY       = :CUST-COUNTRY,
                       STATUS        = :CUST-STATUS,
                       CREDIT_LIMIT  = :CUST-CREDIT-LIMIT,
                       LAST_UPD_TS   = CURRENT TIMESTAMP,
                       LAST_UPD_USER = :TXN-USER-ID,
                       VERSION       = VERSION + 1
                 WHERE CUST_ID = :CUST-ID
                   AND VERSION = :CUST-VERSION
           END-EXEC
           EVALUATE SQLCODE
              WHEN 0
                 ADD 1 TO CUST-VERSION
              WHEN 100
                 MOVE 'E004' TO WS-REJ-CODE
                 MOVE 'RECORD CHANGED BY ANOTHER USER' TO WS-REJ-TEXT
                 SET WS-TXN-INVALID TO TRUE
              WHEN OTHER
      *          BR020 - ANY SQL FAILURE ROLLS BACK THE UNIT OF WORK
                 DISPLAY 'CUSTUPD - UPDATE FAILED SQLCODE=' SQLCODE
                 EXEC SQL ROLLBACK END-EXEC
                 PERFORM 9999-ABEND
           END-EVALUATE.

      *----------------------------------------------------------------*
       2500-WRITE-AUDIT.
      *----------------------------------------------------------------*
      *    BR021 - EVERY SUCCESSFUL UPDATE PRODUCES AN AUDIT RECORD
           MOVE CUST-ID           TO AUD-CUST-ID
           MOVE TXN-CODE          TO AUD-TXN-CODE
           MOVE WS-OLD-STATUS     TO AUD-OLD-STATUS
           MOVE CUST-STATUS       TO AUD-NEW-STATUS
           MOVE WS-OLD-LIMIT      TO AUD-OLD-LIMIT
           MOVE CUST-CREDIT-LIMIT TO AUD-NEW-LIMIT
           MOVE CUST-VERSION      TO AUD-NEW-VERSION
           MOVE TXN-USER-ID       TO AUD-USER-ID
           MOVE WS-CURRENT-TS     TO AUD-TIMESTAMP
           WRITE AUD-RECORD
           IF WS-AUD-STATUS NOT = '00'
              DISPLAY 'CUSTUPD - AUDIT WRITE FS=' WS-AUD-STATUS
              PERFORM 9999-ABEND
           END-IF.

      *----------------------------------------------------------------*
       2600-WRITE-REJECT.
      *----------------------------------------------------------------*
           MOVE TXN-CUST-ID TO REJ-CUST-ID
           MOVE TXN-CODE    TO REJ-TXN-CODE
           MOVE WS-REJ-CODE TO REJ-REASON-CODE
           MOVE WS-REJ-TEXT TO REJ-REASON-TEXT
           WRITE REJ-RECORD
           ADD 1 TO WS-REJ-CNT
           DISPLAY 'CUSTUPD - REJECTED ' TXN-CUST-ID ' ' WS-REJ-CODE.

      *----------------------------------------------------------------*
       7000-COMMIT.
      *----------------------------------------------------------------*
           EXEC SQL COMMIT END-EXEC
           MOVE ZERO TO WS-COMMIT-CNT.

      *----------------------------------------------------------------*
       8000-READ-TXN.
      *----------------------------------------------------------------*
           READ CUSTTXN-FILE
              AT END
                 SET WS-END-OF-FILE TO TRUE
              NOT AT END
                 ADD 1 TO WS-READ-CNT
           END-READ
           IF NOT WS-TXN-OK AND NOT WS-TXN-EOF
              DISPLAY 'CUSTUPD - READ ERROR CUSTTXN FS=' WS-TXN-STATUS
              PERFORM 9999-ABEND
           END-IF.

      *----------------------------------------------------------------*
       9000-TERMINATE.
      *----------------------------------------------------------------*
           PERFORM 7000-COMMIT
           CLOSE CUSTTXN-FILE CUSTAUD-FILE CUSTREJ-FILE
           DISPLAY 'CUSTUPD - RECORDS READ    : ' WS-READ-CNT
           DISPLAY 'CUSTUPD - RECORDS UPDATED : ' WS-UPD-CNT
           DISPLAY 'CUSTUPD - RECORDS REJECTED: ' WS-REJ-CNT.

      *----------------------------------------------------------------*
       9999-ABEND.
      *----------------------------------------------------------------*
           DISPLAY 'CUSTUPD - ABENDING WITH U999'
           MOVE 999 TO RETURN-CODE
           CALL 'CEE3ABD' USING RETURN-CODE
           STOP RUN.
