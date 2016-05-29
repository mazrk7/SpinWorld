DELETE FROM "aggregatedParticleScore" WHERE "simId" = ?;
INSERT INTO "aggregatedParticleScore"
	SELECT a."simId", a."name", 
	CAST(t.state->'network' AS int) AS network, 
	SUM(CAST(t.state->'U' AS float)) AS "USum" 
	FROM agents AS a 
	LEFT JOIN  agenttransient AS t 
		ON a."simId" = t."simId" 
		AND a.aid = t.aid 
		AND exist(t.state, 'network') 
		AND exist(t.state, 'U') 
	WHERE a."simId" = ? AND exist(t.state, 'network') AND defined(t.state, 'network')
	GROUP BY a."simId", a.aid, t.state->'network';