#
# /*
#  *    Copyright 2026 lxien
#  *
#  *    Licensed under the Apache License, Version 2.0 (the "License");
#  *    you may not use this file except in compliance with the License.
#  *    You may obtain a copy of the License at
#  *
#  *        http://www.apache.org/licenses/LICENSE-2.0
#  *
#  *    Unless required by applicable law or agreed to in writing, software
#  *    distributed under the License is distributed on an "AS IS" BASIS,
#  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  *    See the License for the specific language governing permissions and
#  *    limitations under the License.
#  */
#

#!/bin/bash

# 删除 orbiens 容器
docker rm -f orbiens

# 删除 MySQL 容器
docker rm -f orbiens-mysql

# 删除 Docker 网络
docker network rm orbiens-net

# 删除 orbiens 挂载目录
rm -rf /opt/orbiens

# 删除悬空容器
docker container prune -f

