ALTER TABLE matters ADD COLUMN outcome VARCHAR(100);

-- Let's update some matters to closed/disposed state and assign outcomes for testing Win Rate
INSERT INTO matters (tenant_id, title, case_no, client_name, court, type, area, next_hearing, advocate, status, stage, outcome, filing_date)
VALUES
('default', 'State Bank of India vs Vijay Mallya', 'OA/999/2019', 'State Bank of India', 'DRT Mumbai', 'Litigation', 'Banking Law', '2026-05-10', 'Adv. Amit Sharma', 'Disposed', 'Judgment', 'Won', '2019-03-12'),
('default', 'Tata Motors Vendor Dispute', 'CS/765/2021', 'Tata Motors', 'Bombay High Court', 'Arbitration', 'Commercial Law', '2026-04-12', 'Adv. Priya Kapoor', 'Disposed', 'Award', 'Won', '2021-05-20'),
('default', 'Airtel License Fee Dispute', 'WP/876/2022', 'Bharti Airtel', 'Supreme Court of India', 'Litigation', 'Telecom Law', '2026-06-01', 'Adv. Amit Sharma', 'Disposed', 'Dismissed', 'Lost', '2022-07-15'),
('default', 'Rajesh Kumar Boundary Dispute', 'CS/321/2024', 'Rajesh Kumar', 'Delhi District Court', 'Litigation', 'Property Law', '2026-06-15', 'Adv. Priya Kapoor', 'Disposed', 'Compromise', 'Settled', '2024-02-10');
