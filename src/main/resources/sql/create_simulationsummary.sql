CREATE TABLE IF NOT EXISTS simulationsummary (
	"simId" bigint NOT NULL REFERENCES simulations,
	name varchar(255) NOT NULL,
	network int NOT NULL,
	"ut." float,
	"stddev ut." float,
	"total ut." float,
	"rem." int,
	PRIMARY KEY ("simId", network)
)
