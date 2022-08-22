/*-
 * #%L
 * License Compliance Tool
 * %%
 * Copyright (C) 2022 medavis GmbH
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
package de.medavis.lct.util;

import hudson.model.AbstractBuild;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.StreamSupport;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.actions.WorkspaceActionImpl;

public class WorkspaceResolver {

    public static Path getPathRelativeToWorkspace(String path, AbstractBuild build) throws IOException, InterruptedException {
        return Paths.get(build.getWorkspace().child(path).toURI());
    }

    public static Path getPathRelativeToWorkspace(String path, WorkflowRun build) {
        return StreamSupport.stream(new FlowGraphWalker(build.getExecution()).spliterator(), false)
                .filter(StepStartNode.class::isInstance)
                .flatMap(flowNode -> flowNode.getActions().stream())
                .filter(WorkspaceActionImpl.class::isInstance)
                .map(WorkspaceActionImpl.class::cast)
                .map(WorkspaceActionImpl::getPath)
                .map(Paths::get)
                .map(workspace -> workspace.resolve(path))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Could not determine workspace location."));
    }

}
