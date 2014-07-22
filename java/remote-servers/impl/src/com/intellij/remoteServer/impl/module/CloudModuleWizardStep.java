/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.remoteServer.impl.module;/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.util.containers.hash.HashMap;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CloudModuleWizardStep extends ModuleWizardStep {

  private JPanel myMainPanel;
  private JPanel myAccountPanelPlaceHolder;
  private JPanel myApplicationPanelPlaceHolder;

  private final CloudModuleBuilder myModuleBuilder;
  private final Project myProject;
  private final Disposable myParentDisposable;

  private CloudAccountSelectionPanel myAccountSelectionPanel;

  private Map<ServerType<?>, CloudApplicationConfigurable> myCloudType2ApplicationConfigurable;

  public CloudModuleWizardStep(CloudModuleBuilder moduleBuilder, Project project, Disposable parentDisposable) {
    myModuleBuilder = moduleBuilder;
    myProject = project;
    myParentDisposable = parentDisposable;

    myCloudType2ApplicationConfigurable = new HashMap<ServerType<?>, CloudApplicationConfigurable>();

    List<ServerType<?>> cloudTypes = new ArrayList<ServerType<?>>();
    for (CloudModuleBuilderContribution contribution : CloudModuleBuilderContribution.EP_NAME.getExtensions()) {
      cloudTypes.add(contribution.getCloudType());
    }

    myAccountSelectionPanel = new CloudAccountSelectionPanel(cloudTypes);
    myAccountPanelPlaceHolder.add(myAccountSelectionPanel.getMainPanel());

    myAccountSelectionPanel.setAccountSelectionListener(new Runnable() {

      @Override
      public void run() {
        onAccountSelectionChanged();
      }
    });
    onAccountSelectionChanged();
  }

  private RemoteServer<?> getSelectedAccount() {
    return myAccountSelectionPanel.getSelectedAccount();
  }

  private void onAccountSelectionChanged() {
    CardLayout applicationPlaceHolderLayout = (CardLayout)myApplicationPanelPlaceHolder.getLayout();

    RemoteServer<?> account = getSelectedAccount();
    boolean haveAccount = account != null;
    myApplicationPanelPlaceHolder.setVisible(haveAccount);
    if (!haveAccount) {
      return;
    }

    ServerType<?> cloudType = account.getType();
    String cardName = cloudType.getId();
    CloudApplicationConfigurable<?, ?, ?, ?> applicationConfigurable = getApplicationConfigurable();
    if (applicationConfigurable == null) {
      applicationConfigurable
        = CloudModuleBuilderContribution.getInstanceByType(cloudType).createApplicationConfigurable(myProject, myParentDisposable);
      myCloudType2ApplicationConfigurable.put(cloudType, applicationConfigurable);
      myApplicationPanelPlaceHolder.add(applicationConfigurable.getComponent(), cardName);
    }
    applicationPlaceHolderLayout.show(myApplicationPanelPlaceHolder, cardName);

    applicationConfigurable.setAccount(account);
  }

  @Override
  public JComponent getComponent() {
    return myMainPanel;
  }

  private CloudApplicationConfigurable<?, ?, ?, ?> getApplicationConfigurable() {
    RemoteServer<?> account = getSelectedAccount();
    if (account == null) {
      return null;
    }
    return myCloudType2ApplicationConfigurable.get(account.getType());
  }

  @Override
  public void updateDataModel() {
    myModuleBuilder.setAccount(myAccountSelectionPanel.getSelectedAccount());
    CloudApplicationConfigurable<?, ?, ?, ?> configurable = getApplicationConfigurable();
    myModuleBuilder.setApplicationConfiguration(configurable == null ? null : configurable.createConfiguration());
  }

  @Override
  public boolean validate() throws ConfigurationException {
    myAccountSelectionPanel.validate();
    CloudApplicationConfigurable<?, ?, ?, ?> configurable = getApplicationConfigurable();
    if (configurable != null) {
      configurable.validate();
    }
    return super.validate();
  }
}
