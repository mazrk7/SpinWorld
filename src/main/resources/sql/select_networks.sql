SELECT network,
AVG(CASE WHEN particle LIKE 'c%' THEN "USum" ELSE NULL END),
STDDEV(CASE WHEN particle LIKE 'c%' THEN "USum" ELSE NULL END),
AVG(CASE WHEN particle LIKE 'nc%' THEN "USum" ELSE NULL END),
STDDEV(CASE WHEN particle LIKE 'nc%' THEN "USum" ELSE NULL END),
SUM("USum")
FROM "aggregatedParticleScore"
WHERE "simId" = ?
GROUP BY network
