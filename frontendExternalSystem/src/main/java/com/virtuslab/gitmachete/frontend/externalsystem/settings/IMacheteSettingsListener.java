package com.virtuslab.gitmachete.frontend.externalSystem.settings;

import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.util.messages.Topic;

public interface IMacheteSettingsListener extends ExternalSystemSettingsListener<MacheteProjectSettings> {

  Topic<IMacheteSettingsListener> TOPIC = Topic.create(/* displayName */ "Machete-specific settings",
      IMacheteSettingsListener.class);

}
