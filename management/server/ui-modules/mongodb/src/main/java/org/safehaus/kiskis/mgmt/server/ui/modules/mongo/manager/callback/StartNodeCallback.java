/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.server.ui.modules.mongo.manager.callback;

import com.vaadin.ui.Button;
import com.vaadin.ui.Embedded;
import org.safehaus.kiskis.mgmt.shared.protocol.Response;
import org.safehaus.kiskis.mgmt.shared.protocol.Task;
import org.safehaus.kiskis.mgmt.shared.protocol.Util;
import org.safehaus.kiskis.mgmt.shared.protocol.api.AsyncTaskRunner;
import org.safehaus.kiskis.mgmt.shared.protocol.api.TaskCallback;

/**
 *
 * @author dilshat
 */
public class StartNodeCallback implements TaskCallback {

    private final Button checkButton;
    private final StringBuilder stdOutput = new StringBuilder();
    private final AsyncTaskRunner taskRunner;

    public StartNodeCallback(AsyncTaskRunner taskRunner, Embedded progressIcon, Button checkButton, Button startButton, Button stopButton, Button destroyButton) {
        this.taskRunner = taskRunner;
        progressIcon.setVisible(true);
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        destroyButton.setEnabled(false);
        this.checkButton = checkButton;
    }

    @Override
    public void onResponse(Task task, Response response) {
        if (!Util.isStringEmpty(response.getStdOut())) {
            stdOutput.append(response.getStdOut());
        }
        if (stdOutput.indexOf("child process started successfully, parent exiting") > -1) {
            taskRunner.removeTaskCallback(task.getUuid());
            checkButton.click();
        } else if (Util.isFinalResponse(response)) {
            checkButton.click();
        }
    }

}
