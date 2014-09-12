package org.safehaus.subutai.core.container.ui.executor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by timur on 9/8/14.
 */
public class AgentExecutorImpl implements AgentExecutor {
    private String hostName;
    private List<String> containerNames;
    private Set<AgentExecutionListener> listeners = new HashSet<>();

    public AgentExecutorImpl(String hostName, List<String> containerNames) {
        this.hostName = hostName;
        this.containerNames = containerNames;
    }

    @Override
    public void addListener(AgentExecutionListener listener) {
        if (listener != null)
            listeners.add(listener);
    }

//    public String getHostName() {
//        return hostName;
//    }
//
//    public void setHostName(String hostName) {
//        this.hostName = hostName;
//    }
//
//    public List<String> getContainerNames() {
//        return containerNames;
//    }
//
//    public void setContainerNames(List<String> containerNames) {
//        this.containerNames = containerNames;
//    }

    @Override
    public void execute(final ExecutorService executor, final AgentCommandFactory commandFactory) {
        final CompletionService<AgentExecutionEvent> completionService = new ExecutorCompletionService(executor);
        for (final String containerName : containerNames) {
            completionService.submit(new Callable() {
                public AgentExecutionEvent call() {
                    fireEvent(new AgentExecutionEvent(hostName, containerName, AgentExecutionEventType.START, ""));
                    try {
                        AgentCommand command = commandFactory.newCommand(containerName);
                        command.execute();
                        return (new AgentExecutionEvent(hostName, containerName, AgentExecutionEventType.SUCCESS, ""));
                    } catch (AgentExecutionException ce) {
                        return (new AgentExecutionEvent(hostName, containerName, AgentExecutionEventType.FAIL, ce.toString()));
                    }
                }
            });
        }

        ExecutorService waiter = Executors.newFixedThreadPool(1);
        waiter.execute(new Runnable() {
            @Override
            public void run() {
                for (String cn : containerNames) {
                    try {
                        Future<AgentExecutionEvent> future = completionService.take();
                        AgentExecutionEvent result = future.get();
                        fireEvent(result);
                    } catch (InterruptedException | ExecutionException e) {
                        fireEvent(new AgentExecutionEvent(hostName, "", AgentExecutionEventType.FAIL, e.toString()));
                    }
                }
            }
        });
        waiter.shutdown();
    }

    private void fireEvent(AgentExecutionEvent event) {
        for (AgentExecutionListener listener : listeners) {
            if (listener != null)
                listener.onExecutionEvent(event);
        }
    }
}