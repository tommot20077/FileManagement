#!/bin/bash

# 等待服務可用的腳本
wait_for_service() {
  local host=$1
  local port=$2
  local timeout=$3
  local start_time=$(date +%s)
  local end_time=$((start_time + timeout))
  
  echo "等待服務 $host:$port 準備就緒，超時設定為 $timeout 秒..."
  
  while [ $(date +%s) -lt $end_time ]; do
    if nc -z $host $port > /dev/null 2>&1; then
      echo "服務 $host:$port 已就緒！"
      return 0
    fi
    
    echo "服務 $host:$port 尚未就緒，等待 5 秒..."
    sleep 5
  done
  
  echo "錯誤：等待服務 $host:$port 超時"
  return 1
}

# 這裡的 資料庫 服務名稱需要根據實際情況修改，需符合 docker-compose.yml 中的服務名稱以及端口
# 等待 MySQL
wait_for_service "filemanager_mysql" "3306" "60" || exit 1

# 等待 MongoDB
wait_for_service "filemanager_mongo" "27017" "60" || exit 1

# 等待 Redis
wait_for_service "filemanager_redis" "6381" "60" || exit 1

# 啟動應用
echo "所有依賴服務已準備就緒，啟動應用..."
exec java -jar app.jar