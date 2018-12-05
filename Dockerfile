FROM open-liberty:kernel

COPY server.xml /config/

COPY ./target/pos-server.war /config/dropins/
