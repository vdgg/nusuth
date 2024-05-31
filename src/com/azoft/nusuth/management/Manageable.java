/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
package com.azoft.nusuth.management;

import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;

public interface Manageable {


    public boolean isRestartNeeded(CompositeNusuthWebAppElement settings) throws DeploymentException;


    public void applySettings(CompositeNusuthWebAppElement settings) throws DeploymentException;
}