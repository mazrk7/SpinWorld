CREATE TABLE IF NOT EXISTS "aggregatedParticleScore" (
	"simId" bigint NOT NULL REFERENCES simulations,
	particle varchar(10) NOT NULL,
	network int NOT NULL,
	"USum" float NOT NULL,
	PRIMARY KEY ("simId", particle, network)
)
