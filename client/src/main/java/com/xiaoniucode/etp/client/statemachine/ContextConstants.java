/*
 *
 *  *    Copyright 2026 xiaoniucode
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.xiaoniucode.etp.client.statemachine;

public interface ContextConstants {
    String CREATE_CONN_RESP = "create_connection_resp";
    String BATCH_CREATE_PROXIES_RESP = "batch_create_proxies_resp";
    String CREATE_CONN_COMMAND = "create_conn_command";
    String AUTH_RESP = "auth_resp";
    String ERROR = "error";
    String TUNNEL_ID = "tunnel_Id";
    String COMPRESS = "compress";
    String ENCRYPT = "encrypt";
    String MULTIPLEX = "multiplex";
}
