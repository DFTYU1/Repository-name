PRAGMA foreign_keys = ON;
CREATE TABLE documents (
    id TEXT PRIMARY KEY NOT NULL,
    title_enc TEXT NOT NULL,
    stored_name TEXT NOT NULL UNIQUE,
    sha256 TEXT NOT NULL,
    bytes INTEGER NOT NULL CHECK(bytes >= 0),
    added_utc INTEGER NOT NULL,
    kind TEXT NOT NULL CHECK(kind IN ('USER_UPLOAD','KNOWLEDGE','DOWNLOAD','WORK_FILE')),
    index_state TEXT NOT NULL CHECK(index_state IN ('IMPORTED','READY','FAILED'))
);
CREATE TABLE chunks (
    document_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    ordinal INTEGER NOT NULL CHECK(ordinal >= 0),
    source_offset INTEGER NOT NULL CHECK(source_offset >= 0),
    body_enc TEXT NOT NULL,
    PRIMARY KEY(document_id, ordinal)
);
CREATE TABLE messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role TEXT NOT NULL CHECK(role IN ('user','assistant')),
    body_enc TEXT NOT NULL,
    created_utc INTEGER NOT NULL
);
CREATE TABLE tasks (
    id TEXT PRIMARY KEY NOT NULL,
    state TEXT NOT NULL CHECK(state IN ('PLANNED','RUNNING','SUCCEEDED','FAILED','CANCELLED','INTERRUPTED')),
    tool_id TEXT NOT NULL,
    code TEXT NOT NULL,
    updated_utc INTEGER NOT NULL
);
CREATE TABLE task_events (
    seq INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id TEXT NOT NULL REFERENCES tasks(id),
    state TEXT NOT NULL,
    tool_id TEXT NOT NULL,
    code TEXT NOT NULL,
    created_utc INTEGER NOT NULL
);
CREATE INDEX task_events_task ON task_events(task_id, seq);
CREATE TABLE memory (
    id TEXT PRIMARY KEY NOT NULL,
    body_enc TEXT NOT NULL,
    enabled INTEGER NOT NULL CHECK(enabled IN (0,1)),
    updated_utc INTEGER NOT NULL,
    revision INTEGER NOT NULL DEFAULT 1
);
CREATE TABLE vectors (
    document_id TEXT NOT NULL,
    ordinal INTEGER NOT NULL,
    model_id TEXT NOT NULL,
    model_sha256 TEXT NOT NULL,
    dimensions INTEGER NOT NULL CHECK(dimensions > 0),
    vector_enc TEXT NOT NULL,
    PRIMARY KEY(document_id, ordinal, model_id),
    FOREIGN KEY(document_id, ordinal) REFERENCES chunks(document_id, ordinal) ON DELETE CASCADE
);
