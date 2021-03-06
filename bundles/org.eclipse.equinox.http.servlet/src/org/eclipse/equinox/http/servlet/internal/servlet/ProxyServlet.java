/*******************************************************************************
 * Copyright (c) 2005, 2016 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Raymond Augé - bug fixes and enhancements
 *     Arnaud Mergey <a_mergey@yahoo.fr> - Bug 497510
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.servlet;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.Activator;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.DispatchTargets;
import org.eclipse.equinox.http.servlet.internal.util.Const;

/**
 * The ProxyServlet is the private side of a Servlet that when registered (and init() called) in a servlet container
 * will in-turn register and provide an OSGi Http Service implementation.
 * This class is not meant for extending or even using directly and is purely meant for registering
 * in a servlet container.
 */
public class ProxyServlet extends HttpServlet {

	private static final long serialVersionUID = 4117456123807468871L;
	private HttpServiceRuntimeImpl httpServiceRuntimeImpl;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		Activator.addProxyServlet(this);
	}

	public void destroy() {
		Activator.unregisterHttpService(this);

		super.destroy();
	}

	public void setHttpServiceRuntimeImpl(
		HttpServiceRuntimeImpl httpServiceRuntimeImpl) {

		this.httpServiceRuntimeImpl = httpServiceRuntimeImpl;
	}

	public void sessionIdChanged(String oldSessionId) {
		httpServiceRuntimeImpl.fireSessionIdChanged(oldSessionId);
	}
	
	/**
	 * get the value of path info, not decoded by the server 
	 */
	private String getNotDecodedAlias(HttpServletRequest request) {
		String pathInfo = HttpServletRequestWrapperImpl.getDispatchPathInfo(request);
		if(pathInfo == null) {
			return null;
		}
		String requestUri = HttpServletRequestWrapperImpl.getDispatchRequestURI(request);
		
		String[] pathInfoSegments = pathInfo.split(Const.SLASH);
		String[] requestUriSegments = requestUri.split(Const.SLASH);
		
		if(pathInfoSegments.length == requestUriSegments.length) {
			return requestUri;
		}
		
		StringBuilder aliasBuilder = new StringBuilder();
		for(int i=(requestUriSegments.length - pathInfoSegments.length + 1);i<requestUriSegments.length;i++) {
			aliasBuilder.append(Const.SLASH).append(requestUriSegments[i]);
		}
		return aliasBuilder.toString();
	}

	/**
	 * @see HttpServlet#service(ServletRequest, ServletResponse)
	 */
	protected void service(
			HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		checkRuntime();

		String alias = getNotDecodedAlias(request);

		if (alias == null) {
			alias = Const.SLASH;
		}

		DispatchTargets dispatchTargets = httpServiceRuntimeImpl.getDispatchTargets(alias, null);

		if (dispatchTargets != null) {
			dispatchTargets.doDispatch(
				request, response, alias, request.getDispatcherType());

			return;
		}

		response.sendError(
			HttpServletResponse.SC_NOT_FOUND, "ProxyServlet: " + alias); //$NON-NLS-1$
	}

	private void checkRuntime() {
		if (httpServiceRuntimeImpl == null) {
			throw new IllegalStateException(
				"Proxy servlet not properly initialized. httpServiceRuntimeImpl is null"); //$NON-NLS-1$
		}
	}

}
