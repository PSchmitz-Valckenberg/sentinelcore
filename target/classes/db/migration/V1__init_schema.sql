CREATE TABLE rag_documents (
    id           VARCHAR(255) PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    content      TEXT         NOT NULL,
    trust_level  VARCHAR(50)  NOT NULL
);

CREATE TABLE evaluation_cases (
    id                VARCHAR(255) PRIMARY KEY,
    case_type         VARCHAR(50)  NOT NULL,
    attack_category   VARCHAR(50)  NULL,
    name              VARCHAR(255) NOT NULL,
    user_input        TEXT         NOT NULL,
    expected_behavior TEXT         NOT NULL
);

CREATE TABLE evaluation_case_documents (
    case_id     VARCHAR(255) NOT NULL REFERENCES evaluation_cases(id),
    document_id VARCHAR(255) NOT NULL REFERENCES rag_documents(id),
    PRIMARY KEY (case_id, document_id)
);

CREATE TABLE evaluation_case_checks (
    case_id    VARCHAR(255) NOT NULL REFERENCES evaluation_cases(id),
    check_type VARCHAR(50)  NOT NULL,
    PRIMARY KEY (case_id, check_type)
);

CREATE TABLE evaluation_runs (
    id          VARCHAR(255) PRIMARY KEY,
    mode        VARCHAR(50)  NOT NULL,
    status      VARCHAR(50)  NOT NULL,
    model       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    started_at  TIMESTAMP    NULL,
    finished_at TIMESTAMP    NULL
);

CREATE TABLE attack_executions (
    id         VARCHAR(255) PRIMARY KEY,
    run_id     VARCHAR(255) NOT NULL REFERENCES evaluation_runs(id),
    case_id    VARCHAR(255) NOT NULL REFERENCES evaluation_cases(id),
    case_type  VARCHAR(50)  NOT NULL,
    response   TEXT         NOT NULL,
    blocked    BOOLEAN      NOT NULL,
    refused    BOOLEAN      NOT NULL,
    label      VARCHAR(50)  NOT NULL,
    latency_ms INTEGER      NOT NULL
);

CREATE TABLE score_details (
    id           VARCHAR(255) PRIMARY KEY,
    execution_id VARCHAR(255) NOT NULL REFERENCES attack_executions(id),
    check_type   VARCHAR(50)  NOT NULL,
    result       BOOLEAN      NOT NULL,
    evidence     TEXT         NOT NULL
);
