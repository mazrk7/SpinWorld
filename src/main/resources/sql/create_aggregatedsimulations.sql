CREATE OR REPLACE VIEW "aggregatedSimulations" AS 
	SELECT "name" AS strategy,
	network,
	AVG("ut. C") AS "ut. C",
	STDDEV("ut. C") AS "stddev ut. C",
	AVG("ut. NC") AS "ut. NC",
	STDDEV("ut. NC") AS "stddev ut. NC",
	AVG("rem. C") AS "rem. C",
	AVG("rem. NC") AS "rem. NC",
	COUNT("simId") AS repeats
	FROM "simulationSummary"
	GROUP BY "name", network