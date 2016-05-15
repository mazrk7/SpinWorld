CREATE OR REPLACE VIEW aggregatedSimulations AS 
	SELECT "name" AS strategy,
	network,
	AVG("ut.") AS "ut.",
	STDDEV("ut.") AS "stddev ut.",
	AVG("rem.") AS "rem.",
	COUNT("simId") AS repeats
	FROM simulationsummary
	GROUP BY "name", network
