/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.*;

/**
 *
 */
public abstract class DevelopmentSchemaDescriptor extends AbstractSchemaDescriptor {
/**
 * Returns a location of the plug-in given the plug-in ID. The plug-in
 * can be either workspace or external - this is handled by the 
 * model manager.
 */
	public IPath getPluginLocation(String pluginId) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		ModelEntry entry = manager.findEntry(pluginId);
		if (entry==null) return null;
		IPluginModelBase model = entry.getActiveModel();
		if (model==null) return null;
		String location = model.getInstallLocation();
		return new Path(location);
	}
}