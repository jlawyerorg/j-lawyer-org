alter table cases add `filenumberext` VARCHAR(100) BINARY;

alter table cases add index `IDX_CASES_FILENUMBEREXT` (filenumberext);

commit;