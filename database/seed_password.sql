UPDATE users 
SET password_hash = '$2a$10$s3N3aX/iTyITnA0hHrv/NOT0Y.nWZvFit9AQuS1AB0phj1VUc.eP6'
WHERE username = 'admin';
COMMIT;