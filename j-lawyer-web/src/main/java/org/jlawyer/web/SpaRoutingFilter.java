/*
 * Copyright (C) 2024 j-lawyer.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jlawyer.web;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

/**
 * Serves {@code index.html} with HTTP 200 for Angular client-side routes, so deep links and
 * page reloads (e.g. {@code /cases/123}, {@code /calendar}) load the SPA instead of a 404.
 *
 * <p>Requests for real files (any path whose last segment contains a dot, i.e. an extension
 * such as {@code main-*.js}, {@code i18n/de.json}, {@code favicon.ico}) and the context root
 * are passed through unchanged; the Angular router then resolves the route in the browser.
 *
 * <p>A {@link javax.servlet.RequestDispatcher#forward forward} is used deliberately rather than
 * the {@code web.xml} 404 {@code <error-page>}: a forward keeps the response status at 200,
 * whereas the error-page served index.html but left the status at 404. The filter is mapped to
 * REQUEST dispatches only, so the forward to {@code /index.html} does not re-enter it.
 */
@WebFilter(urlPatterns = "/*")
public class SpaRoutingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // no configuration needed
    }

    @Override
    public void destroy() {
        // no resources to release
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if ("GET".equalsIgnoreCase(req.getMethod()) && isClientRoute(req)) {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * @return true for extension-less, non-root paths (treated as Angular routes); false for
     * the context root and for static assets (paths whose last segment has a file extension).
     */
    private boolean isClientRoute(HttpServletRequest req) {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.isEmpty() || "/".equals(path)) {
            return false; // let the welcome-file serve index.html (already 200)
        }
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        return lastSegment.indexOf('.') < 0;
    }
}
