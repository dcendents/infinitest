/*
 * Infinitest, a Continuous Test Runner.
 *
 * Copyright (C) 2010-2013
 * "Ben Rady" <benrady@gmail.com>,
 * "Rod Coffin" <rfciii@gmail.com>,
 * "Ryan Breidenbach" <ryan.breidenbach@gmail.com>
 * "David Gageot" <david@gageot.net>, et al.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.infinitest.eclipse.event;

import static org.eclipse.core.resources.IResourceChangeEvent.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.infinitest.eclipse.workspace.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

@Component
class FeatureFileChangeProcessor extends EclipseEventProcessor {
	private final WorkspaceFacade workspace;

	@Autowired
	FeatureFileChangeProcessor(WorkspaceFacade workspace) {
		super("Looking for tests");
		this.workspace = workspace;
	}

	@Override
	public boolean canProcessEvent(IResourceChangeEvent event) {
		return (event.getType() & (POST_BUILD | POST_CHANGE)) > 0;
	}

	@Override
	public void processEvent(IResourceChangeEvent event) throws CoreException {
		if (containsFeatureFileChanges(getDeltas(event))) {
			workspace.updateProjects();
		}
	}

	private boolean containsFeatureFileChanges(IResourceDelta... deltas) {
		// DEBT SHould use IResourceDeltaVisitor instead
		for (IResourceDelta delta : deltas) {
			if (isFeatureFile(delta) || containsFeatureFileChanges(delta.getAffectedChildren())) {
				return true;
			}
		}
		return false;
	}

	private boolean isFeatureFile(IResourceDelta delta) {
		return delta.getFullPath().toPortableString().endsWith(".feature");
	}
}
