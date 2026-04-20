UPDATE evaluation_runs
SET strategy_type = 'INPUT_OUTPUT'
WHERE strategy_type = 'DEFENDED';
