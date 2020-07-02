/*
 * Imixs-Workflow
 * 
 * Copyright (C) 2001-2020 Imixs Software Solutions GmbH, http://www.imixs.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This /*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.registry.index.solr;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.services.rest.BasicAuthenticator;
import org.imixs.workflow.services.rest.RestClient;

/**
 * The SolrSchemaService updates the solr schema on startup.
 * 
 * @author rsoika
 * @version 1.0
 */
@Startup
@Singleton
public class SolrSchemaService {
    @Inject
    @ConfigProperty(name = "solr.api", defaultValue = "")
    private String api;

    @Inject
    @ConfigProperty(name = "solr.user", defaultValue = "")
    private String user;

    @Inject
    @ConfigProperty(name = "solr.password", defaultValue = "")
    private String password;

    @Inject
    @ConfigProperty(name = "solr.core", defaultValue = "imixs-registry")
    private String core;

    private RestClient restClient;

    private static Logger logger = Logger.getLogger(SolrSchemaService.class.getName());

    @Inject
    SolrUpdateService solrUpdateService;

    /**
     * Create a solr rest client instance
     * 
     * @throws org.imixs.workflow.services.rest.RestAPIException
     */
    @PostConstruct
    public void init() {

        if (api.isEmpty()) {
            // no solr index!
            return;
        }

        logger.info("...verify solr schema");
        // create rest client
        restClient = new RestClient(api);
        if (user != null && !user.isEmpty()) {
            BasicAuthenticator authenticator = new BasicAuthenticator(user, password);
            restClient.registerRequestFilter(authenticator);
        }

        // try to get the schema of the core...
        try {
            String existingSchema = restClient.get(api + "/api/cores/" + core + "/schema");
            logger.info("...core   - OK ");

            // update schema
            solrUpdateService.updateSchema(existingSchema);
        } catch (org.imixs.workflow.services.rest.RestAPIException e) {
            // no schema found
            logger.severe("...no solr core '" + core + "' found - " + e.getMessage() + ": verify the solr instance!");

        }

    }

}
