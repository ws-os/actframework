package act.xio;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Destroyable;
import act.controller.meta.ActionMethodMetaInfo;

/**
 * Encapsulate operations provided by underline network service, e.g. netty/undertow etc
 */
public interface Network extends Destroyable {
    void register(int port, boolean secure, NetworkHandler client);

    void start();

    void shutdown();

    /**
     * create a {@link WebSocketConnectionHandler} instance
     * @param methodInfo the action handler method meta info
     * @return a websocket connection handler
     */
    WebSocketConnectionHandler createWebSocketConnectionHandler(ActionMethodMetaInfo methodInfo);

    /**
     * Create a {@link WebSocketConnectionHandler} instance without message handler, i.e.
     * the handler is pure created to handle connection request. This is typically used when
     * client request connection for push notification from server
     *
     * @return a connection handler without message processing logic
     */
    WebSocketConnectionHandler createWebSocketConnectionHandler();
}
