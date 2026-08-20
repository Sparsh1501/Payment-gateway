#!/bin/sh
set -eu

PGDATA=/var/lib/postgresql/data

install -d -o postgres -g postgres "$PGDATA"
install -d -o redis -g redis /data
install -d -o grafana -g grafana /var/lib/grafana

if [ ! -s "$PGDATA/PG_VERSION" ]; then
    su postgres -c "initdb -D '$PGDATA'"
fi

su postgres -c "pg_ctl -D '$PGDATA' -l /var/log/postgresql/postgresql.log start"

until pg_isready -h 127.0.0.1 -U postgres >/dev/null 2>&1; do
    sleep 1
done

if ! su postgres -c "psql -tAc \"SELECT 1 FROM pg_roles WHERE rolname='${DB_USERNAME}'\"" | grep -q 1; then
    su postgres -c "psql -c \"CREATE USER ${DB_USERNAME} WITH PASSWORD '${DB_PASSWORD}';\""
fi

if ! su postgres -c "psql -tAc \"SELECT 1 FROM pg_database WHERE datname='paygateway'\"" | grep -q 1; then
    su postgres -c "createdb -O ${DB_USERNAME} paygateway"
fi

su postgres -c "psql -d paygateway -f /docker-entrypoint-initdb.d/init.sql"
su postgres -c "pg_ctl -D '$PGDATA' stop"

exec /usr/bin/supervisord -n -c /etc/supervisor/supervisord.conf
