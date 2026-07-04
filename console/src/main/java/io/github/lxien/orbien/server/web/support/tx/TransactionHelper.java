/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.server.web.support.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class TransactionHelper {
    private final Logger logger = LoggerFactory.getLogger(TransactionHelper.class);

    public void afterCommit(Runnable task) {
        // 没有事务，直接执行
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("afterCommit任务错误（无tx）", e);
            }
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            logger.debug("数据库事务执行成功，开始执行异步任务");
                            task.run();
                        } catch (Exception e) {
                            logger.error("afterCommit 任务错误", e);
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            logger.debug("事务回滚，提交任务后跳过");
                        }
                    }
                }
        );
    }


    /**
     * 事务回滚时执行异步任务
     */
    public void afterRollback(Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            try {
                                logger.debug("数据库事务执行失败，开始执行异步任务");
                                task.run();
                            } catch (Exception e) {
                                logger.error("afterRollback 任务错误", e);
                            }
                        }
                    }
                }
        );
    }
}