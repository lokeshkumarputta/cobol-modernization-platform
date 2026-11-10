      *================================================================*
      * COPYBOOK : CUSTREC  (CUSTOMER_RECORD.cpy)                      *
      * PURPOSE  : HOST-VARIABLE LAYOUT FOR DB2 TABLE CUSTOMER_MASTER  *
      *            SHARED BY CUSTUPD AND ONLINE INQUIRY PROGRAMS.      *
      *================================================================*
       01  CUSTOMER-RECORD.
           05  CUST-ID                    PIC 9(10).
           05  CUST-NAME.
               10  CUST-FIRST-NAME        PIC X(30).
               10  CUST-LAST-NAME         PIC X(30).
           05  CUST-DOB                   PIC 9(08).
           05  CUST-EMAIL                 PIC X(60).
           05  CUST-PHONE                 PIC X(15).
           05  CUST-ADDRESS.
               10  CUST-ADDR-LINE1        PIC X(40).
               10  CUST-ADDR-LINE2        PIC X(40).
               10  CUST-CITY              PIC X(30).
               10  CUST-STATE             PIC X(02).
               10  CUST-ZIP               PIC X(10).
               10  CUST-COUNTRY           PIC X(03).
           05  CUST-STATUS                PIC X(01).
               88  CUST-ACTIVE                       VALUE 'A'.
               88  CUST-INACTIVE                     VALUE 'I'.
               88  CUST-SUSPENDED                    VALUE 'S'.
               88  CUST-CLOSED                       VALUE 'C'.
           05  CUST-TYPE                  PIC X(01).
               88  CUST-INDIVIDUAL                   VALUE 'I'.
               88  CUST-BUSINESS                     VALUE 'B'.
           05  CUST-CREDIT-LIMIT          PIC S9(09)V99 COMP-3.
           05  CUST-ACCOUNT-BAL           PIC S9(09)V99 COMP-3.
           05  CUST-RISK-SCORE            PIC 9(03).
           05  CUST-LAST-UPD-TS           PIC X(26).
           05  CUST-LAST-UPD-USER         PIC X(08).
           05  CUST-VERSION               PIC S9(09) COMP.
