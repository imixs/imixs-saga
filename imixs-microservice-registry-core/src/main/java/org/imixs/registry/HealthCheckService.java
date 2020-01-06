/*  
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

package org.imixs.registry;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.imixs.workflow.bpmn.BPMNModel;

/**
 * The Imixs HealthCheckService implements the Microservice HealthCheck
 * interface.
 * <p>
 * The service returns the count of registered workflow models
 * <p>
 * Example:
 * <code>{"data":{"model.count":1},"name":"imixs-workflow","state":"UP"}</code>
 * <p>
 * This check indicates the overall status of the registry service. If models
 * are available the service works.
 * 
 * @author rsoika
 * @version 1.0
 */
@Health
@ApplicationScoped
public class HealthCheckService implements HealthCheck {

    @Inject
    private RegistryService registryService;

    /**
     * This is the implementation for the health check call back method.
     * <p>
     * The method returns the status 'UP' in case the count of workflow models >= 0
     * <p>
     * Example:
     * <code>{"data":{"model.count":1},"name":"imixs-workflow","state":"UP"}</code>
     * <p>
     * This check indicates the overall status of the registry service. If
     * registryService is available, status is UP.
     * 
     */
    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = null;
        int modelCount = 0;
        String errorMessage = null;
        try {
            List<BPMNModel> models = registryService.getModels();
            if (models != null) {
                modelCount = models.size();
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            // failed! - result in status=down
            modelCount = -1;
        }

        if (modelCount >= 0) {
            builder = HealthCheckResponse.named("imixs-registry").withData("model.count", modelCount).up();
        } else {
            builder = HealthCheckResponse.named("imixs-registry").withData("error", errorMessage).down();
        }

        return builder.build();
    }

}
