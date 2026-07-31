-- V18: Remove old documents with mock s3.example.com URLs to allow clean re-upload
DELETE FROM documents WHERE s3_url LIKE 'https://s3.example.com/%';
