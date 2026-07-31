-- V13: Sync filing stages and document types with the new requirements
UPDATE masters 
SET items = ARRAY[
    'Petition',
    'Affidavit',
    'Vakalatnama',
    'Written Submissions',
    'Rejoinder',
    'Synopsis',
    'Application',
    'Plaint',
    'Written Statement',
    'Court Order',
    'Judgment',
    'Notice',
    'Agreement',
    'Power of Attorney',
    'Evidence',
    'Memo of Appearance'
]
WHERE tenant_id = 'default' AND category = 'docTypes';

UPDATE masters 
SET items = ARRAY[
    'Draft',
    'Under Review',
    'Approved',
    'Signed',
    'Filed',
    'Defects Raised',
    'Defects Cleared',
    'Returned'
]
WHERE tenant_id = 'default' AND category = 'filingStages';

-- Migrate existing filings matching the obsolete stage/status 'Ready to File'
UPDATE filings
SET stage = 'Signed', status = 'Signed'
WHERE stage = 'Ready to File' OR status = 'Ready to File';
