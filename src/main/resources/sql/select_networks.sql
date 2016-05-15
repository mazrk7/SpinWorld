SELECT network,
AVG(USum),
STDDEV(USum),
SUM(USum)
FROM aggregatedParticleScore
WHERE "simId" = ?
GROUP BY network
