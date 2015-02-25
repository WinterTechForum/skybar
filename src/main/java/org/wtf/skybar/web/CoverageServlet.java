package org.wtf.skybar.web;

import org.eclipse.jetty.util.ajax.JSON;
import org.wtf.skybar.registry.SkybarRegistry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 */
public class CoverageServlet extends HttpServlet {
    private final SkybarRegistry registry;

    public CoverageServlet(SkybarRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.getWriter().write(JSON.toString(registry.getSnapshot()));
    }
}
