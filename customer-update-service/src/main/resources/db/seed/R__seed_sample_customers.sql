-- Local/demo data only (profile "local"). One row per interesting CUSTUPD rule path.
INSERT INTO customer_master (cust_id, first_name, last_name, dob, email, phone, addr_line1, city, state, zip, country,
                             status, cust_type, credit_limit, account_bal, risk_score, version)
VALUES (1001, 'JOHN',  'SMITH',   '1980-04-12', 'john.smith@example.com', '5551234567', '1 MAIN ST',  'AUSTIN',  'TX', '73301', 'USA', 'A', 'I', 5000.00,   1200.00, 640, 0),
       (1002, 'MARY',  'JONES',   '1975-09-30', 'mary.jones@example.com', '5559876543', '9 OAK AVE',  'DENVER',  'CO', '80201', 'USA', 'C', 'I', 0.00,      0.00,    500, 0),
       (1003, 'ACME',  'CORP',    NULL,         'ap@acme.example.com',    '5550001111', '100 IND RD', 'DETROIT', 'MI', '48201', 'USA', 'S', 'B', 25000.00,  8000.00, 550, 0),
       (1004, 'PETER', 'PARKER',  '1990-08-10', 'peter@example.com',      '5552223333', '20 INGRAM',  'QUEENS',  'NY', '11375', 'USA', 'A', 'I', 2000.00,   -150.00, 610, 0),
       (1005, 'TONY',  'STARK',   '1970-05-29', 'tony@example.com',       '5554445555', '10880 MALIBU', 'MALIBU', 'CA', '90265', 'USA', 'A', 'I', 10000.00, 500.00, 780, 0),
       (1006, 'BRUCE', 'WAYNE',   '1972-02-19', 'bruce@example.com',      '5556667777', '1007 MTN DR', 'GOTHAM', 'NJ', '07001', 'USA', 'I', 'I', 3000.00,   250.00,  450, 0)
ON CONFLICT (cust_id) DO NOTHING;
