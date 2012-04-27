/**
 * Copyright (c) 2012 Cloudsmith Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Thomas Hallgren (Cloudsmith Inc.) - initial API and implementation
 */

package org.cloudsmith.hammer.jenkins;

import net.sf.ezmorph.MorpherRegistry;
import net.sf.json.JSONObject;
import net.sf.json.util.EnumMorpher;
import net.sf.json.util.JSONUtils;

import org.cloudsmith.hammer.api.json.JSONAdapter;
import org.cloudsmith.hammer.api.json.JSONException;
import org.cloudsmith.hammer.api.model.Provider;
import org.cloudsmith.hammer.api.model.ValidationType;

import com.google.inject.binder.AnnotatedBindingBuilder;

public class StackHammerModule extends org.cloudsmith.hammer.api.StackHammerModule {

	public static class SFJSONAdapter implements JSONAdapter {

		@Override
		public <V> V fromJson(String jsonString, Class<V> type) throws JSONException {
			JSONObject jsonObject = JSONObject.fromObject(jsonString);
			return type.cast(JSONObject.toBean(jsonObject, type));
		}

		@Override
		public String toJson(Object bean) throws JSONException {
			JSONObject jsonObject = JSONObject.fromObject(bean);
			return jsonObject.toString();
		}
	}

	public StackHammerModule(String scheme, String hostname, int port, String prefix, String credentials) {
		super(scheme, hostname, port, prefix, credentials);
	}

	@Override
	protected void bindJSON(AnnotatedBindingBuilder<JSONAdapter> bind) {
		MorpherRegistry registry = JSONUtils.getMorpherRegistry();
		registry.registerMorpher(new EnumMorpher(Provider.class));
		registry.registerMorpher(new EnumMorpher(ValidationType.class));
		bind.to(SFJSONAdapter.class);
	}
}
