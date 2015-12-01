package babybox.events.listener;

import common.schedule.JobScheduler;
import common.thread.TransactionalRunnableTask;

public abstract class EventListener {
    
    protected void executeAsync(TransactionalRunnableTask task) {
        JobScheduler.getInstance().run(task);
    }
}
