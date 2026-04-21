CREATE TABLE benchmarks (
    id          VARCHAR(255) PRIMARY KEY,
    model       VARCHAR(255) NOT NULL,
    status      VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    started_at  TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE TABLE benchmark_strategies (
    benchmark_id    VARCHAR(255) NOT NULL REFERENCES benchmarks(id),
    strategy_type   VARCHAR(50)  NOT NULL,
    strategy_order  INTEGER      NOT NULL,
    PRIMARY KEY (benchmark_id, strategy_type)
);

CREATE TABLE benchmark_runs (
    benchmark_id  VARCHAR(255) NOT NULL REFERENCES benchmarks(id),
    strategy_type VARCHAR(50)  NOT NULL,
    run_id        VARCHAR(255) NOT NULL REFERENCES evaluation_runs(id),
    PRIMARY KEY (benchmark_id, strategy_type),
    FOREIGN KEY (benchmark_id, strategy_type)
        REFERENCES benchmark_strategies(benchmark_id, strategy_type),
    UNIQUE (benchmark_id, run_id)
);