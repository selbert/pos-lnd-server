# Build
mvn clean package && docker build -t ch.puzzle/lnd-pos-server .

# RUN

docker rm -f lnd-pos-server || true && docker run -d -p 8080:8080 -p 4848:4848 --name lnd-pos-server ch.puzzle/lnd-pos-server 