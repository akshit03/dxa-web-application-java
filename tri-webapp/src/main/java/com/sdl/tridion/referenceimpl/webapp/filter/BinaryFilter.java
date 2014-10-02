package com.sdl.tridion.referenceimpl.webapp.filter;

import com.tridion.broker.StorageException;
import com.tridion.storage.*;
import com.tridion.storage.dao.BinaryContentDAO;
import com.tridion.storage.dao.BinaryVariantDAO;
import com.tridion.storage.dao.ItemDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * TODO: Documentation.
 */
public class BinaryFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(BinaryFilter.class);

    // TODO: Publication id should be determined from configuration instead of being hard-coded
    private static final int PUBLICATION_ID = 48;

    private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

    private ServletContext servletContext;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;
        final String url = URL_PATH_HELPER.getRequestUri(req).replace(URL_PATH_HELPER.getContextPath(req), "");

        if (!handleRequest(url, response)) {
            chain.doFilter(request, response);
        }
    }

    private boolean handleRequest(String url, ServletResponse response) throws IOException, ServletException {
        LOG.debug("handleRequest: {}", url);

        if (url.endsWith(".html") || url.endsWith(".jsp")) {
            // Skip if this is a request for a HTML page or a JSP
            return false;
        }

        try {
            final BinaryVariant binaryVariant = findBinaryVariant(PUBLICATION_ID, url);
            if (binaryVariant == null) {
                LOG.debug("No binary variant found for: {}", url);
                return false;
            }

            final BinaryMeta binaryMeta = binaryVariant.getBinaryMeta();
            final ItemMeta itemMeta = findItemMeta(binaryMeta.getPublicationId(), binaryMeta.getItemId());

            final File file = new File(servletContext.getRealPath(url));

            boolean refresh;
            if (file.exists()) {
                refresh = file.lastModified() < itemMeta.getLastPublishDate().getTime();
            } else {
                refresh = true;
                if (!file.getParentFile().exists()) {
                    if (!file.getParentFile().mkdirs()) {
                        throw new IOException("Failed to create parent directory for file: " + file);
                    }
                }
            }

            if (refresh) {
                final BinaryContent binaryContent = findBinaryContent(itemMeta.getPublicationId(), itemMeta.getItemId(),
                        binaryVariant.getVariantId());

                LOG.debug("Writing binary content to file: {}", file);
                Files.write(file.toPath(), binaryContent.getContent());
            } else {
                LOG.debug("File does not need to be refreshed: {}", file);
            }

            LOG.debug("Sending response with content of file: {}", file);
            Files.copy(file.toPath(), response.getOutputStream());
            return true;
        } catch (StorageException e) {
            throw new ServletException("Error while handling request: " + url, e);
        }
    }

    private BinaryVariant findBinaryVariant(int publicationId, String url) throws StorageException {
        final BinaryVariantDAO dao = (BinaryVariantDAO) StorageManagerFactory.getDAO(publicationId,
                StorageTypeMapping.BINARY_VARIANT);
        final List<BinaryVariant> binaryVariants = dao.findByURL(url);
        if (binaryVariants == null || binaryVariants.isEmpty()) {
            return null;
        }

        return binaryVariants.get(0);
    }

    private ItemMeta findItemMeta(int publicationId, int itemId) throws StorageException {
        final ItemDAO dao = (ItemDAO) StorageManagerFactory.getDAO(PUBLICATION_ID, StorageTypeMapping.ITEM_META);
        return dao.findByPrimaryKey(publicationId, itemId);
    }

    private BinaryContent findBinaryContent(int publicationId, int itemId, String variantId) throws StorageException {
        final BinaryContentDAO dao = (BinaryContentDAO) StorageManagerFactory.getDAO(PUBLICATION_ID,
                StorageTypeMapping.BINARY_CONTENT);
        return dao.findByPrimaryKey(publicationId, itemId, variantId);
    }

    @Override
    public void destroy() {
    }
}