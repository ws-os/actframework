package act;

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

import act.app.App;
import act.cli.CliContext;
import act.cli.Command;

/**
 * Admin interface to {@link App}
 */
public class ActAdmin {

    @Command(value = "act.restart", help = "restart application node")
    public void restart(CliContext context) {
        context.println("About to restart app. Your telnet session will be terminated. Please reconnect in a few seconds ...");
        context.flush();
        context.app().restart();
    }

    @Command(value = "act.shutdown", help = "shutdown application node")
    public void shutdown(CliContext context) {
        context.println("About to shutdown app. Your telnet session will be terminated. Please reconnect in a few seconds ...");
        context.flush();
        Act.shutdownApp(context.app());
    }

}
