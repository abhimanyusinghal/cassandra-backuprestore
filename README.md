# cassandra-backup
Tool for backup and restore snapshots of Cassandra

backup: 
java -jar cassandra-backup-jar-with-dependencies.jar backup /opt/cassandra/data scheme /tmp/backup.tar

restore: 
java -jar cassandra-backup-jar-with-dependencies.jar restore /opt/cassandra/data scheme /tmp/backup.tar
