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

package io.github.lxien.orbien.server.web.configuration;

//@Configuration
//public class UidGeneratorConfig {
//    /**
//     * 生成 19位ID
//     *
//     * @return ID
//     */
//    @Bean
//    @ConditionalOnMissingBean(UidGenerator.class)
//    public UidGenerator uidGenerator(WorkerNodeRepository workerNodeRepository) {
//        DefaultUidGenerator generator = new DefaultUidGenerator();
//        generator.setTimeBits(29);
//        generator.setWorkerBits(21);
//        generator.setSeqBits(13);
//        generator.setEpochStr("2026-04-14");
//        generator.setWorkerIdAssigner(new DisposableWorkerIdAssigner(workerNodeRepository));
//        return generator;
//    }
//}
