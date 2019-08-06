mongod --bind_ip_all &
sleep 3s
mongoimport --uri "mongodb://root:test@localhost:27017/garcon?authsource=admin" --collection 111_en-ru /data/words.json