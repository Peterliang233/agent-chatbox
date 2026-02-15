run-server:
	mvn -q -DskipTests exec:java \
	-Dexec.args="--mode server --server-port 8080"

run-client:
	mvn -q -DskipTests exec:java \
	-Dexec.args="--mode client --server-host 127.0.0.1 --server-port 8080 --stream true --typewriter true"