package org.wtf.skybar.web;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.wtf.skybar.source.SourceProvider;

final class SourceServlet extends HttpServlet {

    private final List<SourceProvider> sourceProviders;

    SourceServlet(List<SourceProvider> sourceProviders) {
        this.sourceProviders = sourceProviders;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getRequestURI().substring(req.getContextPath().length() + 1);

        resp.setContentType("text/plain");

        for (SourceProvider sourceProvider : sourceProviders) {

            String source = sourceProvider.getSource(pathInfo);
            if (source == null) {
                continue;
            }

            resp.setStatus(200);
            resp.getWriter().write(source);
            return;
        }

        resp.setStatus(404);
        resp.getWriter().write("Could not find " + pathInfo);
    }
}
