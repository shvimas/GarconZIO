FROM mongo:4.0.3

COPY data /data
COPY run.sh /usr/bin

RUN chown mongodb:mongodb /usr/bin/run.sh
RUN chmod +x /usr/bin/run.sh

#CMD ["mongod", "--bind_ip_all"]
CMD ["/usr/bin/run.sh"]

