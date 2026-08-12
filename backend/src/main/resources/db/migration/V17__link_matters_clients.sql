-- V17: Link existing matters to clients using client name matches
UPDATE matters SET client_id = (SELECT id FROM clients WHERE name = 'Rahul Gupta') WHERE client_name = 'Rahul Gupta';
UPDATE matters SET client_id = (SELECT id FROM clients WHERE name = 'Infosys Ltd.') WHERE client_name = 'Infosys Ltd.';
UPDATE matters SET client_id = (SELECT id FROM clients WHERE name = 'Ministry of Finance') WHERE client_name = 'Ministry of Finance';
UPDATE matters SET client_id = (SELECT id FROM clients WHERE name = 'Sunita Sharma') WHERE client_name = 'Sunita Sharma';
UPDATE matters SET client_id = (SELECT id FROM clients WHERE name = 'TechStart Pvt Ltd') WHERE client_name = 'TechStart Pvt Ltd';
