Install Valkey Cluster 8.0.1

Follow the installation guide at Valkey Installation.

Setup a Valkey Cluster

Ensure you set up a Valkey cluster with a minimum of two master nodes and their replicas.
For visualization, you can use RedisInsight.
Configuration Changes

Update the REDIS_PORT and REDIS_HOST settings in RedisConnectionProvider.java located inside the config package.
Local Cache Synchronization

To check and verify local cache synchronization, run two instances of this application.
API Endpoints

I have exposed two APIs to handle GET and PUT requests with support for expiration, flushing, and invalidation.

Here is a sample shell script to create a Valkey cluster with three primary nodes and their replicas:
#!/bin/bash

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# Settings
BIN_PATH="$SCRIPT_DIR/../../src/"
CLUSTER_HOST=127.0.0.1
PORT=30000
TIMEOUT=2000
NODES=6
REPLICAS=1
PROTECTED_MODE=yes
ADDITIONAL_OPTIONS=""

# You may want to put the above config parameters into config.sh in order to
# override the defaults without modifying this script.

if [ -a config.sh ]
then
    source "config.sh"
fi

# Computed vars
ENDPORT=$((PORT+NODES))

if [ "$1" == "start" ]
then
    while [ $((PORT < ENDPORT)) != "0" ]; do
        PORT=$((PORT+1))
        echo "Starting $PORT"
        $BIN_PATH/valkey-serer --port $PORT --protected-mode $PROTECTED_MODE  --cluster-config-file nodes-${PORT}.conf --cluster-node-timeout $TIMEOUT --appendonly yes --appendfilename appendonly-${PORT}.aof --appenddirname appendonlydir-${PORT} --dbfilename dump-${PORT}.rdb --logfile ${PORT}.log --daemonize yes --enable-protected-configs yes --enable-debug-command yes --enable-module-command yes ${ADDITIONAL_OPTIONS}
    done
    exit 0
fi

if [ "$1" == "create" ]
then
    HOSTS=""
    while [ $((PORT < ENDPORT)) != "0" ]; do
        PORT=$((PORT+1))
        HOSTS="$HOSTS $CLUSTER_HOST:$PORT"
    done
    OPT_ARG=""
    if [ "$2" == "-f" ]; then
        OPT_ARG="--cluster-yes"
    fi
    $BIN_PATH/valkey-cli --cluster create $HOSTS --cluster-replicas $REPLICAS $OPT_ARG
    exit 0
fi

if [ "$1" == "stop" ]
then
    while [ $((PORT < ENDPORT)) != "0" ]; do
        PORT=$((PORT+1))
        echo "Stopping $PORT"
        $BIN_PATH/valkey-cli -p $PORT shutdown nosave
    done
    exit 0
fi

if [ "$1" == "restart" ]
then
    OLD_PORT=$PORT
    while [ $((PORT < ENDPORT)) != "0" ]; do
        PORT=$((PORT+1))
        echo "Stopping $PORT"
        $BIN_PATH/valkey-cli -p $PORT shutdown nosave
    done
    PORT=$OLD_PORT
    while [ $((PORT < ENDPORT)) != "0" ]; do
        PORT=$((PORT+1))
        echo "Starting $PORT"
        $BIN_PATH/valkey-server --port $PORT --protected-mode $PROTECTED_MODE --cluster-enabled yes --cluster-config-file nodes-${PORT}.conf --cluster-node-timeout $TIMEOUT --appendonly yes --appendfilename appendonly-${PORT}.aof --appenddirname appendonlydir-${PORT} --dbfilename dump-${PORT}.rdb --logfile ${PORT}.log --daemonize yes --enable-protected-configs yes --enable-debug-command yes --enable-module-command yes ${ADDITIONAL_OPTIONS}
    done
    exit 0
fi

if [ "$1" == "watch" ]
then
    PORT=$((PORT+1))
    while [ 1 ]; do
        clear
        date
        $BIN_PATH/valkey-cli -p $PORT cluster nodes | head -30
        sleep 1
    done
    exit 0
fi

if [ "$1" == "tail" ]
then
    INSTANCE=$2
    PORT=$((PORT+INSTANCE))
    tail -f ${PORT}.log
    exit 0
fi

if [ "$1" == "tailall" ]
then
    tail -f *.log
    exit 0
fi

if [ "$1" == "call" ]
then
    while [ $((PORT < ENDPORT)) != "0" ]; do
        PORT=$((PORT+1))
        $BIN_PATH/valkey-cli -p $PORT $2 $3 $4 $5 $6 $7 $8 $9
    done
    exit 0
fi

if [ "$1" == "clean" ]
then
    echo "Cleaning *.log"
    rm -rf *.log
    echo "Cleaning appendonlydir-*"
    rm -rf appendonlydir-*
    echo "Cleaning dump-*.rdb"
    rm -rf dump-*.rdb
    echo "Cleaning nodes-*.conf"
    rm -rf nodes-*.conf
    exit 0
fi

if [ "$1" == "clean-logs" ]
then
    echo "Cleaning *.log"
    rm -rf *.log
    exit 0
fi

echo "Usage: $0 [start|create|stop|restart|watch|tail|tailall|clean|clean-logs|call]"
echo "start       -- Launch Valkey Cluster instances."
echo "create [-f] -- Create a cluster using valkey-cli --cluster create."
echo "stop        -- Stop Valkey Cluster instances."
echo "restart     -- Restart Valkey Cluster instances."
echo "watch       -- Show CLUSTER NODES output (first 30 lines) of first node."
echo "tail <id>   -- Run tail -f of instance at base port + ID."
echo "tailall     -- Run tail -f for all the log files at once."
echo "clean       -- Remove all instances data, logs, configs."
echo "clean-logs  -- Remove just instances logs."
echo "call <cmd>  -- Call a command (up to 7 arguments) on all nodes."


# Commands to run
To create a cluster, follow these steps:

1. Edit create-cluster and change the start / end port, depending on the
number of instances you want to create.
2. Use "./create-cluster start" in order to run the instances.
3. Use "./create-cluster create" in order to execute valkey-cli --cluster create, so that
an actual Valkey cluster will be created. (If you're accessing your setup via a local container, ensure that the CLUSTER_HOST value is changed to your local IP)
4. Now you are ready to play with the cluster. AOF files and logs for each instances are created in the current directory.

In order to stop a cluster:

1. Use "./create-cluster stop" to stop all the instances. After you stopped the instances you can use "./create-cluster start" to restart them if you change your mind.
2. Use "./create-cluster clean" to remove all the AOF / log files to restart with a clean environment.

Use the command "./create-cluster help" to get the full list of features.
